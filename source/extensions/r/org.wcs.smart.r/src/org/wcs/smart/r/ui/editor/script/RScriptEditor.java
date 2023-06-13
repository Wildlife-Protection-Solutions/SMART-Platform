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
package org.wcs.smart.r.ui.editor.script;

import java.util.Collections;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.IPartListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.ui.QueryPerspective;
import org.wcs.smart.r.RPlugIn;
import org.wcs.smart.r.RScriptManager;
import org.wcs.smart.r.engine.QueryConfiguration;
import org.wcs.smart.r.internal.Messages;
import org.wcs.smart.r.model.RQuery;
import org.wcs.smart.r.model.RScript;
import org.wcs.smart.util.E3Utils;

/**
 * R script editor/run
 * 
 * @author Emily
 *
 */
public class RScriptEditor extends MultiPageEditorPart {

	public static final String ID = "org.wcs.smart.r.script.editor"; //$NON-NLS-1$
	
	private RunPage page1;
	private ResultsPage page2;
	private ScriptPage page3;
		
	private RQuery query;
	private boolean isDirty = false;
	
	
	/*
	 * show & hide definition and item areas when vision r script
	 * editor as these are not a part of the r script process
	 */
	private IPartListener partListener = new IPartListener() {
		@Override
		public void partVisible(MPart part) {
			Object lpart = E3Utils.getSourceObject(part);
			if (lpart instanceof RScriptEditor){
				//hide definition and list area 
				MUIElement element = part.getContext().get(EModelService.class).find(QueryPerspective.DEF_FOLDER, part.getContext().get(MApplication.class));
				element.getTags().add(IPresentationEngine.MINIMIZED);
				element = part.getContext().get(EModelService.class).find(QueryPerspective.ITEM_FOLDER, part.getContext().get(MApplication.class));
				element.getTags().add(IPresentationEngine.MINIMIZED);
			}
		}
		
		@Override
		public void partHidden(MPart part) {
			Object lpart = E3Utils.getSourceObject(part);
			if (lpart instanceof RScriptEditor){
				//show definition and list area 
				MUIElement element = part.getContext().get(EModelService.class).find(QueryPerspective.DEF_FOLDER, part.getContext().get(MApplication.class));
				element.getTags().remove(IPresentationEngine.MINIMIZED);
				element = part.getContext().get(EModelService.class).find(QueryPerspective.ITEM_FOLDER, part.getContext().get(MApplication.class));
				element.getTags().remove(IPresentationEngine.MINIMIZED);
			}
		}
		
		@Override
		public void partDeactivated(MPart part) {}
		
		@Override
		public void partBroughtToTop(MPart part) {}
		
		@Override
		public void partActivated(MPart part) {
			
		}
	};
	
	public RScriptEditor() {
		
	}

