package org.wcs.smart.query.ui.queryfilter;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.ISourceProviderService;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.SourceProvider;
import org.wcs.smart.query.ui.SourceProvider.QueryPartPanelType;

public abstract class AbstractQueryItemPanel{
	
	protected static final String LOADING_TEXT = Messages.QueryFilterView_LoadingLabel;
	
	private Composite panel = null;
	
	public AbstractQueryItemPanel() {
	}
	
	/**
	 * Refreshes the panel contents
	 */
	public abstract void refreshPanel();
	
	/**
	 * Creates the panel contents
	 * @param parent
	 * @return
	 */
	protected abstract Composite createPanel(Composite parent);
	
	/**
	 * 
	 * @return the panel type associated with this panel
	 */
	public abstract QueryPartPanelType getValidType();
	
	public Composite getPanel(Composite parent){
		if (panel == null){
			panel = createPanel(parent);
			refreshPanel();
		}
		return panel;
		
	}
	/**
	 * Updates the source provider with the 
	 * selection from the data tree.
	 */
	public void addQueryItem(IStructuredSelection currentSelection){
		IWorkbenchPartSite site = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().getSite();
		SourceProvider provider = (SourceProvider) ((ISourceProviderService)site.getService(ISourceProviderService.class)).getSourceProvider(SourceProvider.SELECTED_FILTERS);
		provider.setFilterSelection(currentSelection);
	}
	
	
}
