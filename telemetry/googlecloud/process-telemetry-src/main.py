import json
import logging
from google.cloud.sql.connector import Connector, IPTypes
import pg8000
from google.cloud import storage
import os

class TelemetryProcessingError(Exception):
    """Class telemetry processing errors."""
    pass


connector = None

def main(event, context):
    
    bucket_name = event['bucket']
    file_name = event['name']

    logging.info("Processing file %s", file_name);
    
    storage_client = storage.Client()
    bucket = storage_client.bucket(bucket_name)
    blob = bucket.blob(file_name)

    try:
        # Download file contents
        filedata = blob.download_as_text()
        data = json.loads(filedata)

        # Write payload to your database
        try:
            write_to_db(data)
            # Delete file after successful processing; otherwise keep the file
            blob.delete()
        except Exception as e:
            logging.error("Error processing file %s: %s", file_name, e, exc_info=True)
        

    except Exception as e:
        logging.error("Error occurred: %s", e, exc_info=True)
        

def get_connection():

    global connector   
    if connector is None:
        connector = Connector(refresh_strategy="lazy")
        
    project = os.getenv("PROJECT_ID")
    region = os.getenv("REGION")
    instance = os.getenv("DB_INSTANCE_NAME")
    
    dbuser = os.getenv("DB_USER")
    dbpassword = os.getenv("DB_PASSWORD")
    dbname = os.getenv("DB_NAME")
    
    connectstr = f"{project}:{region}:{instance}"
    
    try:
        return connector.connect(
            connectstr,
            "pg8000",
            user=dbuser,
            password=dbpassword,
            db=dbname,
            ip_type=IPTypes.PRIVATE,
            timeout=30 
        )

    except Exception as e:
        logging.error("Connection Failed: %s", e, exc_info=True)
        raise

            
def write_to_db(data):
    
    sql_desktop_insert = "INSERT INTO telemetry.desktop (smart_install_key, last_uploaded_utc, os_name, os_version, os_arch, smart_version) VALUES (%s, %s, %s, %s, %s, %s)"
    sql_version_insert = "INSERT INTO telemetry.desktop_version (smart_install_key, plugin_id, version) VALUES (%s, %s, %s)"
    sql_stat_insert = "INSERT INTO telemetry.desktop_stat (smart_install_key, month, key, count) VALUES (%s, %s, %s, %s)"
        
    if not data or data is None:
        raise TelemetryProcessingError("No data in file");            
        
    # -- get install key
    installkey = data.get('installKey')
    if not installkey:
        raise TelemetryProcessingError("No install key in json data");

    with get_connection() as conn:
        cur = conn.cursor()
        try:
            # -- delete existing
            cur.execute("DELETE FROM telemetry.desktop where smart_install_key = %s", (installkey,))

            # -- save details
            cur.execute(sql_desktop_insert,(installkey, data.get('datetime'), data.get('os.name'), data.get('os.version'), data.get('os.arch'), data.get('smart.version')))
            
            # -- save version
            versions = data.get('db_version')
            if versions and isinstance(versions, dict):
                for plugin_id, version in versions.items():
                    cur.execute(sql_version_insert,(installkey, plugin_id, version))      
            
            # -- save status
            stats = data.get('stats')
            if stats:
                for key, value in stats.items():
                    if key.startswith("data."):
                        cur.execute(sql_stat_insert, (installkey, None, key, value))                     
                    elif key.startswith("usage.") and isinstance(value, dict):
                        for month, count in value.items():
                            cur.execute(sql_stat_insert, (installkey, month, key, count))
            # -- commit
            conn.commit()
        finally:
            cur.close();               