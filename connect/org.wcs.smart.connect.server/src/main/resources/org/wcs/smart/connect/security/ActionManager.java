package org.wcs.smart.connect.security;

import java.util.ArrayList;
import java.util.List;

public enum ActionManager {
	INSTANCE;
	
	private List<ISmartConnectAction> actions;
	
	private ActionManager(){
		actions = new ArrayList<ISmartConnectAction>();
		actions.add(new AdminAccountAction());
		actions.add(new UserAccountsAction());
		actions.add(new CaAction());
	}
	
	public ISmartConnectAction findAction(String key){
		for (ISmartConnectAction a : actions){
			for (String akey : a.getActionKeys()){
				if (akey.equals(key)){
					return a;
				}
			}
		}
		return null;
	}
	
	public List<ISmartConnectAction> getAllActions(){
		return actions;
	}
}
