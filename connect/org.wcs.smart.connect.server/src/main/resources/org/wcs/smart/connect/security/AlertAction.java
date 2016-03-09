package org.wcs.smart.connect.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.ConservationAreaInfo;

public class AlertAction implements ISmartConnectAction{

	public static final String VIEW_ALERTS_KEY = "viewalerts"; //$NON-NLS-1$
	public static final String CREATE_ALERTS_KEY = "createalerts"; //$NON-NLS-1$
	public static final String UPDATE_ALERTS_KEY = "updatealerts"; //$NON-NLS-1$
	public static final String DELETE_ALERTS_KEY = "deletealerts"; //$NON-NLS-1$

	
	@Override
	public String getActionName(String actionKey, Locale l) {
		if (actionKey.equals(VIEW_ALERTS_KEY)){
			return Messages.getString("AlertsAction.ViewAlertsPermissions", l); //$NON-NLS-1$
		}else if(actionKey.equals(CREATE_ALERTS_KEY)){
			return Messages.getString("AlertsAction.CreateAlertsPermissions", l); //$NON-NLS-1$
		}else if(actionKey.equals(UPDATE_ALERTS_KEY)){
			return Messages.getString("AlertsAction.UpdateAlertsPermissions", l); //$NON-NLS-1$
		}else if(actionKey.equals(DELETE_ALERTS_KEY)){
			return Messages.getString("AlertsAction.DeleteAlertsPermissions", l); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public String[] getActionKeys() {
		return new String[]{VIEW_ALERTS_KEY, CREATE_ALERTS_KEY, UPDATE_ALERTS_KEY, DELETE_ALERTS_KEY
				};
	}

	@Override
	public List<ResourceOption> getResourceOptions(String actionKey, Session s, Locale l) {
		List<ResourceOption> ops = new ArrayList<ResourceOption>();
		ResourceOption ro = new ResourceOption(Messages.getString("CaAction.AllCas", l), null); //$NON-NLS-1$
		ops.add(ro);
		
		List<ConservationAreaInfo> info = s.createCriteria(ConservationAreaInfo.class).list();
		for (ConservationAreaInfo i : info){
			ro = new ResourceOption(i.getLabel(), i.getUuid());
			ops.add(ro);
		}
		return ops;
	}

	@Override
	public String getResourceName(UUID resource, Session s, Locale l) {
		if (resource == null) return Messages.getString("CaAction.AllCas", l); //$NON-NLS-1$
		ConservationAreaInfo info = (ConservationAreaInfo) s.createCriteria(ConservationAreaInfo.class)
				.add(Restrictions.eq("uuid", resource)) //$NON-NLS-1$
				.uniqueResult();
		if (info == null) return resource.toString();
		return info.getLabel();
	}

}