	@Override
	public void dispose() {
		super.dispose();
		IEclipseContext context = (IEclipseContext) getSite().getService(IEclipseContext.class);
		context.get(EPartService.class).removePartListener(partListener);
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		boolean isNew = query.getUuid() == null;
		
		page1.updateQuery(query);
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				HibernateManager.saveOrMerge(session, query);
				session.getTransaction().commit();
				setDirty(false);
			}catch (Exception ex) {
				session.getTransaction().rollback();
				RPlugIn.displayLog(Messages.RScriptEditor_SaveError + ex.getMessage(),ex);
			}
		}
		String eventType = isNew ? RScriptManager.R_NEW : RScriptManager.R_EDIT;
		((IEclipseContext) getSite().getService(IEclipseContext.class)).get(IEventBroker.class).post(eventType, query);	
	}

	@Override
	public void doSaveAs() {
		RQuery newQuery = new RQuery();
		newQuery.setName(Messages.RScriptEditor_CopyOfName + query.getName());
		newQuery.updateName(SmartDB.getCurrentLanguage(), newQuery.getName());
		newQuery.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), newQuery.getName());
		newQuery.setConservationArea(query.getConservationArea());
		newQuery.setScript(query.getScript());
		
		this.query = newQuery;
		
		page1.updateQuery(query);
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				session.merge(query);
				session.getTransaction().commit();
				setDirty(false);
			}catch (Exception ex) {
				session.getTransaction().rollback();
				RPlugIn.displayLog(Messages.RScriptEditor_SaveError +ex.getMessage(),ex);
			}
		}
		
		((IEclipseContext) getSite().getService(IEclipseContext.class)).get(IEventBroker.class).post(RScriptManager.R_NEW, query);
	}

	
	@Override
	public boolean isDirty() {
		return isDirty;
	}

	public void setDirty(boolean isDirty){
		this.isDirty = isDirty;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}
	
	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}


	@Override
	public void setFocus() {
		page1.setFocus();
	}

	/**
	 * Executes the script
	 */
	void executeScript() {
		page1.executeScript();
	}
	
	/**
	 * Shows the results page
	 */
	void showResults() {
		setActiveEditor(page2);
	}
	
	IRScriptOutputStream createPage2OutputStream() {
		return page2.createPage2OutputStream();
	}
	
	public void updateName(String newName) {
		if (query != null) {
			query.setName(newName);
			query.updateName(SmartDB.getCurrentLanguage(), newName);
		}
		setDirty(true);
		setPartName(newName);
	}
	
	@Override
	protected void createPages() {
		
		try {
			page1 = new RunPage(this);
			int i = addPage(page1, getEditorInput());
			setPageText(i, Messages.RScriptEditor_RunPageName);
			setPageImage(i, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.RUN_ICON));
			
			page2 = new ResultsPage(this);
			i = addPage(page2, getEditorInput());
			setPageText(i, Messages.RScriptEditor_ResultsPageName);
			setPageImage(i, QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.TABLE_ICON));
			
			page3 = new ScriptPage(this);
			i = addPage(page3, getEditorInput());
			setPageText(i, Messages.RScriptEditor_ScriptPageName);
			setPageImage(i, RPlugIn.getDefault().getImageRegistry().get(RPlugIn.ICON_R));
			
		} catch (PartInitException e) {
			RPlugIn.displayLog(e.getMessage(),e);
			return;
		}
		loadScriptJob.schedule();
		
		IEclipseContext context = (IEclipseContext) getSite().getService(IEclipseContext.class);
		context.get(EPartService.class).addPartListener(partListener);
	}

	public RQuery getQuery(){
		return this.query;
	}
	
	private Job loadScriptJob = new Job(Messages.RScriptEditor_loadJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			UUID quuid = ((RScriptEditorInput)getEditorInput()).getRQuery();
			if (quuid != null) {
				RQuery temp = null;
				try(Session session = HibernateManager.openSession()){
					session.beginTransaction();
					try{
						temp = (RQuery) session.getReference(RQuery.class, quuid);
						temp.getName();
						temp.getScript().getName();
						temp.getNames().size();
					}finally {
						session.getTransaction().rollback();
					}
				}
				RScriptEditor.this.query = temp;
			}else {
			
				UUID suuid = ((RScriptEditorInput) getEditorInput()).getRScript();
				RScript temp = null;
				try(Session session = HibernateManager.openSession()){
					session.beginTransaction();
					try{
						temp = (RScript) session.getReference(RScript.class, suuid);
						temp.getName();
					}finally {
						session.getTransaction().rollback();
					}
				}
				
				
				RQuery tempquery = new RQuery();
				tempquery.setName(temp.getName());
				tempquery.updateName(SmartDB.getCurrentLanguage(), tempquery.getName());
				tempquery.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), tempquery.getName());
				tempquery.setScript(temp);
				tempquery.setConservationArea(SmartDB.getCurrentConservationArea());
				tempquery.setConfiguration(QueryConfiguration.toConfigurationString(temp.getDefaultParameters(), Collections.emptyList()));
				RScriptEditor.this.query = tempquery;
			}
			
			Display.getDefault().syncExec(()->{
				page1.update();
				page2.update();
				page3.update();
				
				setPartName(query.getName());
				setDirty(false);
			});
			
			return Status.OK_STATUS;
		}
		
	};

}
