package org.wcs.smart.r.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.r.model.RScript;
import org.wcs.smart.util.UuidUtils;

public class RunScriptMenuContribution extends ContributionItem {

	private static List<RScript> scripts = new ArrayList<>();
	
	public static void addScript(RScript script) {
		if (scripts.contains(script)) scripts.remove(script);
		scripts.add(0, script);
		while(scripts.size() > 5){
			scripts.remove(scripts.size() - 1);
		}
	}
	
	public RunScriptMenuContribution() {
	}

	public RunScriptMenuContribution(String id) {
		super(id);
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
}
