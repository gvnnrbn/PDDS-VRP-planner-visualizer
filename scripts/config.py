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

# Supported CSV files
CSV_FILES = {
    'orders': 'orders.csv',
    'vehicles': 'vehicles.csv',
    'warehouses': 'warehouses.csv',
    'blockages': 'blockages.csv',
    'maintenances': 'maintenances.csv',
    'failures': 'failures.csv'
} 