/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.ui.run;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.paws.PawsEvent;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.PawsResultFile;
import org.wcs.smart.paws.model.PawsResultManager;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.ui.HidePartsPartListener;
import org.wcs.smart.query.QueryPlugIn;

/**
 * Editor for displaying PAWS AI analysis results
 * @author Emily
 *
 */
public class RunEditor extends MultiPageEditorPart implements MapPart{

	public static final String ID = "org.wcs.smart.paws.run.editor"; //$NON-NLS-1$

	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());

	private RunSummaryPage summaryPage;
	private RunTableResultsPage resultsPage;
	private RunMapResultsPage mapPage;
	
	private List<EventHandler> handlers = null;
	private IEclipseContext context;
	
	/**
	 * Default constructor
	 */
	public RunEditor() {
		super();	
	}

	@Override
	public void dispose() {
		toolkit.dispose();	
		
		IEventBroker event = context.get(IEventBroker.class);
		if (handlers != null){
			handlers.forEach((h)->event.unsubscribe(h));
		}
		handlers = null;
	}
	
	private void forceCloseEditor(){
		getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(RunEditor.this, false);
	}
	
	private void subscribeToEvent(String eventTopic, EventHandler handler){
		context.get(IEventBroker.class).subscribe(eventTopic, handler);
		handlers.add(handler);
	}
	
	@SuppressWarnings("unchecked")
	private void createEventHandlers() {
		//on delete close editor
		handlers = new ArrayList<>();
		
		subscribeToEvent(PawsEvent.PAWS_RUN_DELETE, (event)->{
			Object data = event.getProperty(IEventBroker.DATA);
			if (data != null){
				Collection<PawsRun> items = (Collection<PawsRun>)data;
				for (PawsRun pc : items){
					if (pc.getUuid().equals(getInputInternal().getUuid())) forceCloseEditor();
				}
			}
		});

		subscribeToEvent(PawsEvent.PAWS_RUN_MODIFY, (event)->{
			Object src = event.getProperty(RunEditor.class.toString());
			if (src != null && src == RunEditor.this) return;
			
			Object data = event.getProperty(IEventBroker.DATA);
			if (data != null){
				Collection<PawsRun> items = (Collection<PawsRun>)data;
				for (PawsRun pc : items){
					if (pc.getUuid().equals(getInputInternal().getUuid())) refresh();
				}
			}
		});
		
		
		subscribeToEvent(SmartPlugIn.E4_DATABASE_CHANGED_EVENT, e->{
			refresh();
		});
	}
	
	private RunEditorInput getInputInternal() {
		return (RunEditorInput)getEditorInput();
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof RunEditorInput)) {
			throw new IllegalArgumentException("Invalid editor input."); //$NON-NLS-1$
		}
		setSite(site);
		setInput(input);
		context = (IEclipseContext) getSite().getService(IEclipseContext.class);
		context.get(EPartService.class).addPartListener(HidePartsPartListener.getInstance());
	}

	
	
	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing
	}

	@Override
	public void setFocus() {
		summaryPage.setFocus();
	}

	@Override
	public void doSaveAs() {
		//not allowed
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	protected void createPages() {
		try{
			summaryPage = new RunSummaryPage(this);
			int index = addPage(summaryPage, getEditorInput());
			super.setPageText(index, Messages.RunEditor_SummaryPage);
		
			resultsPage = new RunTableResultsPage(this);
			index = addPage(resultsPage, getEditorInput());
			setPageImage(index, QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.TABLE_ICON));
			super.setPageText(index, Messages.RunEditor_ResultsPage);
			
			mapPage = new RunMapResultsPage(this);
			index = addPage(mapPage, getEditorInput());
			setPageImage(index, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.MAP_ICON));
			super.setPageText(index, Messages.RunEditor_MapPage);
			

		}catch (Exception ex){
			PawsPlugIn.displayLog(Messages.RunEditor_Error + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			throw new RuntimeException(ex);
		}
		createEventHandlers();
		refresh();
	}
	
	public void refresh() {
		loadSettings.schedule();
	}
	
	public void updateResultsView() {
		if (getResultFile() == null) return;
		
		PawsRun pr = null;
		try(Session s = HibernateManager.openSession()){
			pr = s.get(PawsRun.class, getInputInternal().getUuid());
			if (pr == null) return ;
			
			PawsResultManager results = new PawsResultManager(pr);
			resultsPage.refresh(results);
			mapPage.refresh(results);
		}catch (IOException io) {
			io.printStackTrace();
		}
	}
	
	public void setPartName(String name){
		super.setPartName(name);
	}
	
	public IEclipseContext getContext(){
		return this.context;
	}
	
	public PawsResultFile getResultFile() {
		return summaryPage.getResultsSelection();
	}
	
	private Job loadSettings = new Job(Messages.RunEditor_loadingjobname) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {

			PawsRun pr = null;
			try(Session s = HibernateManager.openSession()){
				pr = s.get(PawsRun.class, getInputInternal().getUuid());
				if (pr == null) return Status.OK_STATUS;
				if (pr.getConfiguration() != null) {
					pr.getConfiguration().getName();
					pr.getConfiguration().getParameters().size();
				}
				pr.getConservationArea().getFileDataStoreLocation();
			}
			
			PawsRun fpr = pr;
			Display.getDefault().syncExec(()->{
				
				summaryPage.init(fpr);
				RunEditor.this.setPartName(fpr.getId());
				try {
					PawsResultManager results = new PawsResultManager(fpr);
					try {
						if (results.getRun().getStatus() == org.wcs.smart.paws.model.PawsRun.Status.COMPLETE) {
							results.createImages(); //should all be created, but if not try again
						}
					}catch (Exception ex) {
						PawsPlugIn.displayLog(ex.getMessage(), ex);
					}
					summaryPage.refresh(results);
					resultsPage.refresh(results);
					mapPage.refresh(results);
				}catch (Exception ex) {
					PawsPlugIn.displayLog(Messages.RunEditor_LoadError + ex.getMessage(), ex);
				}
				
				
			});
			return Status.OK_STATUS;
		}
		
	};

	@Override
	public Map getMap() {
		return mapPage.getMap();
	}

	@Override
	public void openContextMenu() {		
	}

	@Override
	public void setFont(Control textArea) {
	}

	@Override
	public void setSelectionProvider(IMapEditorSelectionProvider selectionProvider) {
		
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return null;
	}
}
