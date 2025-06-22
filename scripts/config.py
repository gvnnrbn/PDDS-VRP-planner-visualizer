"""
Database configuration for CSV parsing scripts.
Credentials match the backend application.properties configuration.
"""

# Database configuration
DB_CONFIG = {
    'host': 'localhost',
    'port': 5432,
    'database': 'postgres',
    'user': 'postgres',
    'password': 'mysecretpassword'
}

# CSV data directory
DATA_DIR = 'data'

# Supported files
TXT_FILES = {
    'vehicles': 'vehiculos.txt',
    'warehouses': 'almacenes.txt',
    'failures': 'averias.txt'
} 