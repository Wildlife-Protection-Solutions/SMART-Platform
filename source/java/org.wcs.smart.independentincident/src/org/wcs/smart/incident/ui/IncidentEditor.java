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
package org.wcs.smart.incident.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IncidentManager;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.event.IIncidentListener;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.events.IWaypointEventListener;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.events.WaypointEventManager.EventType;
import org.wcs.smart.observation.model.ObservationOptions;
import org.wcs.smart.observation.model.Waypoint;
/**
 * Incident editor.
 * 
 * @author Emily
 *
 */
public class IncidentEditor extends MultiPageEditorPart implements MapPart{ //,IAdaptable{

	public static final String ID = "org.wcs.smart.incident.ui.IncidentEditor"; //$NON-NLS-1$

	private Waypoint incident = null;
	private IncidentSummaryPage summaryEditor;
	private IncidentMapPage mapPage;
	private ObservationOptions ops;
	
	/*
	 * Waypoint listener
	 */
	private IWaypointEventListener wlistener = new IWaypointEventListener() {
		
		@Override
		public void handleEvent(Waypoint wp) {
			if (wp.equals(incident)){
				//reset
				reloadIncident();
			}
		}
	};
	
	/*
	 * Incident listener
	 */
	private IIncidentListener listener = new IIncidentListener() {
		
		@Override
		public void handleEvent(int eventType, Object source) {
			if (eventType == IncidentEventManager.INCIDENT_MODIFIED){
				//if this editor matches the incident being modified
				//we need to reload values
				if ((source instanceof Waypoint &&
						((Waypoint)source).equals(incident) ) ||
						(source instanceof IncidentEditorInput &&
								(((IncidentEditorInput)source).getUuid().equals(incident.getUuid())))) {
					
					reloadIncident();
					
				}
						
			}else if (eventType == IncidentEventManager.INCIDENT_DELETED){
				//if this editor matches the item deleted we need to
				//close the editor
				if ((source instanceof Waypoint &&
						((Waypoint)source).equals(incident) ) ||
						(source instanceof IncidentEditorInput &&
								((IncidentEditorInput)source).getUuid().equals(incident.getUuid()))) {
					
					//close this editor
					getEditorSite().getShell().getDisplay().asyncExec(new Runnable(){
						@Override
						public void run() {
							getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(IncidentEditor.this, false);					
						}});
				}
			}
		}
	};
	
	public IncidentEditor() {
		super();
	}

	@Override
	public void dispose() {
		IncidentEventManager.getInstance().removeListener(listener);
		WaypointEventManager.getInstance().removeListener(EventType.WAYPOINT_MODIFIED, wlistener);
		super.dispose();
	}

	
	/**
	 * refreshed the incident and the editor values
	 */
	private void reloadIncident(){
		//reload incident
		incident = null;
		getIncident();
		
		//update editor name
		((IncidentEditorInput)getEditorInput()).setId(incident.getId());
		((IncidentEditorInput)getEditorInput()).setDateTime(incident.getDateTime());
		setPartName(((IncidentEditorInput)getEditorInput()).getName());
		
		summaryEditor.initData(incident);
		mapPage.updatePointsLayer();
		
		super.setPartName(MessageFormat.format(Messages.IncidentEditorInput_EditorName, new Object[]{String.valueOf(getIncident().getId())}));
	}
	/**
	 * 
	 * @return null if the incident can be editted, otherwise a string
	 * that described reason why can't be edited.
	 */
	public String canEdit(){
		return IncidentManager.getInstance().canEdit(incident, ops);
	}
	

