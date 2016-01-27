package org.wcs.smart.connect.dataqueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.security.ISmartConnectAction;
import org.wcs.smart.connect.security.ResourceOption;

public class DataQueueAction implements ISmartConnectAction {

	public static final String VIEW_KEY = "viewdataqueue"; //$NON-NLS-1$
	public static final String DELETE_KEY = "deletedataqueue"; //$NON-NLS-1$
	public static final String PROCESS_KEY = "processdataqueue"; //$NON-NLS-1$
	public static final String ADD_KEY = "adddataqueue"; //$NON-NLS-1$
	
	@Override
	public String getActionName(String actionKey, Locale l) {
		if (actionKey.equals(VIEW_KEY)){
			return "View Data Queue Items";
		}else if (actionKey.equals(DELETE_KEY)){
			return "Delete Data Queue Items";
		}else if (actionKey.equals(PROCESS_KEY)){
			return "Process (and Update) Data Queue Items";
		}else if (actionKey.equals(ADD_KEY)){
			return "Add/Upload Files To Data Queue";
		}
		return null;
	}

	@Override
	public String[] getActionKeys() {
		return new String[]{VIEW_KEY, DELETE_KEY, PROCESS_KEY, ADD_KEY};
	}

	@Override
	public List<ResourceOption> getResourceOptions(String actionKey, Session s,
			Locale l) {
		
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
