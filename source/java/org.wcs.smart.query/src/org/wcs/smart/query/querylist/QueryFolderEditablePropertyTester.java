package org.wcs.smart.query.querylist;

import org.eclipse.core.expressions.PropertyTester;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.QueryInput;

public class QueryFolderEditablePropertyTester extends PropertyTester {

	public QueryFolderEditablePropertyTester() {
	}

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		
		if (args.length != 1){
			return false;
		}
		
		String operator = (String)args[0];
		if (receiver instanceof QueryFolder){
			if (operator.equals("rename") || operator.equals("delete")){
				if (((QueryFolder)receiver).isRootFolder()){
					return false;
				}
				//if (((QueryFolder) receiver).getEmployee() != null && ((QueryFolder) receiver).getEmployee().equals(SmartDB.getCurrentEmployee())){
				if (((QueryFolder)receiver).getEmployee() != null){
					return true;
				}else if (((QueryFolder) receiver).getEmployee() == null ){
					//conservation area level folder; only rename or deletable if admin or manager
					if (SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.ADMIN || 
							SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.MANAGER
							){
						return true;
					}
				}
				
			}else if (operator.equals("newfolder")){
				if (((QueryFolder)receiver).getEmployee() != null){
					return true;
				}else if (((QueryFolder) receiver).getEmployee() == null ){
					//conservation area level folder; only rename or deletable if admin or manager
					if (SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.ADMIN || 
							SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.MANAGER
							){
						return true;
					}
				}
				
			}
			
			return false;
			
		}else if (receiver instanceof QueryInput){
			if (operator.equals("delete")){
				if (SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.ADMIN || 
						SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.MANAGER
						){
					return true;
				}else{
					if (((QueryInput)receiver).isShared()){
						return false;
					}else{
						return true;
					}
					
				}
				
			}
		}
		return false;
	}

}
