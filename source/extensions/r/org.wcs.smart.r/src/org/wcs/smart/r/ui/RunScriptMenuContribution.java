/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.r.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.r.RPlugIn;
import org.wcs.smart.r.model.RScript;
import org.wcs.smart.util.UuidUtils;

/**
 * R script menu contribution to query menu
 * @author Emily
 *
 */
public class RunScriptMenuContribution extends ContributionItem {

	private static final String SEPARATOR = ","; //$NON-NLS-1$

	private static final String PREF_KEY = "org.wcs.smart.r.ui.menu."; //$NON-NLS-1$
	
	private static List<RScript> scripts = null;
	
	public static synchronized void addScript(RScript script) {
		if (scripts == null) return;
		if (scripts.contains(script)) scripts.remove(script);
		scripts.add(0, script);
		while(scripts.size() > 5){
			scripts.remove(scripts.size() - 1);
		}
		savePreference();
	}
	
	public static synchronized void removeScript(RScript script) {
		if (scripts == null) return;
		scripts.remove(script);
		savePreference();
	}
	public RunScriptMenuContribution() {
		loadPreference();
	}

    @Override
	public boolean isDynamic() {
        return true;
    }
    
    @Override
	public boolean isDirty() {
    	return true;
    }
    
    @Override
	public void fill(Menu menu, int index) {
    	if (scripts == null) return;
    	for (int i = scripts.size()-1; i >= 0; i--) {
    		RScript r = scripts.get(i);
    		MenuItem mi = new MenuItem(menu, SWT.PUSH, index);
    		mi.setText(r.getName());
    		mi.addListener(SWT.Selection, e->{
    			IEclipseContext parentContext = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
    			(new RunRScriptHandler()).execute((IStructuredSelection)parentContext.get(ESelectionService.class).getSelection(), UuidUtils.uuidToString(r.getUuid()), parentContext);
    		});
    	}
    }
    
    private static void savePreference() {
    	StringBuilder sb = new StringBuilder();
    	for (RScript s : scripts) {
    		sb.append(UuidUtils.uuidToString(s.getUuid()));
    		sb.append(SEPARATOR);
    	}
    	String key = PREF_KEY + UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid()); 
    	RPlugIn.getDefault().getPreferenceStore().setValue(key, sb.toString());
    }
    
    
    private static synchronized void loadPreference() {
    	if (scripts != null) return;
    	scripts = new ArrayList<>();
    	String key = PREF_KEY + UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid()); 
    	String items = RPlugIn.getDefault().getPreferenceStore().getString(key);
    	if (items == null || items.isEmpty()) return;
    	
    	String[] bits = items.split(SEPARATOR);
    	try(Session session = HibernateManager.openSession()){
	    	for (String x : bits) {
	    		try {
	    			UUID uuid = UuidUtils.stringToUuid(x);
	    			RScript ss = session.get(RScript.class, uuid);
	    			ss.getName();
	    			scripts.add(ss);
	    		}catch (Exception ex) {
	    		}
	    	}
    	}
    	
    }
}
