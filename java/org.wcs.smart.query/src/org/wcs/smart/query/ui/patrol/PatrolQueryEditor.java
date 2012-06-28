package org.wcs.smart.query.ui.patrol;

import java.util.ArrayList;
import java.util.List;

import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.ui.internal.MapPart;
import net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.QueryInput;
import org.wcs.smart.query.model.QueryResultItem;
import org.wcs.smart.query.model.patrol.PatrolQuery;

public class PatrolQueryEditor extends MultiPageEditorPart implements MapPart, IAdaptable{

	public static final String ID = "org.wcs.smart.query.PatrolQueryEditor";
	private PatrolQuery currentQuery;
	
	private PatrolQueryMapPage mapPage;
	private PatrolQueryTableResultsPage tablePage;
	
	
	public PatrolQuery getQuery(){
		//TODO: see QueryResultsEditor.getQuery()
		return this.currentQuery;
	}
	/* (non-Javadoc)
	 * @see net.refractions.udig.project.ui.internal.MapPart#getMap()
	 */
	@Override
	public Map getMap() {
		if (mapPage != null){
			return mapPage.getMap();
		}
		return null;
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

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.MultiPageEditorPart#createPages()
	 */
	@Override
	protected void createPages() {
		QueryInput input = ((QueryInput) getEditorInput());
		super.setPartName(input.getName());
		showBusy(true);
		try {
			tablePage = new PatrolQueryTableResultsPage(this);
			addPage(0, tablePage, input);
			setPageText(0, "Tabular Results");
			
			if (this.currentQuery != null && this.currentQuery.getUuid() == null){
				tablePage.setQuery();
			}
			
			mapPage = new PatrolQueryMapPage(this);
			addPage(1, mapPage, input);
			setPageText(1, "Mapped Results");
			
		} catch (final Throwable t) {
			QueryPlugIn.log("Could not open query editor", t);
		}finally{
			showBusy(false);
		}
		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		//TODO see QueryResultsEditor.doSave
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
		//TODO see QueryResultsEditor.doSaveAs		
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}
	
	
	 
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setSite(site);
		super.setInput(input);
		//TODO:  see QueryResultsEditor.init()
		
	 }
	/**
	 * @see org.eclipse.ui.part.MultiPageEditorPart#dispose()
	 */
	@Override
	public void dispose() {
		//TODO: see QueryResultsEditor.init()
	}

	public void updatePartName(){
		//TODO: see QueryResultsEditor.updatePartName
	}
 
	public void setDirty(boolean isDirty){
		//TODO: see QueryResultsEditor.setDirty
	}
	
	private void updateQuery(){
		//TODO:  see QueryResultsEditor
	}

	@Override
	public boolean isDirty(){
		//TODO:  see QueryResultsEditor
		return false;
	}
	
	/**
	 * Re-run the query and refresh the results.
	 */
	public void refreshQuery(){
		//TODO:  see QueryResultsEditor	
	}

}
