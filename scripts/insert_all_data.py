"""
Script to parse all CSVs in data/ and insert them into the corresponding PostgreSQL tables.
Follows the logic in backend/algos/utils/CSVDataParser.java and matches backend model fields.
"""

import os
import sys
import logging
import re
from datetime import datetime, timedelta
from db_connection import get_db_connection
from config import TXT_FILES, DATA_DIR
import json

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

def parse_orders(filepath, year, month):
    """Parse a sales file (e.g., ventas202405.csv) and extract orders."""
    orders = []
    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            # Format: 11d13h31m:13,18,c-198,3m3,4h
            if ':' not in line:
                logger.warning(f"Invalid order line: {line}")
                continue
            time_part, data_part = line.split(':', 1)
            # Parse time (e.g., 11d13h31m)
            m = re.match(r"(\d+)d(\d+)h(\d+)m", time_part)
            if not m:
                logger.warning(f"Invalid time format: {time_part}")
                continue
            day, hour, minute = map(int, m.groups())
            creation_time = datetime(year, month, day, hour, minute)
            # Parse data
            parts = [p.strip() for p in data_part.split(',')]
            if len(parts) != 5:
                logger.warning(f"Invalid order data: {data_part}")
                continue
            x, y = int(parts[0]), int(parts[1])
            client_id = parts[2]
            amount_glp = int(parts[3].replace('m3', ''))
            deadline_h = int(parts[4].replace('h', ''))
            deadline = creation_time + timedelta(hours=deadline_h)
            # DB fields: codigo_cliente, fecha_registro, posicionx, posiciony, cantidadglp, tiempo_tolerancia
            orders.append({
                'codigo_cliente': client_id,
                'fecha_registro': creation_time,
                'posicionx': x,
                'posiciony': y,
                'cantidadglp': amount_glp,
                'tiempo_tolerancia': deadline_h
            })
    return orders

def parse_vehicles(filepath):
    """Parse vehicles.txt as in CSVDataParser.java"""
    vehicles = []
    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or 'Tipo' in line or 'Unidades' in line:
                continue
            parts = [p.strip() for p in line.split(',')]
            if len(parts) != 6:
                logger.warning(f"Invalid vehicle line: {line}")
                continue
            type_, gross_weight_ton, max_glp, _, _, units = parts
            gross_weight = int(float(gross_weight_ton) * 1000)
            max_glp = int(max_glp)
            units = int(units)
            for i in range(units):
                placa = f"{type_}{i+1:02d}"
                vehicles.append({
                    'tipo': type_,
                    'placa': placa,
                    'peso': gross_weight,
                    'max_combustible': 25.0,
                    'curr_combustible': 25.0,
                    'max_glp': float(max_glp),
                    'curr_glp': float(max_glp),
                    'posicionx': 12.0,
                    'posiciony': 8.0,
                    'disponible': True
                })
    return vehicles

def parse_warehouses(filepath):
    """Parse warehouses.txt as in CSVDataParser.java"""
    warehouses = []
    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line or 'Tipo' in line:
                continue
            parts = [p.strip() for p in line.split(',')]
            if len(parts) not in (3, 4):
                logger.warning(f"Invalid warehouse line: {line}")
                continue
            x, y, max_glp = int(parts[0]), int(parts[1]), float(parts[2])
            is_main = len(parts) == 4 and parts[3].lower() == 'main'
            if is_main:
                capacidad_efectivam3 = float('inf')
                horario_abastecimiento = 'Siempre'
            else:
                capacidad_efectivam3 = 160.0
                horario_abastecimiento = '00:00'
            warehouses.append({
                'posicionx': x,
                'posiciony': y,
                'capacidad_efectivam3': max_glp if not is_main else 1e9,  # Use a large value for main
                'es_principal': is_main,
                'horario_abastecimiento': horario_abastecimiento
            })
    return warehouses

