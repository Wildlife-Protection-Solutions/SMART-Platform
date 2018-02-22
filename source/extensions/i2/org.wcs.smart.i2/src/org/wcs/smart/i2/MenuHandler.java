package org.wcs.smart.i2;

import java.util.Locale;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuContribution;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.ILoginHandler;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelConfigurationOption;

/**
 * This is a window handler that is run before the window is displayed.
 * It allows the plugin to update the menu names as required.
 * 
 * @author Emily
 *
 */
public class MenuHandler implements ILoginHandler {

	public MenuHandler() {
	}

	@Override
	public void onLogin() throws Exception {
		
		String langeCode = Locale.getDefault().getLanguage();
		String key = IntelConfigurationOption.MENU_NAME_KEY + "." + langeCode;
		IntelConfigurationOption op = null;
		try (Session session = HibernateManager.openSession()){
			op = QueryFactory.buildQuery(session, IntelConfigurationOption.class, 
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
					new Object[] {"key", key}).uniqueResult();
			if (op == null) {
				op = QueryFactory.buildQuery(session, IntelConfigurationOption.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},
						new Object[] {"key", IntelConfigurationOption.MENU_NAME_KEY}).uniqueResult();
			}
		}
		if (op == null) return;
		
        IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
        MApplication app = ctx.get(MApplication.class);
    	for (MMenuContribution c : app.getMenuContributions()) {
    		for (MMenuElement i : c.getChildren()) {
    			if (i.getElementId().equals("org.wcs.smart.i2.menu.main")) {
    				i.setLabel(op.getValue());
    			}
    		}
    	}
	}

}
