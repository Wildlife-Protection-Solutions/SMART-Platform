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
import net.refractions.udig.tool.info.internal.InfoView2;
import net.refractions.udig.ui.UDIGDragDropUtilities;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.wcs.smart.backup.AutoBackupEngine;
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

	/**
	 * A job family jobs can join that must be finished before the hibernate
	 * session factory is closed;
	 */
	public static final String SHUTDOWN_JOB_FAMILY = "org.wcs.smart.shut.job.family"; //$NON-NLS-1$
	
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
				
				AutoBackupEngine.autoBackup(workbench.getActiveWorkbenchWindow().getShell());
				
				return true;
				
			}
			return false;
		}
		
		@Override
		public void postShutdown(IWorkbench workbench) {
			try {
				Job.getJobManager().join(SHUTDOWN_JOB_FAMILY, null);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
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

        configurer.setShowCoolBar(true);
        configurer.setShowStatusLine(false);	//udig adds a bunch of tools to the status line which we don't want
        configurer.setShowProgressIndicator(true);
        
        // setup perspective tracker
        IPartService service = (IPartService) configurer.getWindow().getService(IPartService.class);
        service.addPartListener(perspectiveTracker);
        configurer.getWindow().addPerspectiveListener(perspectiveListener);

        // setup drag and drop support 
        UDIGDragDropUtilities.registerUDigDND(configurer);
    }
    
    @Override
    public void postWindowOpen() {
    	getWindowConfigurer().getWorkbenchConfigurer().getWorkbench().addWorkbenchListener(shutdownListener);
        //assign title to window
        getWindowConfigurer().getWindow().getShell().setText("SMART : " + SmartDB.getCurrentConservationArea().getNameLabel()); //$NON-NLS-1$
        
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
				if (partRef.getPart(false) instanceof MapPart){
					IViewPart infoView = partRef.getPage().findView(InfoView2.VIEW_ID);
					if (infoView != null){
						partRef.getPage().hideView(infoView);
					}
				}
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
    	if (view != null){
    		LoadDefaultLayersJob job = new LoadDefaultLayersJob(view.getMap(), true);
    		job.schedule();
    	}
    	
    	
    }
    
    public void postWindowCreate() {
    	super.postWindowCreate();
    	
    	// remove all non-smart preference pages
    	PreferenceManager pm = PlatformUI.getWorkbench().getPreferenceManager();
    	IPreferenceNode[] nodes = pm.getRootSubNodes();
    	for (IPreferenceNode node: nodes){
    		if (!node.getId().contains("smart")){ //$NON-NLS-1$
    			pm.remove(node.getId());
    		}
    	}    	
    }    

}
