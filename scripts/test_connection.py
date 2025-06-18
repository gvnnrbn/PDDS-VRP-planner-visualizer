"""
Simple test script to verify database connection and basic operations.
"""

import sys
from db_connection import get_db_connection

def test_connection():
    """Test database connection and basic operations."""
    print("Testing database connection...")
    
    # Get database connection
    db = get_db_connection()
    if not db:
        print("❌ Failed to connect to database")
        return False
    
    try:
        # Test basic query
        print("✅ Database connection successful")
        
        # Test version query
        result = db.execute_query("SELECT version();")
        if result:
            print(f"✅ PostgreSQL version: {result[0]['version']}")
        
        # Test table listing
        tables = db.execute_query("""
            SELECT table_name 
            FROM information_schema.tables 
            WHERE table_schema = 'public' 
            ORDER BY table_name;
        """)
        
        if tables:
            print(f"✅ Found {len(tables)} tables:")
            for table in tables:
                print(f"   - {table['table_name']}")
        else:
            print("ℹ️  No tables found in database")
        
        # Test table structure for existing tables
        if tables:
            print("\nTesting table structure...")
            for table in tables:
                table_name = table['table_name']
                columns = db.get_table_columns(table_name)
                if columns:
                    print(f"✅ Table '{table_name}' has {len(columns)} columns:")
                    for col in columns:
                        nullable = "NULL" if col['is_nullable'] == 'YES' else "NOT NULL"
                        print(f"   - {col['column_name']}: {col['data_type']} ({nullable})")
        
        print("\n✅ All tests passed!")
        return True
        
    except Exception as e:
        print(f"❌ Error during testing: {e}")
        return False
    
    finally:
        db.disconnect()

if __name__ == "__main__":
    success = test_connection()
    sys.exit(0 if success else 1) 