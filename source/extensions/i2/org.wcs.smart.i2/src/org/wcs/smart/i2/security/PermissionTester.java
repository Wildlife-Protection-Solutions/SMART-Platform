package org.wcs.smart.i2.security;

import org.eclipse.core.expressions.PropertyTester;
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

	}


}
