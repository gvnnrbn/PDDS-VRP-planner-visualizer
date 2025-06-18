"""
Database connection utility for PostgreSQL.
Provides connection management and basic database operations.
"""

import psycopg2
import psycopg2.extras
from psycopg2 import Error
from config import DB_CONFIG
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class DatabaseConnection:
    """Database connection manager for PostgreSQL."""
    
    def __init__(self):
        self.connection = None
        self.cursor = None
    
    def connect(self):
        """Establish connection to PostgreSQL database."""
        try:
            self.connection = psycopg2.connect(**DB_CONFIG)
            self.cursor = self.connection.cursor(cursor_factory=psycopg2.extras.RealDictCursor)
            logger.info("Successfully connected to PostgreSQL database")
            return True
        except Error as e:
            logger.error(f"Error connecting to PostgreSQL: {e}")
            return False
    
    def disconnect(self):
        """Close database connection."""
        if self.cursor:
            self.cursor.close()
        if self.connection:
            self.connection.close()
            logger.info("Database connection closed")
    
    def execute_query(self, query, params=None):
        """Execute a query and return results."""
        try:
            self.cursor.execute(query, params)
            return self.cursor.fetchall()
        except Error as e:
            logger.error(f"Error executing query: {e}")
            self.connection.rollback()
            return None
    
    def execute_insert(self, query, params=None):
        """Execute an insert query and commit."""
        try:
            self.cursor.execute(query, params)
            self.connection.commit()
            return True
        except Error as e:
            logger.error(f"Error executing insert: {e}")
            self.connection.rollback()
            return False
    
    def execute_batch_insert(self, query, params_list):
        """Execute batch insert operations."""
        try:
            psycopg2.extras.execute_batch(self.cursor, query, params_list)
            self.connection.commit()
            logger.info(f"Successfully inserted {len(params_list)} records")
            return True
        except Error as e:
            logger.error(f"Error executing batch insert: {e}")
            self.connection.rollback()
            return False
    
    def table_exists(self, table_name):
        """Check if a table exists in the database."""
        query = """
        SELECT EXISTS (
            SELECT FROM information_schema.tables 
            WHERE table_schema = 'public' 
            AND table_name = %s
        );
        """
        result = self.execute_query(query, (table_name,))
        return result[0]['exists'] if result else False
    
    def get_table_columns(self, table_name):
        """Get column information for a table."""
        query = """
        SELECT column_name, data_type, is_nullable
        FROM information_schema.columns
        WHERE table_schema = 'public' 
        AND table_name = %s
        ORDER BY ordinal_position;
        """
        return self.execute_query(query, (table_name,))


def get_db_connection():
    """Factory function to get a database connection."""
    db = DatabaseConnection()
    if db.connect():
        return db
    return None 