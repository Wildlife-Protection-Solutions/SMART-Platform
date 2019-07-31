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

import javax.inject.Inject;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.ui.QuerySourceProvider;

/**
 * Abstract query item panel. 
 * @author Emily
 *
 */
public abstract class AbstractQueryItemPanel implements IQueryItemPanel{
	
	protected static final String LOADING_TEXT = Messages.QueryFilterView_LoadingLabel;
	
	private @Inject QuerySourceProvider provider;
	/**
	 * Sets the current selection.
	 */
	public void addQueryItem(IStructuredSelection currentSelection)
	{
		provider.setQueryDefinitionSelection(currentSelection, getId());
	}
	
	protected abstract void addItem();
	
	protected void createAddButton(TreeViewer filterTreeViewer, Composite parent) {
		Menu m = new Menu(filterTreeViewer.getControl());
		MenuItem add = new MenuItem(m, SWT.PUSH);
		add.setText(Messages.AbstractQueryItemPanel_AddToQueryItem);
		add.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		add.addListener(SWT.Selection, e->addItem( ));
		filterTreeViewer.getControl().setMenu(m);
		
		Button btnAdd = new Button(parent, SWT.PUSH);
		btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAdd.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAdd.setText(Messages.AbstractQueryItemPanel_AddToQueryItem);
		btnAdd.addListener(SWT.Selection, e->addItem());
	}
}