	public ObservationOptions getOptions(){
		return this.ops;
	}
	
	
	/**
	 * Loads the incident 
	 * @return
	 */
	public Waypoint getIncident(){
		if (this.incident == null){
			
			UUID uuid = ((IncidentEditorInput) getEditorInput()).getUuid();
			Session session = HibernateManager.openSession();
			try{
				//load incident 
				session.beginTransaction();
				this.incident = (Waypoint) session.load(Waypoint.class, uuid);
				this.incident.getId();
				
				try{
					ObservationHibernateManager.computeAttachmentLocations(incident, session);
				}catch (Exception ex){
					ObservationPlugIn.displayLog(ex.getMessage(), ex);
				}
				
				session.getTransaction().commit();
				
				if (ops == null){
					ops = ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(),session);
				}
			}finally{			
				session.close();
			}
		}
		return this.incident;
	}

	/**
	 * Updates the incident editor part name 
	 */
	public void updatePartName(){
		IncidentEditorInput input = ((IncidentEditorInput) getEditorInput());
		super.setPartName(MessageFormat.format(Messages.IncidentEditor_EditorPartName, input.getId()));
	}
	
	
	/** Creates the summary and map pages
	 * @see org.eclipse.ui.part.MultiPageEditorPart#createPages()
	 */
	@Override
	protected void createPages() {
		super.setPartName(MessageFormat.format(Messages.IncidentEditorInput_EditorName, new Object[]{String.valueOf(getIncident().getId())}));
		showBusy(true);
		try {
			
			getIncident();
			
			summaryEditor= new IncidentSummaryPage(this);
			int i = addPage(summaryEditor, getEditorInput());
			setPageText(i, Messages.IncidentEditor_DetailsPageName);
			setPageImage(i, IncidentPlugIn.getDefault().getImageRegistry().get(IncidentPlugIn.INCIDENT_ICON));
			
			mapPage = new IncidentMapPage(this);
			int mapIndex = addPage(mapPage, getEditorInput());
			setPageText(mapIndex, Messages.IncidentEditor_MapPageName);
			setPageImage(mapIndex, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.MAP_ICON));
			
			//-- event managers --
			IncidentEventManager.getInstance().addListener(listener);
			WaypointEventManager.getInstance().addListener(EventType.WAYPOINT_MODIFIED, wlistener);
			
		} catch (final Throwable t) {
			getSite().getPage().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable(){
				@Override
						public void run() {
							try {
								IncidentEditor.this.dispose();
								IncidentEditor.this.getSite().getPage().closeEditor(IncidentEditor.this, false);
								if (t instanceof SWTError&& t.getMessage().contains("No more handles")) { //$NON-NLS-1$
									IncidentPlugIn.displayLog(Messages.IncidentEditor_EditorError1 + t.getLocalizedMessage(), t);
								} else {
									IncidentPlugIn.displayLog(Messages.IncidentEditor_EditorError2 + t.getLocalizedMessage(), t);
								}
							} catch (Exception ex) {
								IncidentPlugIn.log("Failure",ex); //$NON-NLS-1$
							}

						}
			});
			throw new RuntimeException(Messages.IncidentEditor_EditorError2 + t.getMessage(), t);
		}finally{
			showBusy(false);
		}
		
		getSite().setSelectionProvider(new ISelectionProvider() {
			private Collection<ISelectionChangedListener> listeners = new ArrayList<ISelectionChangedListener>();
			@Override
			public void setSelection(ISelection selection) {
				for (ISelectionChangedListener list: listeners){
					list.selectionChanged(new SelectionChangedEvent(this, selection));
				}
			}
			
			@Override
			public void removeSelectionChangedListener(
					ISelectionChangedListener listener) {
				listeners.remove(listener);
			}
			
			@Override
			public ISelection getSelection() {
				return new StructuredSelection(getIncident());
			}
			
			@Override
			public void addSelectionChangedListener(ISelectionChangedListener listener) {
				listeners.add(listener);
			}
		});
	}
	
	/** 
	 * @return false
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	
	/** Does nothing
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	/** Does nothing
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
	}

	/* (non-Javadoc)
	 * @see org.locationtech.udig.project.ui.internal.MapPart#getMap()
	 */
	@Override
	public Map getMap() {
		if (mapPage == null){
			return null;
		}
		return 	mapPage.getMap();
	}

	/* (non-Javadoc)
	 * @see org.locationtech.udig.project.ui.internal.MapPart#openContextMenu()
	 */
	@Override
	public void openContextMenu() {
		mapPage.openContextMenu();
		
	}

	/* (non-Javadoc)
	 * @see org.locationtech.udig.project.ui.internal.MapPart#setFont(org.eclipse.swt.widgets.Control)
	 */
	@Override
	public void setFont(Control textArea) {
		mapPage.setFont(textArea);
		
	}

	/* (non-Javadoc)
	 * @see org.locationtech.udig.project.ui.internal.MapPart#setSelectionProvider(org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider)
	 */
	@Override
	public void setSelectionProvider(
			IMapEditorSelectionProvider selectionProvider) {
		mapPage.setSelectionProvider(selectionProvider);
		
	}

	/* (non-Javadoc)
	 * @see org.locationtech.udig.project.ui.internal.MapPart#getStatusLineManager()
	 */
	@Override
	public IStatusLineManager getStatusLineManager() {
		return mapPage.getStatusLineManager();
	}

//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	public Object getAdapter(Class adaptee) {
//		if (adaptee.isAssignableFrom(Map.class)) {
//			return getMap();
//		}
//		return super.getAdapter(adaptee);
//	}
	public void setFocus() {
		super.setFocus();
		getSite().getSelectionProvider().setSelection(getSite().getSelectionProvider().getSelection());
	}
}
