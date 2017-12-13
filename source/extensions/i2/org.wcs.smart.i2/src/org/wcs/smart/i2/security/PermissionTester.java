package org.wcs.smart.i2.security;

import org.eclipse.core.expressions.PropertyTester;
import org.wcs.smart.ca.SmartUserLevel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.user.UserLevelManager;

public class PermissionTester extends PropertyTester {

	public static enum Permission{
		ANALYST_PERSPECTIVE,
		ASSESSMENT_PERSPECTIVE,
		IMPORT_DATA,
		EXPORT_DATA,
		CREATE_RECORD,
		CREATE_QUERY,
		CREATE_ENTITY,
		CONFIGURE
	}
	
	/**
	 * Tests the current logged in user level against the level
	 * provided by the expectedValue.  
	 * 
	 * @param expectedValue one of the smart user level keys 
	 * "admin", "manager", "analyst", "dataentry" or others supplied by a plugin
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		
		if (SmartDB.getCurrentEmployee() == null) return false;
		Permission p = null;
		try {
			p = Permission.valueOf(expectedValue.toString().toUpperCase());
		}catch (Exception ex) {
			//not supported
			ex.printStackTrace();
			return false;
		}
		switch(p) {
		case ANALYST_PERSPECTIVE:
			SmartUserLevel[] intelUserLevels = new SmartUserLevel[] {
					IntelAnalystUserLevel.INSTANCE,
					IntelCreateEntityUserLevel.INSTANCE,
					IntelCreateRecordUserLevel.INSTANCE,
					//IntelDataEntryUserLevel.INSTANCE,
					IntelEditEntityUserLevel.INSTANCE,
					IntelEditRecordUserLevel.INSTANCE,
					IntelReadOnlyUserLevel.INSTANCE,
					IntelViewEntityUserLevel.INSTANCE,
					IntelViewRecordsUserLevel.INSTANCE,
					IntelQueryAllUserLevel.INSTANCE
			};
			for (SmartUserLevel l : intelUserLevels) {
				if (UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), l)) return true;
			}
			return false;
		case ASSESSMENT_PERSPECTIVE:
			return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelAnalystUserLevel.INSTANCE) || 
					//UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelDataEntryUserLevel.INSTANCE) ||
					UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelCreateRecordUserLevel.INSTANCE) || 
					UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelEditRecordUserLevel.INSTANCE);
		case CONFIGURE:
			return IntelSecurityManager.INSTANCE.canConfigure();
		case CREATE_ENTITY:
			return IntelSecurityManager.INSTANCE.canCreateEntity();
		case CREATE_QUERY:
			return IntelSecurityManager.INSTANCE.canCreateQuery();
		case CREATE_RECORD:
			return IntelSecurityManager.INSTANCE.canCreateRecord();
		case EXPORT_DATA:
			break;
		case IMPORT_DATA:
			return IntelSecurityManager.INSTANCE.canConfigure();
		}
		return false;
	}


}
