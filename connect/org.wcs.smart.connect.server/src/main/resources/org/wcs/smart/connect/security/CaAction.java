package org.wcs.smart.connect.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.ConservationAreaInfo;

public class CaAction implements ISmartConnectAction{

	public static final String VIEWCA_KEY = "viewca"; //$NON-NLS-1$
	public static final String DELETECA_KEY = "deleteca"; //$NON-NLS-1$
	public static final String UPDATECA_KEY = "updateca"; //$NON-NLS-1$
	
	@Override
	public String getActionName(String actionKey, Locale l) {
		if (actionKey.equals(VIEWCA_KEY)){
			return Messages.getString("CaAction.ViewCaPermission", l); //$NON-NLS-1$
		}else if (actionKey.equals(DELETECA_KEY)){
			return Messages.getString("CaAction.DeleteCaPermission", l); //$NON-NLS-1$
		}else if (actionKey.equals(UPDATECA_KEY)){
			return Messages.getString("CaAction.UpdateCaPermission", l); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public String[] getActionKeys() {
		return new String[]{VIEWCA_KEY, DELETECA_KEY, UPDATECA_KEY};
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
		if (info == null) return null;
		return info.getLabel();
	}

}