def parse_failures(filepath, db):
    """Parse failures.txt and prepare for incidencia insertion."""
    incidencias = []
    placa_to_id = {}
    # Build a map from placa to vehiculo.id
    vehiculos = db.execute_query("SELECT id, placa FROM vehiculo")
    if vehiculos:
        placa_to_id = {v['placa']: v['id'] for v in vehiculos}
    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            # Format: T1_TA01_TI3
            parts = line.split('_')
            if len(parts) != 3:
                logger.warning(f"Invalid failure line: {line}")
                continue
            shift_str, placa, type_str = parts
            # Map shift
            shift_map = {'T1': 'T1', 'T2': 'T2', 'T3': 'T3'}
            turno = shift_map.get(shift_str, None)
            if not turno:
                logger.warning(f"Invalid shift: {shift_str}")
                continue
            # Map vehicle
            vehiculo_id = placa_to_id.get(placa)
            if not vehiculo_id:
                logger.warning(f"Vehicle placa not found: {placa}")
                continue
            # Use today as fecha (or could parse from elsewhere)
            fecha = datetime.now().date()
            # Always set ocurrido to False (or True if you want)
            incidencias.append({
                'fecha': fecha,
                'turno': turno,
                'vehiculo_id': vehiculo_id,
                'ocurrido': False
            })
    return incidencias

def parse_blockages(filepath, year, month):
    """Parse a blockages file (e.g., 202405.bloqueos.txt) and prepare for bloqueo insertion for the given year and the next.
    The file is duplicated for the next year."""
    blockages = []
    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            # Format: 01d06h00m-01d15h00m:50,45,50,40,44,40
            if ':' not in line:
                logger.warning(f"Invalid blockage line: {line}")
                continue
            time_part, coords_part = line.split(':', 1)
            if '-' not in time_part:
                logger.warning(f"Invalid blockage time range: {time_part}")
                continue
            start_str, end_str = time_part.split('-')
            m1 = re.match(r"(\d+)d(\d+)h(\d+)m", start_str)
            m2 = re.match(r"(\d+)d(\d+)h(\d+)m", end_str)
            if not m1 or not m2:
                logger.warning(f"Invalid blockage time: {time_part}")
                continue

            start_day, start_hour, start_minute = map(int, m1.groups())
            end_day, end_hour, end_minute = map(int, m2.groups())
            
            start_time_current_year = datetime(year, month, start_day, start_hour, start_minute)
            end_time_current_year = datetime(year, month, end_day, end_hour, end_minute)
            
            coords = [int(c.strip()) for c in coords_part.split(',') if c.strip()]
            vertices = [(coords[i], coords[i+1]) for i in range(0, len(coords), 2)]
            vertices_json = json.dumps([{"x": x, "y": y} for x, y in vertices])

            # Add blockage for the current year
            blockages.append({
                'start_time': start_time_current_year,
                'end_time': end_time_current_year,
                'vertices_json': vertices_json
            })

            # Add blockage for the next year
            start_time_next_year = start_time_current_year.replace(year=year + 1)
            end_time_next_year = end_time_current_year.replace(year=year + 1)
            blockages.append({
                'start_time': start_time_next_year,
                'end_time': end_time_next_year,
                'vertices_json': vertices_json
            })
    return blockages

def parse_maintenances(filepath, db):
    """Parse a maintenances file and prepare for insertion, duplicating every 2 months for a year.
    The file is duplicated for the next year."""
    maintenances = []
    placa_to_id = {}
    vehiculos = db.execute_query("SELECT id, placa FROM vehiculo")
    if vehiculos:
        placa_to_id = {v['placa']: v['id'] for v in vehiculos}
    
    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            # Format: YYYYMMDD:PLACA
            if ':' not in line:
                logger.warning(f"Invalid maintenance line: {line}")
                continue
            
            date_str, placa = line.split(':', 1)
            try:
                original_year = int(date_str[:4])
                original_month = int(date_str[4:6])
                original_day = int(date_str[6:])

                vehiculo_id = placa_to_id.get(placa)
                if not vehiculo_id:
                    logger.warning(f"Vehicle placa not found in DB: {placa}")
                    continue

                # Generate maintenance records for original month, then +2, +4, ..., +22 months
                for i in range(0, 23, 2):
                    # Calculate the new month and year
                    new_month_total = original_month + i
                    new_year = original_year + (new_month_total - 1) // 12
                    final_month = (new_month_total - 1) % 12 + 1
                    
                    start_time = datetime(new_year, final_month, original_day)
                    end_time = start_time + timedelta(hours=23, minutes=59)
                    
                    maintenances.append({
                        'vehiculo_id': vehiculo_id,
                        'start_time': start_time,
                        'end_time': end_time
                    })
            except ValueError:
                logger.warning(f"Invalid date format in maintenance line: {line}")
            except Exception as e:
                logger.error(f"Error processing maintenance line '{line}': {e}")
                
    return maintenances

