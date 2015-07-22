package org.wcs.smart.connect.security;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.connect.i18n.Messages;

public class UserAccountsAction implements ISmartConnectAction{

	public static final String KEY = "useraccounts"; //$NON-NLS-1$
	
	@Override
	public String getActionName(String actionKey, Locale l) {
		if (actionKey.equals(KEY)){
			return Messages.getString("UserAccountsAction.UserAcctPermissions", l); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public String[] getActionKeys() {
		return new String[]{KEY};
	}

	@Override
	public List<ResourceOption> getResourceOptions(String actionKey, Session session, Locale l) {
		return null;
	}

	@Override
	public String getResourceName(UUID resource, Session session, Locale l) {
		return null;
	}

}
