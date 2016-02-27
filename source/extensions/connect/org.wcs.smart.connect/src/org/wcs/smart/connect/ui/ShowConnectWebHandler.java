package org.wcs.smart.connect.ui;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.BrowserView;

public class ShowConnectWebHandler {

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell,
			EPartService ePartService) {
		MPart part = ePartService.showPart(BrowserView.ID, PartState.VISIBLE);
		
		Session s = HibernateManager.openSession();
		try{
			ConnectServer cs = (ConnectServer) s.createCriteria(ConnectServer.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.uniqueResult();
			if (cs != null){
				part.getContext().set(BrowserView.DEFAULT_URL, cs.getServerUrl() + "/connect");
			}
						
		}finally{
			s.close();
		}
		
		
	}
	
	public static class ShowConnectWebHandlerWrapper extends DIHandler<ShowConnectWebHandler>{
		public ShowConnectWebHandlerWrapper() {
			super(ShowConnectWebHandler.class);
		}
		
	}
}

