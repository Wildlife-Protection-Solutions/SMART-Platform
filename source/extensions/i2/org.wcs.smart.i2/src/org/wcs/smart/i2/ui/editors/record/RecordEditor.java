/*   
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.editors.record;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.AttachmentManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelObservation;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.ui.IntelDataAnalysisPerspective;
import org.wcs.smart.i2.ui.IntelDataAssessmentPerspective;

import com.vividsolutions.jts.geom.Polygon;

public class RecordEditor extends MultiPageEditorPart implements MapPart, IAdaptable{
	
	public static final String ID = "org.wcs.smart.i2.editor.record"; //$NON-NLS-1$

	private RecordEditorInput input;

	private boolean isEditMode = false;
	private boolean isDirty = false;

	private IEclipseContext parentContext;
	
	private RecordSummaryPage summaryPage;
	private RecordMapPage mapPage;
	
	private IntelRecord record;
	
	private Job loadRecordJob = new Job("load intelligence record"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			record = null;
			IntelRecord temp = null;
			UUID uuid = null;
			if (input.getRecord() == null){
				uuid = input.getUuid();
			}else{
				uuid = input.getRecord().getUuid();
				temp = input.getRecord();
			}
			if (uuid != null){
				record = null;
				Session s = HibernateManager.openSession();
				try{
					temp = (IntelRecord) s.get(IntelRecord.class, uuid);
					temp.getCreatedBy().getFamilyName();
					temp.getLastModifiedBy().getFamilyName();
					if (temp.getAttachments().size() > 0){
						for (IntelRecordAttachment a : temp.getAttachments()){
							try{
								a.getAttachment().computeFileLocation(s);
							}catch (Exception ex){
								//TODO: 
							}
						}
					}
					if (temp.getEntities() != null){
						for (IntelEntityRecord rr : temp.getEntities()){
							rr.getEntity().getIdAttributeAsText();
							rr.getEntity().getEntityType().getName();
							if (rr.getEntity().getPrimaryAttachment()!= null){
								try{
									rr.getEntity().getPrimaryAttachment().computeFileLocation(s);
								}catch (Exception ex){
									//TODO:
								}
							}
							if (rr.getEntity().getEntityAttachments() != null){
								for (IntelEntityAttachment a : rr.getEntity().getEntityAttachments()){
									try{
										a.getAttachment().computeFileLocation(s);
									}catch (Exception ex){
										//TODO:
									}
								}
							}
						}
					}
					if (temp.getLocations() != null){
						for (IntelLocation loc : temp.getLocations()){
							loc.getId();
						}
					}
				}finally{
					s.close();
				}
			}
			record = temp;
			
			
			Display.getDefault().syncExec(new Runnable(){

				@Override
				public void run() {
					initPage();
				}
				
			});
			return org.eclipse.core.runtime.Status.OK_STATUS;
		}
		
	};
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		Set<IntelEntity> modifiedEntities = new HashSet<IntelEntity>();
		boolean isnew = record.getUuid() == null;
		
		Session s = HibernateManager.openSession(new AttachmentInterceptor());
		try{
			s.beginTransaction();
			
			if (record.getAttachments() != null){
				for (IntelRecordAttachment a : record.getAttachments()){
					s.saveOrUpdate(a.getAttachment());
				}
			}
			
			for (IntelEntityAttachment entityAttachments : summaryPage.getNewAttachments()){
				s.save(entityAttachments);
				entityAttachments.getEntity().getEntityAttachments().add(entityAttachments);
				modifiedEntities.add(entityAttachments.getEntity());
			}
			
			for (IntelEntityRecord r : summaryPage.getDeleteEntityLinks()){
				modifiedEntities.add(r.getEntity());
				s.delete(r);
			}
			
			for (IntelEntityRecord r : summaryPage.getNewEntityLinks()){
				modifiedEntities.add(r.getEntity());
			}
			
			s.flush();
			s.saveOrUpdate(record);
			s.flush();
			
			for (IntelRecordAttachment ea : summaryPage.getDeleteAttachments()){
				if (ea.getAttachment().getUuid() != null){
					if (AttachmentManager.INSTANCE.canDelete(ea.getAttachment(), s)){
						s.delete(ea);
						s.delete(ea.getAttachment());
					}
				}
			}
			
			s.getTransaction().commit();
			
			summaryPage.clearLists();
		}catch (Exception ex){
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog("Unable to save changes to intelligence record. " + ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		
		//fire events; one for record and one for each modified entity
		if (isnew){
			parentContext.get(IEventBroker.class).post(IntelEvents.RECORD_NEW, record);
		}else{
			parentContext.get(IEventBroker.class).post(IntelEvents.RECORD_MODIFIED, record);
		}
		for (IntelEntity modified : modifiedEntities){
			parentContext.get(IEventBroker.class).post(IntelEvents.ENTITY_MODIFIED, modified);
		}
		
		setDirty(false);
		summaryPage.doAfterSave();
		super.setPartName(record.getTitle());
	}
	
	public void refresh(){
		loadRecordJob.schedule();
	}
	
	public RecordSummaryPage getSummaryPage(){
		return this.summaryPage;
	}
	
	@Override
	protected void createPages() {
		
		
		showBusy(true);
		try {
			parentContext = (IEclipseContext) getSite().getService(IEclipseContext.class);
			
			//configure tags so editors show in both perspectives
			MPart part = parentContext.get(MPart.class); 
			if (!part.getTags().contains(IntelDataAssessmentPerspective.ID)) part.getTags().add(IntelDataAssessmentPerspective.ID);
			if (!part.getTags().contains(IntelDataAnalysisPerspective.ID)) part.getTags().add(IntelDataAnalysisPerspective.ID);
			
			//on delete close editor
			parentContext.get(IEventBroker.class).subscribe(IntelEvents.RECORD_DELETE, new EventHandler() {
				@Override
				public void handleEvent(org.osgi.service.event.Event event) {
					Object data = event.getProperty(IEventBroker.DATA);
					if (data != null && data.equals(record)){
						getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(RecordEditor.this, false);
					}
				}
			});

			  
			getSite().getWorkbenchWindow().addPerspectiveListener(new PerspectiveAdapter() {
				@Override
				public void perspectiveActivated(IWorkbenchPage page,
						IPerspectiveDescriptor perspective) {
					if (isDirty && perspective.getId().equals(IntelDataAnalysisPerspective.ID)){
						//save and be done with it
						setEditMode(false);
					}else if (perspective.getId().equals(IntelDataAssessmentPerspective.ID)){
						setEditMode(true);
					}
				}
			});
			
			summaryPage = new RecordSummaryPage(this);
			int i = addPage(summaryPage, getEditorInput());
			setPageText(i, "Summary");
			
			mapPage = new RecordMapPage(this);
			i = addPage(mapPage, getEditorInput());
			setPageText(i, "Map");
		} catch (final Throwable t) {
			Intelligence2PlugIn.log(t.getMessage(), t);
			//TODO:
		}finally{
			showBusy(false);
		}
		
		getSite().getWorkbenchWindow().addPerspectiveListener(new PerspectiveAdapter() {
			@Override
			public void perspectiveActivated(IWorkbenchPage page,
					IPerspectiveDescriptor perspective) {
				if (isDirty && perspective.getId().equals(IntelDataAnalysisPerspective.ID)){
					//save and be done with it
					setEditMode(false);
				}else if (perspective.getId().equals(IntelDataAssessmentPerspective.ID)){
					setEditMode(true);
				}
			}
		});
		
		refresh();
	}
	


	@Override
	public void doSaveAs() {		
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setSite(site);
		super.setInput(input);
		
		this.input = (RecordEditorInput)input;
		super.setPartName(input.getName());
	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}

	public void setDirty(boolean isDirty){
		boolean fire = isDirty != this.isDirty;
		this.isDirty = isDirty;
		if (fire) firePropertyChange(IEditorPart.PROP_DIRTY);
	}
	
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	
	public IntelRecord getRecord(){
		return this.record;
	}
	
	public void addNewLocation(Polygon p){
		if (record.getLocations() == null) record.setLocations(new ArrayList<IntelLocation>());
		
		IntelLocation newLocation = new IntelLocation();
		newLocation.setComment(null);
		newLocation.setConservationArea(SmartDB.getCurrentConservationArea());
		newLocation.setDateTime(record.getDateCreated());
		newLocation.setGeometry(p);
		newLocation.setId(MessageFormat.format("Location {0}", record.getLocations().size()+1));
		newLocation.setRecord(record);
		newLocation.setObservations(new ArrayList<IntelObservation>());
		
		record.getLocations().add(newLocation);
		
		summaryPage.getLocationPanel().refreshTable();
		mapPage.refresh();
		setDirty(true);
	}
	
	
	public void linkEntity(IntelEntity entity){
		Job link = new Job("linking entity"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IntelEntity tolink = null;
				Session s = HibernateManager.openSession();
				try{
					tolink = (IntelEntity) s.get(IntelEntity.class, entity.getUuid());
					if (tolink != null){
						tolink.getIdAttributeAsText();
						if (tolink.getPrimaryAttachment() != null){
							try{
								tolink.getPrimaryAttachment().computeFileLocation(s);
							}catch (Exception ex){
								//TODO:
							}
						}
						if (tolink.getEntityAttachments() != null){
							for (IntelEntityAttachment a : tolink.getEntityAttachments()){
								try{
									a.getAttachment().computeFileLocation(s);	
								}catch (Exception ex){
									//TODO;
								}
								
							}
						}
					}
				}finally{
					s.close();
				}
				if (tolink != null){
					final IntelEntity link = tolink;
					Display.getDefault().syncExec(() -> {summaryPage.linkEntity(link);});
				}
				return org.eclipse.core.runtime.Status.OK_STATUS;
			}	
		};
		link.schedule();
	}
	
	public IEclipseContext getContext(){
		return parentContext;
	}
	
	
	public void setEditMode(boolean editMode){
		if (editMode == isEditMode) return;
		if (isEditMode && !editMode && isDirty){
			doSave(new NullProgressMonitor());
		}
		this.isEditMode = editMode;
		
		summaryPage.setEditMode(editMode);
		mapPage.setEditMode(editMode);
		
		if (record != null){
			initPage();
		}		
	}
	
	private void initPage(){
		summaryPage.initPage();
		mapPage.initPage();
		
		
		super.setPartName(record.getTitle());
	}
	
	@Override
	public void setFocus() {
	}
	

	public boolean getEditMode() {
		return this.isEditMode;
	}


	@Override
	public Map getMap() {
		return mapPage.getMap();
	}


	@Override
	public void openContextMenu() {
		mapPage.openContextMenu();
	}


	@Override
	public void setFont(Control textArea) {
		mapPage.setFont(textArea);
	}


	@Override
	public void setSelectionProvider(
			IMapEditorSelectionProvider selectionProvider) {
		mapPage.setSelectionProvider(selectionProvider);
	}


	@Override
	public IStatusLineManager getStatusLineManager() {
		return mapPage.getStatusLineManager();
	}

}
