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
package org.wcs.smart.i2.ui.views.entity.search;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.search.AdvancedEntitySearch;
import org.wcs.smart.i2.ui.views.EntitySearchView;

/**
 * Advanced search panel for entity searches
 * 
 * @author Emily
 *
 */
public class AdvancedEntitySearchPanel extends Composite {

	private EntitySearchView view;
	private AdvancedEntitySearch search = null;
	
	private EntitySearchPanel searchPanel;
	
	public AdvancedEntitySearchPanel(Composite parent, EntitySearchView view, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.view = view;
		createContents(toolkit);
	}
	
	private void createContents(FormToolkit toolkit){
		
		setLayout(new GridLayout());
		
		searchPanel = new EntitySearchPanel(this) {
			@Override
			public void saveSearch() {
				AdvancedEntitySearchPanel.this.saveSearch();
			}

			@Override
			public void doSearch() {
				AdvancedEntitySearchPanel.this.doSearch();
			}
			
		};
		searchPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	private void saveSearch(){
		if (!configureSearch()) return;	
		view.saveSearch(search);
	}
	
	public void doSearch(){
		if (!configureSearch()) return;
		view.doAdvancedSearch(search, 0);
	}
	
	private boolean configureSearch(){
		String error = searchPanel.validate();
		if (error != null){
			MessageDialog.openError(getShell(), Messages.AdvancedEntitySearchPanel_SaveDialogTitle, Messages.AdvancedEntitySearchPanel_SaveDialogMsg);
			return false;
		}
		
		if (search == null){
			search = new AdvancedEntitySearch(SmartDB.getCurrentConservationArea());
		}
		search.setSearchString(searchPanel.getQueryString());
		return true;
	}
	
	
	public void initPanel(AdvancedEntitySearch search){
		searchPanel.initPanel(search.getSearchString());
	}
	
	
}
