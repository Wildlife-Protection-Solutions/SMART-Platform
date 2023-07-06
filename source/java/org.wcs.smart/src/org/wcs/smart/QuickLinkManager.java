/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart;

import java.util.List;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.hibernate.Session;
import org.wcs.smart.ca.QuickLink;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

public enum QuickLinkManager {

	INSTANCE;
	
	private int linkCnt = -1;
	
	public List<QuickLink> getQuickLinks(Session session) {
		List<QuickLink> links = session.createQuery("FROM QuickLink WHERE conservationArea = :ca AND (employee is null or employee = :user) ORDER BY employee, uiOrder", QuickLink.class) //$NON-NLS-1$
				.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
				.setParameter("user",  SmartDB.getCurrentEmployee()) //$NON-NLS-1$
				.list();
		linkCnt = links.size();
		return links;
	}
	
	public synchronized int getLinkCnt() {
		if (linkCnt == -1) {
			try(Session session = HibernateManager.openSession()){
				linkCnt = session.createQuery("select count(*) FROM QuickLink WHERE conservationArea = :ca AND (employee is null or employee = :user)", Long.class) //$NON-NLS-1$
					.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
					.setParameter("user",  SmartDB.getCurrentEmployee()) //$NON-NLS-1$
					.uniqueResult().intValue();
			}
		}
		return linkCnt;	
	}
	
	public void reset() {
		this.linkCnt = -1;
		
		//update menu
		MenuManager manager = ((WorkbenchWindow)PlatformUI.getWorkbench().getActiveWorkbenchWindow())
				.getMenuManager();
		String mainMenuId = "org.wcs.smart.menu.quicklinks"; //$NON-NLS-1$
		IContributionItem item  = manager.find(mainMenuId);
		if (item instanceof MenuManager) {
			IContributionItem subitem = ((MenuManager)item).find("org.wcs.smart.menu.quicklinks.dynamic"); //$NON-NLS-1$
			if (subitem != null) subitem.update();
		}
	}
}
