# CSV Data Parsing Scripts

This directory contains Python scripts for parsing CSV data files and inserting them into the PostgreSQL database used by the PDDS VRP planner application.

## Structure

```
scripts/
├── data/                    # CSV data files
│   ├── orders.csv          # Order data
│   ├── vehicles.csv        # Vehicle data
│   ├── warehouses.csv      # Warehouse data
│   ├── blockages.csv       # Road blockage data
│   ├── maintenances.csv    # Vehicle maintenance data
│   └── failures.csv        # Vehicle failure data
├── config.py               # Database and file configuration
├── db_connection.py        # PostgreSQL connection utilities
├── csv_parser.py           # CSV parsing utilities
├── main.py                 # Main script and setup demonstration
├── requirements.txt        # Python dependencies
└── README.md              # This file
```

## Setup

1. **Install Python dependencies:**
   ```bash
   cd scripts
   pip install -r requirements.txt
   ```

2. **Ensure PostgreSQL is running:**
   - The scripts use the same database credentials as the backend application
   - Default configuration: `localhost:5432/postgres` with user `postgres`

3. **Verify CSV files are present:**
   - All CSV files should be in the `data/` directory
   - Check that files match the expected names in `config.py`

## Usage

### Running the main script

```bash
cd scripts
python main.py
```

This will:
- Test the database connection
- List available CSV files
- Show information about each CSV file
- Demonstrate the parsing setup

### Database Configuration

The database configuration in `config.py` matches the backend application:

```python
DB_CONFIG = {
    'host': 'localhost',
    'port': 5432,
    'database': 'postgres',
    'user': 'postgres',
    'password': 'mysecretpassword'
}
```

### CSV File Structure

The scripts expect the following CSV files:

- **orders.csv**: Order data with customer information and delivery requirements
- **vehicles.csv**: Vehicle fleet information
- **warehouses.csv**: Warehouse locations and capacities
- **blockages.csv**: Road blockage information
- **maintenances.csv**: Vehicle maintenance schedules
- **failures.csv**: Vehicle failure records

## Components

### Database Connection (`db_connection.py`)

Provides PostgreSQL connection management with:
- Connection establishment and cleanup
- Query execution (single and batch)
- Table existence checking
- Column information retrieval

### CSV Parser (`csv_parser.py`)

Handles CSV file operations:
- File reading with pandas
- Data validation and cleaning
- Structure validation
- File information retrieval

### Configuration (`config.py`)

Centralized configuration for:
- Database connection parameters
- CSV file mappings
- Data directory paths

## Next Steps

To implement entity-specific parsing:

1. Create entity-specific parser modules (e.g., `order_parser.py`, `vehicle_parser.py`)
2. Implement data transformation logic for each entity type
3. Create database insertion queries matching the backend model structure
4. Add validation and error handling for each entity type

## Dependencies

- `psycopg2-binary`: PostgreSQL adapter for Python
- `pandas`: Data manipulation and CSV parsing
- `python-dotenv`: Environment variable management (optional)

## Notes

- The scripts use the same database credentials as the Spring Boot backend
- All database operations include proper error handling and logging
- The setup is designed to be extensible for different entity types
- CSV parsing includes basic data cleaning and validation 