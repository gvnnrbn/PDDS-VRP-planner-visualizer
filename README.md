# PDDS-VRP-planner-visualizer

A full-stack application for scheduling vehicle routes to deliver GLP packages with blockages, warehouses, maintenances and failures.

## Project Structure

```
PDDS-VRP-planner-visualizer/
├── backend/                 # Spring Boot backend application
├── frontend/                # React + TypeScript frontend
├── scripts/                 # Python scripts for CSV data parsing
│   ├── data/               # CSV data files
│   ├── config.py           # Database configuration
│   ├── db_connection.py    # PostgreSQL connection utilities
│   ├── csv_parser.py       # CSV parsing utilities
│   ├── main.py             # Main parsing script
│   └── README.md           # Scripts documentation
└── README.md               # This file
```

## Quick Start

### Backend
```bash
cd backend
./mvnw spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

### CSV Data Parsing
```bash
cd scripts
pip install -r requirements.txt
python main.py
```

## Database

The application uses PostgreSQL with the following default configuration:
- Host: localhost
- Port: 5432
- Database: postgres
- User: postgres
- Password: mysecretpassword

## CSV Data Processing

The `scripts/` folder contains Python utilities for parsing CSV data files and inserting them into the PostgreSQL database. See `scripts/README.md` for detailed documentation.