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
package org.wcs.smart;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.project.internal.command.navigation.ZoomExtentCommand;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.project.ui.internal.LayersView;
import net.refractions.udig.project.ui.internal.MapPart;

import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.geotools.gml3.smil.SMIL20;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.udig.catalog.smart.SmartService;
import org.wcs.smart.udig.catalog.smart.SmartServiceExtension;
import org.wcs.smart.ui.map.MapView;

public class SmartWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	private IPartListener2 partListener = null;
	
    public SmartWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);
    }

    public void dispose(){
    	super.dispose();
    	PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(partListener);
    }
    public ActionBarAdvisor createActionBarAdvisor(IActionBarConfigurer configurer) {
    	return new ActionBarAdvisor(configurer);
    }
    
    public void preWindowOpen() {
        IWorkbenchWindowConfigurer configurer = getWindowConfigurer();
        configurer.setInitialSize(new Point(1200, 800));
        configurer.setShowCoolBar(true);
        configurer.setShowStatusLine(false);
        configurer.setShowProgressIndicator(true);
    }
    
    @Override
    public void postWindowOpen() {
    	
        //assign title to window
        getWindowConfigurer().getWindow().getShell().setText("SMART : " + SmartDB.getCurrentConservationArea().getId() + " - " + SmartDB.getCurrentConservationArea().getName());
        
    	partListener = new IPartListener2() {
    		@Override
			public void partActivated(IWorkbenchPartReference partRef) {
			}

			@Override
			public void partBroughtToTop(IWorkbenchPartReference partRef) {
			}

			@Override
			public void partClosed(IWorkbenchPartReference partRef) {
			}

			@Override
			public void partDeactivated(IWorkbenchPartReference partRef) {
			}

			@Override
			public void partOpened(IWorkbenchPartReference partRef) {
			}

			@Override
			public void partHidden(IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) instanceof MapPart){
					MapPart mp = (MapPart)partRef.getPart(false); 
					if (PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() == null){
						return;
					}
					LayersView view = (LayersView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(LayersView.ID);
					if (view.getCurrentMap() == mp.getMap()){
						view.setCurrentMap(null);
					}
				}
			}

			@Override
			public void partVisible(IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) instanceof MapPart){
					MapPart mp = (MapPart)partRef.getPart(false); 
					LayersView view = (LayersView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(LayersView.ID);
					//if (view.getCurrentMap() == null){
						view.setCurrentMap(mp.getMap());
					//}
				}
				
			}

			@Override
			public void partInputChanged(IWorkbenchPartReference partRef) {
				// TODO Auto-generated method stub
				
			}
		};
    	PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(partListener);
    	
        //find view
    	Display.getCurrent().asyncExec(new Runnable(){

			@Override
			public void run() {
				MapView view = (MapView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(MapView.ID);
		    	if (view != null){
		    		HashMap<String, Serializable> params = new HashMap<String, Serializable>();
		    		params.put(SmartServiceExtension.CA_UUID_KEY, SmartDB.getCurrentConservationArea().getUuid());
		    		SmartService ss = new SmartService(params);
		    		CatalogPlugin.getDefault().getLocalCatalog().add(ss);
		    		try {
		    			List<IGeoResource> layers = (List<IGeoResource>) ss.resources(null);
						ApplicationGIS.addLayersToMap(view.getMap(),layers,0);
						view.getMap().sendCommandASync(new ZoomExtentCommand());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		    	}
				
			}});
    	
    	//TODO put this in SmartPlugIn class
		ImageDescriptor descriptor = AbstractUIPlugin
				.imageDescriptorFromPlugin(SmartPlugIn.PLUGIN_ID,
						"images/icons/obj16/user_orange.png"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(SmartPlugIn.SMART_EMPLOYEE_ICON, descriptor);
		}
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
				SmartPlugIn.PLUGIN_ID, "images/icons/obj16/user_green.png"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(SmartPlugIn.EMPLOYEE_ICON, descriptor);
		}
    }
    
    public void postWindowCreate() {
    	super.postWindowCreate();
    	
    	// remove all non-smart preference pages
    	PreferenceManager pm = PlatformUI.getWorkbench().getPreferenceManager();
    	IPreferenceNode[] nodes = pm.getRootSubNodes();
    	for (IPreferenceNode node: nodes){
    		if (!node.getId().contains("smart")){
    			pm.remove(node.getId());
    		}
    	}    	
    }
    
    
    
//    /**
//     * Look up configuration object, using UDIGWorkbenchConfiguration as a default.
//     * @return WorkbenchConfiguration from preferences, or UDIGWorkbenchConfiguration if not found
//     */
//    private WorkbenchConfiguration lookupConfiguration() {
//
//        Class<WorkbenchConfiguration> interfaceClass = WorkbenchConfiguration.class;
//        String prefConstant = PreferenceConstants.P_WORKBENCH_CONFIGURATION;
//        String xpid = WorkbenchConfiguration.XPID;
//        String idField = WorkbenchConfiguration.ATTR_ID;
//        String classField = WorkbenchConfiguration.ATTR_CLASS;
//
//        WorkbenchConfiguration config = (WorkbenchConfiguration) UiPlugin
//                .lookupConfigurationObject(interfaceClass, UiPlugin.getDefault()
//                        .getPreferenceStore(), UiPlugin.ID, prefConstant, xpid, idField, classField);
//        if (config == null) {
//            return new SmartWorkbenchConfigurator();
//        }
//        return config;
//    }
}
