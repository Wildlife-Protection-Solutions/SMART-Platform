/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.patrol;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuContribution;
import org.eclipse.e4.ui.model.application.ui.menu.MMenuElement;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarContribution;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarElement;
import org.eclipse.e4.ui.model.application.ui.menu.MToolItem;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolType;

/**
 * Manage the dynamic menus for patrol/track
 * 
 * @since 8.1.0
 */
public enum PatrolDynamicMenuManager {

	INSTANCE;
	
	private volatile String currentTerm = null;
	
	public String updateTerm() {
		this.currentTerm = null;
		synchronized (this) {
			if (this.currentTerm == null) {
				try(Session session = HibernateManager.openSession()){
					List<PatrolType> types = PatrolHibernateManager.getActivePatrolTypes(SmartDB.getCurrentConservationArea(), session);
					
					if (types.size() == 1 && types.get(0).getKeyId().equalsIgnoreCase(PatrolType.DefaultType.PATROL.getKeyId())) {
						this.currentTerm = Messages.PatrolDynamicMenuManager_PatrolOnlyMenu;
					}else {
						this.currentTerm = Messages.PatrolDynamicMenuManager_PatrolTrackMenu;
					}
				}
			}
			return this.currentTerm;
		}
			
	}
	
	public String getCurrentTerm() {
		if (currentTerm == null) {
			return updateTerm();
		}
		return this.currentTerm;
	}
	
	/**
	 * Updates the patrol menu
	 */
	public void updateMenu() {
		updateTerm();
		
        IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
        MApplication app = ctx.get(MApplication.class);
    	for (MMenuContribution c : app.getMenuContributions()) {
    		for (MMenuElement i : c.getChildren()) {
    			if (i.getElementId().equals("org.wcs.smart.menu.patrol")  ) { //$NON-NLS-1$
    				i.setLabel(this.currentTerm);
    			}else if (i.getElementId().equals("org.wcs.smart.patrol.create")) { //$NON-NLS-1$
    				i.setLabel(MessageFormat.format(Messages.PatrolDynamicMenuManager_New, this.currentTerm));    				
    			}else if (i.getElementId().equals("org.wcs.smart.patrol.view")) { //$NON-NLS-1$
    				i.setLabel(MessageFormat.format(Messages.PatrolDynamicMenuManager_View, this.currentTerm));
    			}else if (i.getElementId().equals("org.wcs.smart.patrol.configuration")) { //$NON-NLS-1$
    				i.setLabel(MessageFormat.format(Messages.PatrolDynamicMenuManager_Config, this.currentTerm));
    			}else if (i.getElementId().equals("org.wcs.smart.patrol.data")) { //$NON-NLS-1$
    				i.setLabel(MessageFormat.format(Messages.PatrolDynamicMenuManager_Data, this.currentTerm));

    			}		
    		}
    	}
    	for (MToolBarContribution c : app.getToolBarContributions()) {
    		for (MToolBarElement e : c.getChildren()) {
    			if (e.getElementId().equals("org.wcs.smart.perspective.fielddata.patrol")  ) { //$NON-NLS-1$
    				if (e instanceof MToolItem mt) {
    					mt.setLabel(currentTerm);
    				}
    			}
    		}
    	}

		
	}
}
