package org.wcs.smart.cybertracker.patrol.model;

public class PatrolMetadata {

	public static enum JsonPatrolKey {
		TRANSPORT_TYPE("tt"), //$NON-NLS-1$
		MANDATE("pm"), //$NON-NLS-1$
		STATION("ps"), //$NON-NLS-1$
		TEAM("pt"); //$NON-NLS-1$
		
		public String key;
		
		private JsonPatrolKey(String key){
			this.key = key;
		}
	}
	
}
