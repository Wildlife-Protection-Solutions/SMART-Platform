package org.wcs.smart.i2;

import org.wcs.smart.hibernate.SmartDB;

public enum IntelSecurityManager {

	INSTANCE;
	
	/**
	 * Determine if the current user can edit entities records
	 * @return
	 */
	public boolean canDeleteRecord(){
		return SmartDB.getCurrentEmployee().supportsUser(IntelAnalystUserLevel.INSTANCE);
	}
	
	/**
	 * True if current user can link attachments to entities
	 * @return
	 */
	public boolean canLinkAttachmentsToEntities(){
		return SmartDB.getCurrentEmployee().supportsUser(IntelAnalystUserLevel.INSTANCE);
	}
	/**
	 * True if current user can link locations to entities
	 * @return
	 */
	public boolean canLinkLocationsToEntities(){
		return SmartDB.getCurrentEmployee().supportsUser(IntelAnalystUserLevel.INSTANCE);
	}
	/**
	 * Determine if the current user can edit entities records
	 * @return
	 */
	public boolean canEditEntity(){
		return SmartDB.getCurrentEmployee().supportsUser(IntelAnalystUserLevel.INSTANCE);
	}
	
	/**
	 * Determine if the current user can view and modify
	 * working sets
	 * 
	 * @return
	 */
	public boolean canViewWorkingSets(){
		return SmartDB.getCurrentEmployee().supportsUser(IntelAnalystUserLevel.INSTANCE);
	}
	
	/**
	 * Determine if the current user can view and modify queries
	 * 
	 * @return
	 */
	public boolean canViewQueries(){
		return SmartDB.getCurrentEmployee().supportsUser(IntelAnalystUserLevel.INSTANCE);
	}
}
