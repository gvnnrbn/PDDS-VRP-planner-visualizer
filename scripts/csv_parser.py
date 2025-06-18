"""
CSV parser utility for reading and processing CSV data files.
Provides common functionality for parsing different CSV formats.
"""

import pandas as pd
import os
import logging
from config import DATA_DIR, CSV_FILES

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


class CSVParser:
    """CSV parser for handling different data file formats."""
    
    def __init__(self):
        self.data_dir = DATA_DIR
    
    def read_csv(self, filename, **kwargs):
        """
        Read a CSV file and return a pandas DataFrame.
        
        Args:
            filename (str): Name of the CSV file
            **kwargs: Additional arguments to pass to pandas.read_csv()
        
        Returns:
            pandas.DataFrame: Parsed CSV data
        """
        file_path = os.path.join(self.data_dir, filename)
        
        try:
            if not os.path.exists(file_path):
                logger.error(f"File not found: {file_path}")
                return None
            
            # Default parameters for CSV reading
            default_params = {
                'encoding': 'utf-8',
                'delimiter': ',',
                'header': 0,
                'skip_blank_lines': True
            }
            
            # Update with any provided parameters
            default_params.update(kwargs)
            
            df = pd.read_csv(file_path, **default_params)
            logger.info(f"Successfully read {filename}: {len(df)} rows, {len(df.columns)} columns")
            return df
            
        except Exception as e:
            logger.error(f"Error reading CSV file {filename}: {e}")
            return None
    
    def get_csv_info(self, filename):
        """
        Get basic information about a CSV file.
        
        Args:
            filename (str): Name of the CSV file
        
        Returns:
            dict: Information about the CSV file
        """
        df = self.read_csv(filename)
        if df is None:
            return None
        
        info = {
            'filename': filename,
            'rows': len(df),
            'columns': len(df.columns),
            'column_names': list(df.columns),
            'data_types': df.dtypes.to_dict(),
            'missing_values': df.isnull().sum().to_dict(),
            'sample_data': df.head(3).to_dict('records')
        }
        
        return info
    
    def validate_csv_structure(self, filename, expected_columns):
        """
        Validate that a CSV file has the expected column structure.
        
        Args:
            filename (str): Name of the CSV file
            expected_columns (list): List of expected column names
        
        Returns:
            bool: True if structure is valid, False otherwise
        """
        df = self.read_csv(filename)
        if df is None:
            return False
        
        actual_columns = list(df.columns)
        missing_columns = set(expected_columns) - set(actual_columns)
        extra_columns = set(actual_columns) - set(expected_columns)
        
        if missing_columns:
            logger.error(f"Missing columns in {filename}: {missing_columns}")
            return False
        
        if extra_columns:
            logger.warning(f"Extra columns in {filename}: {extra_columns}")
        
        logger.info(f"CSV structure validation passed for {filename}")
        return True
    
    def clean_data(self, df):
        """
        Basic data cleaning operations.
        
        Args:
            df (pandas.DataFrame): DataFrame to clean
        
        Returns:
            pandas.DataFrame: Cleaned DataFrame
        """
        if df is None:
            return None
        
        # Remove duplicate rows
        initial_rows = len(df)
        df = df.drop_duplicates()
        if len(df) < initial_rows:
            logger.info(f"Removed {initial_rows - len(df)} duplicate rows")
        
        # Remove rows with all NaN values
        initial_rows = len(df)
        df = df.dropna(how='all')
        if len(df) < initial_rows:
            logger.info(f"Removed {initial_rows - len(df)} completely empty rows")
        
        # Strip whitespace from string columns
        for col in df.select_dtypes(include=['object']).columns:
            df[col] = df[col].astype(str).str.strip()
        
        return df
    
    def list_available_files(self):
        """
        List all available CSV files in the data directory.
        
        Returns:
            list: List of available CSV files
        """
        available_files = []
        
        for file_type, filename in CSV_FILES.items():
            file_path = os.path.join(self.data_dir, filename)
            if os.path.exists(file_path):
                available_files.append({
                    'type': file_type,
                    'filename': filename,
                    'path': file_path,
                    'size': os.path.getsize(file_path)
                })
            else:
                logger.warning(f"CSV file not found: {filename}")
        
        return available_files


def get_csv_parser():
    """Factory function to get a CSV parser instance."""
    return CSVParser() 