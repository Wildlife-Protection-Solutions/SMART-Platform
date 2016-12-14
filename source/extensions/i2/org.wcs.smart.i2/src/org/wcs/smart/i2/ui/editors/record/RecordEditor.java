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
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.hibernate.criterion.Restrictions;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.AttachmentManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.model.IntelEntityLocation;
import org.wcs.smart.i2.model.IntelEntityRecord;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelObservation;
import org.wcs.smart.i2.model.IntelObservationAttribute;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.ui.IntelDataAnalysisPerspective;
import org.wcs.smart.i2.ui.IntelDataAssessmentPerspective;




import com.drew.lang.annotations.NotNull;
import com.vividsolutions.jts.geom.Geometry;

public class RecordEditor extends MultiPageEditorPart implements MapPart, IAdaptable{
	
	public static final String ID = "org.wcs.smart.i2.editor.record"; //$NON-NLS-1$

	private RecordEditorInput input;

	private boolean isEditMode = false;
	private boolean isDirty = false;

	private IEclipseContext parentContext;
	private List<EventHandler> handlers = null;
	
	private RecordSummaryPage summaryPage;
	private RecordMapPage mapPage;
	
	private IntelRecord record;
	
	private List<IntelEntityLocation> currentEntityLocationLinks = new ArrayList<IntelEntityLocation>();
	private List<IntelEntityLocation> newEntityLocationLinks = new ArrayList<IntelEntityLocation>();
	private List<IntelEntityLocation> deleteEntityLocationLinks = new ArrayList<IntelEntityLocation>();
	
	private List<IntelLocation> locationsToDelete = new ArrayList<IntelLocation>();
	
