package org.wcs.smart.observation.query;

import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.hibernate.HibernateManager;

public class DatabaseUtils {

	private static final String DB_VERSION = "1.0.0";
	
	private void validateAndUpgrade() throws Exception{
		
		
	}
	
	public static void createTablesDerby(){
		final String sql[] = new String[24];
		
		int i = 0;
		sql[i++]= "CREATE TABLE smart.obs_waypoint_query"
		+"("
			+"UUID CHAR(16) FOR BIT DATA NOT NULL,"
			+"CREATOR_UUID CHAR(16) FOR BIT DATA  NOT NULL,"
			+"QUERY_FILTER VARCHAR(32672),"
			+"CA_FILTER VARCHAR(32672),"
			+"CA_UUID CHAR(16) FOR BIT DATA NOT NULL,"
			+"FOLDER_UUID CHAR(16) FOR BIT DATA,"
			+"COLUMN_FILTER VARCHAR(32672),"
			+"SHARED BOOLEAN DEFAULT false NOT NULL,"
			+"ID VARCHAR(6) NOT NULL,"
			+"PRIMARY KEY (UUID)"
		+")";

		sql[i++] = "CREATE TABLE smart.obs_summary_query"
			+"("
			+"UUID CHAR(16) for bit data NOT NULL,"
			+"CREATOR_UUID CHAR(16) for bit data NOT NULL,"
			+"CA_FILTER VARCHAR(32672),"
			+"QUERY_DEF VARCHAR(32672),"
			+"FOLDER_UUID CHAR(16) for bit data,"
			+"SHARED BOOLEAN NOT NULL,"
			+"CA_UUID CHAR(16) for bit data NOT NULL,"
			+"ID VARCHAR(6) NOT NULL,"
			+"PRIMARY KEY (UUID)"
		+")";

		sql[i++] = "CREATE TABLE smart.obs_gridded_query"
		+"("
			+"UUID CHAR(16) for bit data NOT NULL,"
			+"CREATOR_UUID CHAR(16) for bit data NOT NULL,"
			+"QUERY_FILTER VARCHAR(32672),"
			+"CA_FILTER VARCHAR(32672),"
			+"QUERY_DEF VARCHAR(32672),"
			+"FOLDER_UUID CHAR(16) for bit data,"
			+"SHARED BOOLEAN NOT NULL,"
			+"CA_UUID CHAR(16) for bit data NOT NULL,"
			+"ID VARCHAR(6) NOT NULL,"
			+"CRS_DEFINITION VARCHAR(32672) NOT NULL,"
			+"PRIMARY KEY (UUID)"
		+")";

		sql[i++]= "CREATE TABLE smart.obs_observation_query"
				+"("
					+"UUID CHAR(16) FOR BIT DATA NOT NULL,"
					+"CREATOR_UUID CHAR(16) FOR BIT DATA  NOT NULL,"
					+"QUERY_FILTER VARCHAR(32672),"
					+"CA_FILTER VARCHAR(32672),"
					+"CA_UUID CHAR(16) FOR BIT DATA NOT NULL,"
					+"FOLDER_UUID CHAR(16) FOR BIT DATA,"
					+"COLUMN_FILTER VARCHAR(32672),"
					+"SHARED BOOLEAN DEFAULT false NOT NULL,"
					+"ID VARCHAR(6) NOT NULL,"
					+"PRIMARY KEY (UUID)"
				+")";


		sql[i++] = "ALTER TABLE smart.obs_waypoint_query"
			+" ADD constraint obswaypoint_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID)"
			+" REFERENCES smart.employee (UUID)"
			+" ON UPDATE RESTRICT"
			+" ON DELETE RESTRICT";
		
		sql[i++] = "ALTER TABLE smart.obs_waypoint_query"
			+" ADD constraint obs_waypoint_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID)"
			+" REFERENCES smart.query_folder (uuid)"
			+" ON UPDATE RESTRICT"
			+" ON DELETE RESTRICT";
		
		sql[i++] = "ALTER TABLE smart.obs_waypoint_query"
				+" ADD constraint obs_waypoint_query_ca_uuid_fk FOREIGN KEY (CA_UUID)"
				+" REFERENCES smart.conservation_area (uuid)"
				+" ON UPDATE RESTRICT"
				+" ON DELETE RESTRICT";
		
		sql[i++] = "ALTER TABLE smart.obs_observation_query"
				+" ADD constraint obsobservation_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID)"
				+" REFERENCES smart.employee (UUID)"
				+" ON UPDATE RESTRICT"
				+" ON DELETE RESTRICT";
		
		sql[i++] = "ALTER TABLE smart.obs_observation_query"
				+" ADD constraint obs_observation_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID)"
				+" REFERENCES smart.query_folder (uuid)"
				+" ON UPDATE RESTRICT"
				+" ON DELETE RESTRICT";
		
		sql[i++] = "ALTER TABLE smart.obs_observation_query"
				+" ADD constraint obs_observation_query_ca_uuid_fk FOREIGN KEY (CA_UUID)"
				+" REFERENCES smart.conservation_area (uuid)"
				+" ON UPDATE RESTRICT"
				+" ON DELETE RESTRICT";



		sql[i++] = "ALTER TABLE smart.obs_summary_query"
			+ " ADD constraint obs_summary_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID)"
			+ " REFERENCES smart.employee (UUID)"
			+ " ON UPDATE RESTRICT"
			+ " ON DELETE RESTRICT";
		
		sql[i++] = "ALTER TABLE smart.obs_summary_query"
				+ " ADD constraint obs_summary_query_ca_uuid_fk FOREIGN KEY (CA_UUID)"
				+ " REFERENCES smart.conservation_area (UUID)"
				+ " ON UPDATE RESTRICT"
				+ " ON DELETE RESTRICT";

		sql[i++] = "ALTER TABLE smart.obs_summary_query"
				+ " ADD constraint obs_summary_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID)"
				+ " REFERENCES smart.query_folder (UUID)"
				+ " ON UPDATE RESTRICT"
				+ " ON DELETE RESTRICT";

		sql[i++] = "ALTER TABLE smart.obs_gridded_query"
				+ " ADD constraint obs_gridded_query_creator_uuid_fk FOREIGN KEY (CREATOR_UUID)"
				+ " REFERENCES smart.employee (UUID)"
				+ " ON UPDATE RESTRICT"
				+ " ON DELETE RESTRICT";
			
		sql[i++] = "ALTER TABLE smart.obs_gridded_query"
				+ " ADD constraint obs_gridded_query_ca_uuid_fk FOREIGN KEY (CA_UUID)"
				+ " REFERENCES smart.conservation_area (UUID)"
				+ " ON UPDATE RESTRICT"
				+ " ON DELETE RESTRICT";

		sql[i++] = "ALTER TABLE smart.obs_gridded_query"
				+ " ADD constraint obs_gridded_query_folder_uuid_fk FOREIGN KEY (FOLDER_UUID)"
				+ " REFERENCES smart.query_folder (UUID)"
				+ " ON UPDATE RESTRICT"
				+ " ON DELETE RESTRICT";

		sql[i++] = "GRANT ALL PRIVILEGES ON smart.obs_observation_query to manager";
		sql[i++] = "GRANT ALL PRIVILEGES ON smart.obs_waypoint_query to manager";
		sql[i++] = "GRANT ALL PRIVILEGES ON smart.obs_summary_query to manager";
		sql[i++] = "GRANT ALL PRIVILEGES ON smart.obs_gridded_query to manager";
		sql[i++] = "GRANT ALL PRIVILEGES ON smart.obs_observation_query to analyst";
		sql[i++] = "GRANT ALL PRIVILEGES ON smart.obs_waypoint_query to analyst";
		sql[i++] = "GRANT ALL PRIVILEGES ON smart.obs_summary_query to analyst";
		sql[i++] = "GRANT ALL PRIVILEGES ON smart.obs_gridded_query to analyst";
		
		Session s = HibernateManager.openSession();
		s.doWork(new Work() {
			
			@Override
			public void execute(Connection c) throws SQLException {
				c.setAutoCommit(false);
				for (int k = 0; k < sql.length; k++){
					c.createStatement().execute(sql[k]);
				}
				c.commit();
			}
		});
		
	}
}
