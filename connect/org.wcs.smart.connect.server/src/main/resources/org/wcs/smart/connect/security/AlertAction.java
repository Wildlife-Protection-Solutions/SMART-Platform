package org.wcs.smart.connect.security;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.connect.i18n.Messages;

public class AlertAction implements ISmartConnectAction{

	public static final String VIEW_ALL_KEY = "viewalerts"; //$NON-NLS-1$
	public static final String CREATE_ALERTS_KEY = "createalerts"; //$NON-NLS-1$
	public static final String UPDATE_ALL_KEY = "updatealerts"; //$NON-NLS-1$
	public static final String DELETE_ALL_KEY = "deletealerts"; //$NON-NLS-1$
	
	@Override
	public String getActionName(String actionKey, Locale l) {
		if (actionKey.equals(VIEW_ALL_KEY)){
			return Messages.getString("AlertsAction.ViewAlertsPermissions", l); //$NON-NLS-1$
		}else if(actionKey.equals(CREATE_ALERTS_KEY)){
			return Messages.getString("AlertsAction.CreateAlertsPermissions", l); //$NON-NLS-1$
		}else if(actionKey.equals(UPDATE_ALL_KEY)){
			return Messages.getString("AlertsAction.UpdateAlertsPermissions", l); //$NON-NLS-1$
		}else if(actionKey.equals(DELETE_ALL_KEY)){
			return Messages.getString("AlertsAction.DeleteAlertsPermissions", l); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public String[] getActionKeys() {
		return new String[]{VIEW_ALL_KEY, CREATE_ALERTS_KEY, UPDATE_ALL_KEY, DELETE_ALL_KEY};
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