	private Job loadRecordJob = new Job("load intelligence record"){

		@SuppressWarnings("unchecked")
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
								Intelligence2PlugIn.log(ex.getMessage(), ex);
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
									Intelligence2PlugIn.log(ex.getMessage(), ex);
								}
							}
							if (rr.getEntity().getEntityAttachments() != null){
								for (IntelEntityAttachment a : rr.getEntity().getEntityAttachments()){
									try{
										a.getAttachment().computeFileLocation(s);
									}catch (Exception ex){
										Intelligence2PlugIn.log(ex.getMessage(), ex);
									}
								}
							}
						}
					}
					if (temp.getLocations() != null){
						for (IntelLocation loc : temp.getLocations()){
							loc.getId();
							if (loc.getObservations() != null){
								for (IntelObservation oo : loc.getObservations()){
									oo.getCategory().getFullCategoryName();
									if (oo.getObservationAttributes() != null){
										for (IntelObservationAttribute a : oo.getObservationAttributes()){
											a.getAttribute().getName();
											if (a.getAttributeListItem() != null) a.getAttributeListItem().getName();
											if (a.getAttributeTreeNode() != null) a.getAttributeTreeNode().getName();
										}
									}
								}
							}
						}
					}
					if (!temp.getLocations().isEmpty()){
						currentEntityLocationLinks = s.createCriteria(IntelEntityLocation.class)
							.add(Restrictions.in("id.location", temp.getLocations()))
							.list();
						for (IntelEntityLocation ll : currentEntityLocationLinks){
							ll.getEntity().getIdAttributeAsText();
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
	public void dispose(){
		IEventBroker event = parentContext.get(IEventBroker.class);
		if (handlers != null){
			handlers.forEach((h)->event.unsubscribe(h));
		}
		super.dispose();
	}
	
	@SuppressWarnings("unchecked")
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

			
			
			for (IntelEntityRecord r : summaryPage.getDeleteEntityLinks()){
				if (r.getRecord().getUuid() != null){
					modifiedEntities.add(r.getEntity());
					s.delete(r);
				}
			}
			
			for (IntelEntityRecord r : summaryPage.getNewEntityLinks()){
				modifiedEntities.add(r.getEntity());
			}
			for (IntelLocation location : locationsToDelete){
				if (location.getUuid() == null) continue;
				//find any entity location links and remove these
				List<IntelEntityLocation> todelete = s.createCriteria(IntelEntityLocation.class)
					.add(Restrictions.eq("id.location", location))
					.list();
				for (IntelEntityLocation l : todelete){
					l.setLocation((IntelLocation) s.merge(l.getLocation()));
					modifiedEntities.add(l.getEntity());
					s.delete(l);
				}
			}
			

			for (IntelEntityLocation locationlink : deleteEntityLocationLinks){
				s.delete(locationlink);
				modifiedEntities.add(locationlink.getEntity());
			}
			
			s.flush();
			s.clear();
			s.saveOrUpdate(record);
			s.flush();
			
			for (IntelEntityAttachment entityAttachments : summaryPage.getNewAttachments()){
				s.save(entityAttachments);
				entityAttachments.getEntity().getEntityAttachments().add(entityAttachments);
				modifiedEntities.add(entityAttachments.getEntity());
			}
			
			
			for (IntelEntityLocation locationlink : newEntityLocationLinks){
				s.saveOrUpdate(locationlink.getLocation());
				s.save(locationlink);
				modifiedEntities.add(locationlink.getEntity());
			}
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
			
			currentEntityLocationLinks.addAll(newEntityLocationLinks);
			newEntityLocationLinks.clear();
			deleteEntityLocationLinks.clear();
			locationsToDelete.clear();
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
		summaryPage.enableWs(WorkingSetManager.INSTANCE.isSet() && getRecord().getUuid() != null);
		mapPage.refresh();
		super.setPartName(record.getTitle());
	}
	
	public void refresh(){
		loadRecordJob.schedule();
	}
	
	public RecordSummaryPage getSummaryPage(){
		return this.summaryPage;
	}
	
	private void subscribeToEvent(String eventTopic, EventHandler handler){
		parentContext.get(IEventBroker.class).subscribe(eventTopic, handler);
		handlers.add(handler);
	}
	
	@Override
	protected void createPages() {
		
		
		showBusy(true);
		try {
			handlers = new ArrayList<EventHandler>();
			
			parentContext = (IEclipseContext) getSite().getService(IEclipseContext.class);
			
			//configure tags so editors show in both perspectives
			MPart part = parentContext.get(MPart.class); 
			if (!part.getTags().contains(IntelDataAssessmentPerspective.ID)) part.getTags().add(IntelDataAssessmentPerspective.ID);
			if (!part.getTags().contains(IntelDataAnalysisPerspective.ID)) part.getTags().add(IntelDataAnalysisPerspective.ID);
			
			//on delete close editor
			subscribeToEvent(IntelEvents.RECORD_DELETE, (event)->{
				Object data = event.getProperty(IEventBroker.DATA);
				if (data != null && data.equals(record)){
					getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(RecordEditor.this, false);
				}
			});
			
			//wsset active
			subscribeToEvent(IntelEvents.ACTIVE_WS_SET, (event)->{
				summaryPage.enableWs(WorkingSetManager.INSTANCE.isSet() && getRecord().getUuid() != null);
			});
		

			//entity deleted
			subscribeToEvent(IntelEvents.ENTITY_DELETE, (event)->{
				IntelEntity entity = (IntelEntity) event.getProperty(IEventBroker.DATA);
				if (isDirty){
					//try to just remove this entity link
					MessageDialog.openWarning(getSite().getShell(), "Warning", MessageFormat.format("The record {0} has local modifications and could not be refreshed after the entity {0} was deleted.  You may need to manually remove the entity or refresh the editor and drop all changes.", getEditorInput().getName(), entity.getIdAttributeAsText()) );
				}else{
					//refresh the entire editor
					refresh();
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
		}finally{
			showBusy(false);
		}	
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

	public List<IntelEntityLocation> getEntityLocationLinks(){
		List<IntelEntityLocation> links = new ArrayList<IntelEntityLocation>();
		links.addAll(currentEntityLocationLinks);
		links.addAll(newEntityLocationLinks);
		return links;
	}
	public IntelRecord getRecord(){
		return this.record;
	}
	
	public void locationsUpdated(){
		setDirty(true);
		summaryPage.getLocationPanel().refreshTable();
		mapPage.refresh();
	}
	
	/**
	 * unlinks entity from the given location; if entity is null it will unlink
	 * all entities associated with the location
	 * 
	 * @param location
	 * @param entity can be null in which case all entities are unlinked
	 */
	public void unlinkEntityFromLocation(IntelLocation location, IntelEntity entity){
		for (Iterator<IntelEntityLocation> iterator = newEntityLocationLinks.iterator(); iterator.hasNext();) {
			IntelEntityLocation newLink = (IntelEntityLocation) iterator.next();
			if (newLink.getLocation().equals(location) && (entity == null || entity.equals(newLink.getEntity()))){
				iterator.remove();
			}
		}
			
		for (Iterator<IntelEntityLocation> iterator = currentEntityLocationLinks.iterator(); iterator.hasNext();) {
			IntelEntityLocation newLink = (IntelEntityLocation) iterator.next();
			if (newLink.getLocation().equals(location) && (entity == null || entity.equals(newLink.getEntity()))){
				deleteEntityLocationLinks.add(newLink);
				iterator.remove();
			}
		}
		
		summaryPage.getEntityPanel().init();
		summaryPage.getLocationPanel().refreshTable();
		mapPage.refresh();
		setDirty(true);
	}
	
	
	public void linkEntityToLocation(IntelLocation location, IntelEntity entity){
		IntelEntityLocation newitem = new IntelEntityLocation();
		newitem.setLocation(location);
		newitem.setEntity(entity);
		
		//only add if it doesn't already exist
		if (!currentEntityLocationLinks.contains(newitem) && !newEntityLocationLinks.contains(newitem)){
			newEntityLocationLinks.add(newitem);
		}
		summaryPage.getEntityPanel().init();
		summaryPage.getLocationPanel().refreshTable();
		mapPage.refresh();
		setDirty(true);
	}
	
	/**
	 * creates a new location with given date time.  If datetime is null; then uses the record
	 * date time.  Geometry is required.
	 * @param p
	 * @param dateTime
	 */
	public void addNewLocation(@NotNull Geometry p, Date dateTime){
		if (record.getLocations() == null) record.setLocations(new ArrayList<IntelLocation>());
		
		IntelLocation newLocation = new IntelLocation();
		newLocation.setComment(null);
		newLocation.setConservationArea(SmartDB.getCurrentConservationArea());
		newLocation.setDateTime(dateTime == null ? record.getDateCreated() : dateTime);
		newLocation.setGeometry(p);
		newLocation.setId(MessageFormat.format("Location {0}", record.getLocations().size()+1));
		newLocation.setRecord(record);
		newLocation.setObservations(new ArrayList<IntelObservation>());
		
		record.getLocations().add(newLocation);
		
		summaryPage.getLocationPanel().refreshTable();
		mapPage.refresh();
		setDirty(true);
	}
	
	public void deleteLocation(IntelLocation location){
		record.getLocations().remove(location);
		location.setRecord(null);
		locationsToDelete.add(location);
		
		List<IntelEntityLocation> toRemove = new ArrayList<IntelEntityLocation>();
		for (IntelEntityLocation e : currentEntityLocationLinks){
			if (e.getLocation().equals(location)){
				toRemove.add(e);
			}
		}
		currentEntityLocationLinks.removeAll(toRemove);
		toRemove = new ArrayList<IntelEntityLocation>();
		for (IntelEntityLocation e : newEntityLocationLinks){
			if (e.getLocation().equals(location)){
				toRemove.add(e);
			}
		}
		newEntityLocationLinks.removeAll(toRemove);
		
		summaryPage.getEntityPanel().init();
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
								Intelligence2PlugIn.log(ex.getMessage(), ex);
							}
						}
						if (tolink.getEntityAttachments() != null){
							for (IntelEntityAttachment a : tolink.getEntityAttachments()){
								try{
									a.getAttachment().computeFileLocation(s);	
								}catch (Exception ex){
									Intelligence2PlugIn.log(ex.getMessage(), ex);
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