def insert_entities(db, table, entities, field_order):
    if not entities:
        logger.info(f"No data to insert for {table}")
        return
    placeholders = ','.join(['%s'] * len(field_order))
    columns = ','.join(field_order)
    query = f"INSERT INTO {table} ({columns}) VALUES ({placeholders})"
    params_list = [[e[f] for f in field_order] for e in entities]
    logger.info(f"Inserting {len(entities)} rows into {table}...")
    db.execute_batch_insert(query, params_list)

def purge_database(db):
    """Purge all data from all tables in the correct order (respecting foreign keys)."""
    logger.info("Purging database tables...")
    
    try:
        # Temporarily disable foreign key checks
        db.execute_query("SET session_replication_role = replica;")
        
        # Purge all tables
        tables_to_purge = [
            'mantenimiento', 'incidencia', 'bloqueo', 'pedido', 'vehiculo', 'almacen'
        ]
        
        for table in tables_to_purge:
            try:
                db.execute_insert(f"DELETE FROM {table}")
                logger.info(f"Purged table: {table}")
            except Exception as e:
                logger.warning(f"Could not purge {table}: {e}")
        
        # Re-enable foreign key checks
        db.execute_query("SET session_replication_role = DEFAULT;")
        
    except Exception as e:
        logger.error(f"Error during purge: {e}")
        # Try to re-enable foreign key checks even if purge failed
        try:
            db.execute_query("SET session_replication_role = DEFAULT;")
        except:
            pass

def main():
    db = get_db_connection()
    if not db:
        logger.error("Could not connect to database.")
        sys.exit(1)
    try:
        # Purge existing data
        purge_database(db)
        
        # Orders
        all_orders = []
        ventas_pattern = re.compile(r"ventas(\d{4})(\d{2})\.txt")
        for filename in os.listdir(DATA_DIR):
            match = ventas_pattern.match(filename)
            if match:
                logger.info(f"Parsing sales file: {filename}")
                year = int(match.group(1))
                month = int(match.group(2))
                filepath = os.path.join(DATA_DIR, filename)
                orders = parse_orders(filepath, year, month)
                all_orders.extend(orders)
        
        insert_entities(db, 'pedido', all_orders, [
            'codigo_cliente', 'fecha_registro', 'posicionx', 'posiciony', 'cantidadglp', 'tiempo_tolerancia'
        ])
        # Vehicles
        vehicles = parse_vehicles(os.path.join(DATA_DIR, TXT_FILES['vehicles']))
        insert_entities(db, 'vehiculo', vehicles, [
            'tipo', 'placa', 'peso', 'max_combustible', 'curr_combustible', 'max_glp', 'curr_glp', 'posicionx', 'posiciony', 'disponible'
        ])
        # Warehouses
        warehouses = parse_warehouses(os.path.join(DATA_DIR, TXT_FILES['warehouses']))
        insert_entities(db, 'almacen', warehouses, [
            'posicionx', 'posiciony', 'capacidad_efectivam3', 'es_principal', 'horario_abastecimiento'
        ])
        # Failures (as Incidencia)
        failures = parse_failures(os.path.join(DATA_DIR, TXT_FILES['failures']), db)
        insert_entities(db, 'incidencia', failures, [
            'fecha', 'turno', 'vehiculo_id', 'ocurrido'
        ])
        # Blockages
        all_blockages = []
        blockage_pattern = re.compile(r"(\d{4})(\d{2})\.bloqueos\.txt")
        for filename in os.listdir(DATA_DIR):
            match = blockage_pattern.match(filename)
            if match:
                logger.info(f"Parsing blockages file: {filename}")
                year = int(match.group(1))
                month = int(match.group(2))
                filepath = os.path.join(DATA_DIR, filename)
                blockages = parse_blockages(filepath, year, month)
                all_blockages.extend(blockages)
        
        insert_entities(db, 'bloqueo', all_blockages, [
            'start_time', 'end_time', 'vertices_json'
        ])
        # Maintenances
        all_maintenances = []
        maintenance_pattern = re.compile(r"mantenimientos(\d{4})(\d{2})\.txt")
        for filename in os.listdir(DATA_DIR):
            match = maintenance_pattern.match(filename)
            if match:
                logger.info(f"Parsing maintenances file: {filename}")
                filepath = os.path.join(DATA_DIR, filename)
                maintenances = parse_maintenances(filepath, db)
                all_maintenances.extend(maintenances)

        insert_entities(db, 'mantenimiento', all_maintenances, [
            'vehiculo_id', 'start_time', 'end_time'
        ])
        logger.info("All data inserted successfully.")
    finally:
        db.disconnect()

if __name__ == '__main__':
    main() 