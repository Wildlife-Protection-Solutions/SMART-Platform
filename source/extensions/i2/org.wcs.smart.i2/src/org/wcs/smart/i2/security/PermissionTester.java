package org.wcs.smart.i2.security;

import java.util.Locale;

import org.eclipse.core.expressions.PropertyTester;
import org.wcs.smart.ca.SmartUserLevel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.user.UserLevelManager;

public class PermissionTester extends PropertyTester {
	
	/**
	 * Tests the current logged in user level against the level
	 * provided by the expectedValue.  
	 * 
	 * @param expectedValue one of the smart user level keys
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		
		if (SmartDB.getCurrentEmployee() == null) return false;
		
		if ( expectedValue.toString().equalsIgnoreCase(IntelAdminUserLevel.INSTANCE.getKey())){
			return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelAdminUserLevel.INSTANCE);
		}else if ( expectedValue.toString().equalsIgnoreCase(IntelUserUserLevel.INSTANCE.getKey())){
			return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelUserUserLevel.INSTANCE);
		}else if ( expectedValue.toString().equalsIgnoreCase("create_record")){
			return IntelSecurityManager.INSTANCE.canCreateRecordAny();
		}else if ( expectedValue.toString().equalsIgnoreCase("create_entity")){
			return IntelSecurityManager.INSTANCE.canCreateEntityAny();
		}else if ( expectedValue.toString().equalsIgnoreCase("create_query")){
			return IntelSecurityManager.INSTANCE.canCreateQueryAny();
		}else if ( expectedValue.toString().equalsIgnoreCase("assessment_perspective")){
			return IntelSecurityManager.INSTANCE.canViewRecordAny();
		}else if ( expectedValue.toString().equalsIgnoreCase("analyst_perspective")){
			return IntelSecurityManager.INSTANCE.canViewEntityAny() ||
					IntelSecurityManager.INSTANCE.canViewQueryAny();
		}else if ( expectedValue.toString().equalsIgnoreCase("entity_perspective")){
			return IntelSecurityManager.INSTANCE.canViewEntityAny();
		}
		return false;
//		Permission p = null;
//		try {
//			p = Permission.valueOf(expectedValue.toString().toUpperCase(Locale.ROOT));
//		}catch (Exception ex) {
//			//not supported
//			ex.printStackTrace();
//			return false;
//		}
//		switch(p) {
//		case ANALYST_PERSPECTIVE:
//			SmartUserLevel[] intelUserLevels = new SmartUserLevel[] {
//					IntelAnalystUserLevel.INSTANCE,
//					IntelUserUserLevel.INSTANCE,
//					IntelCreateRecordUserLevel.INSTANCE,
//					//IntelDataEntryUserLevel.INSTANCE,
//					IntelDeleteEntityUserLevel.INSTANCE,
//					IntelEditEntityUserLevel.INSTANCE,
//					IntelDeleteRecordUserLevel.INSTANCE,
//					IntelEditRecordUserLevel.INSTANCE,
//					IntelEditRecordWithStatusUserLevel.INSTANCE,
//					IntelReadOnlyUserLevel.INSTANCE,
//					IntelViewEntityUserLevel.INSTANCE,
//					IntelViewRecordsUserLevel.INSTANCE,
//					IntelQueryAllUserLevel.INSTANCE
//			};
//			for (SmartUserLevel l : intelUserLevels) {
//				if (UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), l)) return true;
//			}
//			return false;
//		case ASSESSMENT_PERSPECTIVE:
//			return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelAnalystUserLevel.INSTANCE) || 
//					//UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelDataEntryUserLevel.INSTANCE) ||
//					UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelCreateRecordUserLevel.INSTANCE) || 
//					UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelEditRecordUserLevel.INSTANCE) ||
//					UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelEditRecordWithStatusUserLevel.INSTANCE) ||
//					UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), IntelDeleteRecordUserLevel.INSTANCE);
//		case ENTITY_PERSPECTIVE:
//			intelUserLevels = new SmartUserLevel[] {
//					IntelAnalystUserLevel.INSTANCE,
//					IntelUserUserLevel.INSTANCE,
//					IntelCreateRecordUserLevel.INSTANCE,
//					IntelEditEntityUserLevel.INSTANCE,
//					IntelDeleteEntityUserLevel.INSTANCE,
//					IntelEditRecordUserLevel.INSTANCE,
//					IntelEditRecordWithStatusUserLevel.INSTANCE,
//					IntelDeleteRecordUserLevel.INSTANCE,
//					IntelReadOnlyUserLevel.INSTANCE,
//					IntelViewEntityUserLevel.INSTANCE,
//					IntelViewRecordsUserLevel.INSTANCE
//			};
//			for (SmartUserLevel l : intelUserLevels) {
//				if (UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), l)) return true;
//			}
//			
//			
//			return false;
//		case CONFIGURE:
//			return IntelSecurityManager.INSTANCE.canConfigureAny();
//		case CREATE_ENTITY:
//			return IntelSecurityManager.INSTANCE.canCreateEntityAny();
//		case CREATE_QUERY:
//			return IntelSecurityManager.INSTANCE.canCreateQuery();
//		case CREATE_RECORD:
//			return IntelSecurityManager.INSTANCE.canCreateRecordAny();
//		case EXPORT_DATA:
//			break;
//		case IMPORT_DATA:
//			return IntelSecurityManager.INSTANCE.canConfigureAny();
//		}
//		return false;
	}


}
