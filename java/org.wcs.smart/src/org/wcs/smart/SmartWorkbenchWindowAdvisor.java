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

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimElement;
import org.eclipse.e4.ui.model.application.ui.menu.MToolControl;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.preference.IPreferenceNode;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.progress.WorkbenchJob;
import org.hibernate.Session;
import org.locationtech.udig.project.ui.internal.LayersView;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.tool.info.internal.InfoView2;
import org.locationtech.udig.ui.UDIGDragDropUtilities;
import org.wcs.smart.backup.AutoBackupEngine;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

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
			try {
				HibernateManager.endSessionFactory(false, true);
			} catch (Exception e) {
			}
			
		}
	};
	
    public SmartWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
        super(configurer);        
    }

    public void dispose(){
    	super.dispose();
    	
    	PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(partListener);
    	getWindowConfigurer().getWorkbenchConfigurer().getWorkbench().removeWorkbenchListener(shutdownListener);
    	super.getWindowConfigurer().getWindow().removePerspectiveListener(perspectiveListener);
    	
    	perspectiveTracker = null;
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
        IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
        perspectiveTracker = ContextInjectionFactory.make(PerspectiveEditorTracker.class, ctx);

        perspectiveListener = new PerspectiveEditorListener(perspectiveTracker, ctx.get(EPartService.class));
        configurer.getWindow().addPerspectiveListener(perspectiveListener);
        
        // setup drag and drop support 
        UDIGDragDropUtilities.registerUDigDND(configurer);
    }
    
    @Override
    public void postWindowOpen() {
    	getWindowConfigurer().getWorkbenchConfigurer().getWorkbench().addWorkbenchListener(shutdownListener);
        //assign title to window
    	updateWorkbenchWindowTitle();

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
					if (view != null ){
						if (mp == null){
							view.setCurrentMap(null);
						}else{
							view.setCurrentMap(mp.getMap());
						}
					}
				}
				
			}

			@Override
			public void partInputChanged(IWorkbenchPartReference partRef) {				
			}
		};
    	PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(partListener);

    	
		// reconfigure the location of the user name control
		IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench()
				.getService(IEclipseContext.class);
		EModelService modelService = ctx.get(EModelService.class);
		MTrimBar element = (MTrimBar) modelService.find("org.eclipse.ui.main.toolbar", ctx.get(MApplication.class)); //$NON-NLS-1$
		int src = -1;
		int trg = -1;
		for (int i = 0; i < element.getChildren().size(); i++) {
			if (element.getChildren().get(i).getElementId()
					.equals("org.wcs.smart.userNameInfo")) { //$NON-NLS-1$
				src = i;
			} else if (element.getChildren().get(i).getElementId()
					.equals("PerspectiveSpacer")) { //$NON-NLS-1$
				trg = i;
			}
		}
		if (src != -1 && trg != -1 && trg > src) {
			MTrimElement e = element.getChildren().get(src);
			element.getChildren().remove(src);
			element.getChildren().add(trg, e);
		}
		
		//this gets hidden by some visibility changes events that I don't have control over;
		//so instead we ensure it is visible here.
		MTrimBar statusBar = (MTrimBar) modelService.find("org.eclipse.ui.trim.status", ctx.get(MApplication.class)); //$NON-NLS-1$
		statusBar.setVisible(true);
		
		//add a spacer so we can force items to be on the right
		MToolControl tc = modelService.createModelElement(MToolControl.class);
		tc.setElementId("org.wcs.smart.trim.status.spacer"); //$NON-NLS-1$
		statusBar.getChildren().add(tc);
		tc.getTags().add("stretch"); //$NON-NLS-1$
		
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

    public static void updateWorkbenchWindowTitle(){
    	if (SmartDB.getCurrentConservationArea() == null) return;
    	WorkbenchJob job = new WorkbenchJob("refresh title") { //$NON-NLS-1$
			
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				s.beginTransaction();
				try{
					ConservationArea ca = (ConservationArea) s.get(ConservationArea.class, SmartDB.getCurrentConservationArea().getUuid());
					
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell().setText(
							MessageFormat.format(Messages.SmartWorkbenchWindowAdvisor_WindowTitle, ca.getNameLabel())); 
				}catch (Exception ex){
					SmartPlugIn.log(ex.getMessage(), ex);
				}finally{
					s.close();
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
    }
}
