/*
 * Copyright (C) 2016 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.i2;

import java.text.MessageFormat;
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
import org.wcs.smart.i2.internal.Messages;
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
		String key = IntelConfigurationOption.MENU_NAME_KEY + "." + langeCode; //$NON-NLS-1$
		IntelConfigurationOption op = null;
		try (Session session = HibernateManager.openSession()){
			op = QueryFactory.buildQuery(session, IntelConfigurationOption.class, 
					new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
					new Object[] {"key", key}).uniqueResult(); //$NON-NLS-1$
			if (op == null) {
				op = QueryFactory.buildQuery(session, IntelConfigurationOption.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
						new Object[] {"key", IntelConfigurationOption.MENU_NAME_KEY}).uniqueResult(); //$NON-NLS-1$
			}
		}
		if (op == null) return;
		
        IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
        MApplication app = ctx.get(MApplication.class);
    	for (MMenuContribution c : app.getMenuContributions()) {
    		for (MMenuElement i : c.getChildren()) {
    			if (i.getElementId().equals("org.wcs.smart.i2.menu.main")) { //$NON-NLS-1$
    				i.setLabel(op.getValue());
    			}else if (i.getElementId().equals("org.wcs.smart.menu.query.newquery.i2queries")) { //$NON-NLS-1$
    				i.setLabel(MessageFormat.format(Messages.MenuHandler_QueryMenuPostfix, op.getValue()));
    			}else if (i.getElementId().equals("org.wcs.smart.menu.query.createquery.i2queries")) { //$NON-NLS-1$
    				i.setLabel(MessageFormat.format(Messages.MenuHandler_QueryMenuPostfix, op.getValue()));
    			}		
    		}
    	}
	}

}
