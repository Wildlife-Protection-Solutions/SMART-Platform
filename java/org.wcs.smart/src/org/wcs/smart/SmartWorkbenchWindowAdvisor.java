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

import net.refractions.udig.project.ui.internal.LayersView;
import net.refractions.udig.project.ui.internal.MapPart;

import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapView;

/**
 * Smart Workbench Window Advisor.
 * @author Emily
 * @since 1.0.0
 */
public class SmartWorkbenchWindowAdvisor extends WorkbenchWindowAdvisor {

	private IPartListener2 partListener = null;

	private PerspectiveEditorTracker perspectiveTracker = null;
	private PerspectiveEditorListener perspectiveListener = null;
	
	private IWorkbenchListener shutdownListener = new IWorkbenchListener() {
		@Override
		public boolean preShutdown(IWorkbench workbench, boolean forced) {
			if (workbench.saveAllEditors(true)){
				if (workbench.getActiveWorkbenchWindow() != null && workbench.getActiveWorkbenchWindow().getActivePage() != null){
					workbench.getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
				}
				return true;
			}
			return false;
		}
		
		@Override
		public void postShutdown(IWorkbench workbench) {
			//close database connection
			HibernateManager.endSessionFactory(false);
			
		}
	};
	
    public SmartWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);
        
        perspectiveTracker = new PerspectiveEditorTracker();
        perspectiveListener = new PerspectiveEditorListener(perspectiveTracker);
    }

    public void dispose(){
    	super.dispose();
    	PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(partListener);
    	PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(perspectiveTracker);
    	//getWindowConfigurer().getWorkbenchConfigurer().getWorkbench().removeWorkbenchListener(shutdownListener);
    	super.getWindowConfigurer().getWindow().removePerspectiveListener(perspectiveListener);
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
        
        /* setup perspective tracker */
        IPartService service = (IPartService) configurer.getWindow().getService(IPartService.class);
        service.addPartListener(perspectiveTracker);
        configurer.getWindow().addPerspectiveListener(perspectiveListener);
    }
    
    @Override
    public void postWindowOpen() {
    	getWindowConfigurer().getWorkbenchConfigurer().getWorkbench().addWorkbenchListener(shutdownListener);
        //assign title to window
        getWindowConfigurer().getWindow().getShell().setText("SMART : " + SmartDB.getCurrentConservationArea().getId() + " - " + SmartDB.getCurrentConservationArea().getName());
        
        /* -- setup part listener for layer view legend */
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
//				if (partRef.getPart(false) instanceof MapPart){
//					MapPart mp = (MapPart)partRef.getPart(false); 
//					if (PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() == null){
//						return;
//					}
//					LayersView view = (LayersView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(LayersView.ID);
//					if (view.getCurrentMap() == mp.getMap()){
//						view.setCurrentMap(null);
//					}
//				}
			}

			@Override
			public void partVisible(IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) instanceof MapPart){
					MapPart mp = (MapPart)partRef.getPart(false); 
					LayersView view = (LayersView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(LayersView.ID);
					//if (view.getCurrentMap() == null){
					if (view != null){
						view.setCurrentMap(mp.getMap());
					}
					//}
				}
				
			}

			@Override
			public void partInputChanged(IWorkbenchPartReference partRef) {				
			}
		};
    	PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(partListener);
    	
        /* --- Add initial layers to map --- */
    	final MapView view = (MapView)PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView(MapView.ID);
    	LoadDefaultLayersJob job = new LoadDefaultLayersJob(view.getMap(), true);
    	job.schedule();
    	
    	/* --- Image Descriptors for PlugIn --- */
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
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
				SmartPlugIn.PLUGIN_ID, "images/icons/obj16/station.png"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(SmartPlugIn.STATION_ICON, descriptor);
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
