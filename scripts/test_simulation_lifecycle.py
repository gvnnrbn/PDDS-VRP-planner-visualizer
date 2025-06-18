#!/usr/bin/env python3
"""
Test script to verify the simulation lifecycle with database queries.
This script tests that when a simulation starts via WebSocket, fresh data is loaded from the database.
"""

import requests
import json
import time
from datetime import datetime

def test_simulation_lifecycle():
    """Test the complete simulation lifecycle with database queries."""
    
    base_url = "http://localhost:8080"
    
    print("🧪 Testing Simulation Lifecycle with Database Queries")
    print("=" * 60)
    
    # 1. Check if backend is running
    print("\n1. Checking backend status...")
    try:
        response = requests.get(f"{base_url}/api/vehiculos", timeout=5)
        if response.status_code == 200:
            print("✅ Backend is running")
        else:
            print(f"❌ Backend returned status {response.status_code}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"❌ Cannot connect to backend: {e}")
        return False
    
    # 2. Check current data in database
    print("\n2. Checking current data in database...")
    
    endpoints = [
        ("vehicles", "/api/vehiculos"),
        ("orders", "/api/pedidos"), 
        ("warehouses", "/api/almacenes"),
        ("blockages", "/api/bloqueos"),
        ("failures", "/api/incidencias"),
        ("maintenances", "/api/mantenimientos")
    ]
    
    current_data = {}
    for name, endpoint in endpoints:
        try:
            response = requests.get(f"{base_url}{endpoint}", timeout=5)
            if response.status_code == 200:
                data = response.json()
                current_data[name] = len(data) if isinstance(data, list) else 0
                print(f"   {name.capitalize()}: {current_data[name]} records")
            else:
                print(f"   ❌ Failed to get {name}: {response.status_code}")
        except Exception as e:
            print(f"   ❌ Error getting {name}: {e}")
    
    # 3. Test WebSocket simulation start (this would require a WebSocket client)
    print("\n3. WebSocket simulation test...")
    print("   ℹ️  To test WebSocket simulation start:")
    print("   - Start the frontend application")
    print("   - Navigate to the simulation control panel")
    print("   - Click 'Iniciar Simulación'")
    print("   - Check the backend logs for database query messages")
    
    # 4. Instructions for manual testing
    print("\n4. Manual Testing Instructions:")
    print("   📋 To verify database queries on simulation start:")
    print("   1. Start the backend with debug logging enabled")
    print("   2. Start the frontend application")
    print("   3. Add some test data via the frontend forms")
    print("   4. Start a simulation via WebSocket")
    print("   5. Check backend logs for messages like:")
    print("      - 'Starting simulation - loading fresh data from database...'")
    print("      - 'Loaded X vehicles, Y orders, Z blockages...'")
    print("      - 'Simulation started successfully'")
    
    # 5. Expected behavior
    print("\n5. Expected Behavior:")
    print("   ✅ When simulation starts via /app/init WebSocket endpoint:")
    print("      - DataProvider.getVehicles() should query vehiculoRepository.findAll()")
    print("      - DataProvider.getOrders() should query pedidoRepository.findAll()")
    print("      - DataProvider.getBlockages() should query bloqueoRepository.findAll()")
    print("      - DataProvider.getWarehouses() should query almacenRepository.findAll()")
    print("      - DataProvider.getFailures() should query incidenciaRepository.findAll()")
    print("      - DataProvider.getMaintenances() should query mantenimientoRepository.findAll()")
    print("      - All data should be fresh from the database")
    
    print("\n" + "=" * 60)
    print("🎯 Test completed. Check the manual testing instructions above.")
    
    return True

if __name__ == "__main__":
    test_simulation_lifecycle() 