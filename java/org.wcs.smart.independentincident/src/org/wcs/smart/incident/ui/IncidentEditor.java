package org.wcs.smart.incident.ui;

import java.text.MessageFormat;
import java.util.List;

import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.ui.internal.MapPart;
import net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.hibernate.Session;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.observation.model.Waypoint;

public class IncidentEditor extends MultiPageEditorPart implements MapPart, IAdaptable{

	public static final String ID = "org.wcs.smart.incident.ui.IncidentEditor"; //$NON-NLS-1$

	private Waypoint incident = null;
	
	private IncidentSummaryPage summaryEditor;
	private IncidentMapPage mapPage;
	
	private Projection[] projections;
	
	//TODO:
	private ISelectionProvider selectionProvider = null;
	
	public IncidentEditor() {
		super();
	}

	public Projection[] getAvailableProjections(){
		return this.projections;
	}
	
	@Override
	public void dispose() {
		super.dispose();
	

	}

	
	/**
	 * 
	 * @return null if the patrol can be editted, otherwise a string
	 * that described reason why can't be edited.
	 */
	public String canEdit(){
//		return PatrolManager.getInstance().canEdit(patrol, ops);
		return null;
	}
	
	public Waypoint getIncident(){
		if (this.incident == null){
			
			byte[] uuid = ((IncidentEditorInput) getEditorInput()).getUuid();
			Session session = HibernateManager.openSession();
			try{
				//load patrol items so don't have lazy loading issues later.
				session.beginTransaction();
				this.incident = (Waypoint) session.load(Waypoint.class, uuid);
				List<Projection> tmp = HibernateManager.getCaProjectionList(session);
				this.projections = tmp.toArray(new Projection[tmp.size()]);
			
				session.getTransaction().commit();
			}finally{			
				session.close();
			}
		}
		return this.incident;
	}

	public void updatePartName(){
		IncidentEditorInput input = ((IncidentEditorInput) getEditorInput());
		super.setPartName(MessageFormat.format("Incident {0}", input.getId()));
	}
	
	
	@Override
	protected void createPages() {
		IncidentEditorInput input = ((IncidentEditorInput) getEditorInput());
		super.setPartName(input.getName());
		showBusy(true);
		try {
			
			getIncident();
			
			summaryEditor= new IncidentSummaryPage(this);
			int i = addPage(summaryEditor, getEditorInput());
			setPageText(i, "Details");
			
			mapPage = new IncidentMapPage(this);
			int mapIndex = addPage(mapPage, getEditorInput());
			setPageText(mapIndex, "Map");
			
			getSite().setSelectionProvider(selectionProvider);
		} catch (final Throwable t) {
			getSite().getPage().getWorkbenchWindow().getShell().getDisplay().asyncExec(new Runnable(){
				@Override
						public void run() {
							try {
								IncidentEditor.this.dispose();
								IncidentEditor.this.getSite().getPage().closeEditor(IncidentEditor.this, false);
								if (t instanceof SWTError&& t.getMessage().contains("No more handles")) { //$NON-NLS-1$
									IncidentPlugIn.displayLog("Incident editor could not be created.  Please try closing existing open editors and try again. " + t.getLocalizedMessage(), t);
								} else {
									IncidentPlugIn.displayLog("Error occurred while loading editor. " + t.getLocalizedMessage(), t);
								}
							} catch (Exception ex) {
								IncidentPlugIn.log("Failure",ex); //$NON-NLS-1$
							}

						}
			});

		}finally{
			showBusy(false);
		}
	}
	
	public ISelectionProvider getSelectionProvider(){
		return this.selectionProvider;
	}
	
//	public void updateSummaryPage(){
//		//summaryEditor.refreshPatrolSummaryTable();
//	}
//	
	
	
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	
	@Override
	public void doSave(IProgressMonitor monitor) {

	}

	@Override
	public void doSaveAs() {
	}

	/* (non-Javadoc)
	 * @see net.refractions.udig.project.ui.internal.MapPart#getMap()
	 */
	@Override
	public Map getMap() {
		if (mapPage == null){
			return null;
		}
		return 	mapPage.getMap();
	}

	/* (non-Javadoc)
	 * @see net.refractions.udig.project.ui.internal.MapPart#openContextMenu()
	 */
	@Override
	public void openContextMenu() {
		mapPage.openContextMenu();
		
	}

	/* (non-Javadoc)
	 * @see net.refractions.udig.project.ui.internal.MapPart#setFont(org.eclipse.swt.widgets.Control)
	 */
	@Override
	public void setFont(Control textArea) {
		mapPage.setFont(textArea);
		
	}

	/* (non-Javadoc)
	 * @see net.refractions.udig.project.ui.internal.MapPart#setSelectionProvider(net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider)
	 */
	@Override
	public void setSelectionProvider(
			IMapEditorSelectionProvider selectionProvider) {
		mapPage.setSelectionProvider(selectionProvider);
		
	}

	/* (non-Javadoc)
	 * @see net.refractions.udig.project.ui.internal.MapPart#getStatusLineManager()
	 */
	@Override
	public IStatusLineManager getStatusLineManager() {
		return mapPage.getStatusLineManager();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object getAdapter(Class adaptee) {
		if (adaptee.isAssignableFrom(Map.class)) {
			return getMap();
		}
		return super.getAdapter(adaptee);
	}
	
}
