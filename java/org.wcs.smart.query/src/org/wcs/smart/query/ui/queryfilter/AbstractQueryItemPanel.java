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
package org.wcs.smart.query.ui.queryfilter;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.ISourceProviderService;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.SourceProvider;
import org.wcs.smart.query.ui.SourceProvider.QueryPartPanelType;

/**
 * A panel to display in the Query Filter View.  This
 * panel displays options that are available to add
 * to the current active definition panel.
 * 
 * @author Emily
 *
 */
public abstract class AbstractQueryItemPanel{
	/**
	 * Loading text label
	 */
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
	
	/**
	 * 
	 * @param parent
	 * @return the created panel.
	 */
	public Composite getPanel(Composite parent){
		if (panel == null){
			panel = createPanel(parent);
			refreshPanel();
		}
		return panel;
		
	}
	
	
	/**
	 * Updates the source provider with the 
	 * selection from the data tree.  This is used
	 * to pass the item to the query definition panel.
	 */
	public void addQueryItem(IStructuredSelection currentSelection){
		IWorkbenchPartSite site = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite();
		SourceProvider provider = (SourceProvider) ((ISourceProviderService)site.getService(ISourceProviderService.class)).getSourceProvider(SourceProvider.SELECTED_FILTERS);
		provider.setFilterSelection(currentSelection);
	}
}
