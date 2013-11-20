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
package org.wcs.smart.query.ui.itempanel;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.ISourceProviderService;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.QuerySourceProvider;

/**
 * Abstract query item panel. 
 * @author Emily
 *
 */
public abstract class AbstractQueryItemPanel implements IQueryItemPanel{
	
	protected static final String LOADING_TEXT = Messages.QueryFilterView_LoadingLabel;
	
	/**
	 * Sets the current selection.
	 */
	public void addQueryItem(IStructuredSelection currentSelection)
	{
		IWorkbenchPartSite site = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite();
		QuerySourceProvider provider = (QuerySourceProvider) ((ISourceProviderService)site.getService(ISourceProviderService.class)).getSourceProvider(QuerySourceProvider.DEFINITION_ITEMS);
		provider.setQueryDefinitionSelection(currentSelection, getId());
	}

}
