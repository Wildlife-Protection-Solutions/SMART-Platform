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
package org.wcs.smart.query.ui.newwizard;

import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.query.internal.Messages;

/**
 * Page in the query wizard that contains a list
 * of items on the left and a browser on the right
 * for displaying information about the selected list item
 * 
 * @author Emily
 *
 */
public abstract class ListHelpWizardPage extends WizardPage {

	private TableViewer options = null;
	protected Browser helpPage = null;

	protected ListHelpWizardPage(String pageName) {
		super(pageName);
	}

	/**
	 * Sets the list options and their label provider
	 * @param data
	 * @param lblProvider
	 */
	public void setOptions (List<?> data, LabelProvider lblProvider){
		if (options != null){
			options.setInput(data);
			options.setLabelProvider(lblProvider);
		}
	}
	@Override
	public void createControl(Composite parent) {
		SashForm main = new SashForm(parent, SWT.HORIZONTAL);

		Composite tv = new Composite(main, SWT.NONE);
		tv.setLayout(new GridLayout());
		
		options = new TableViewer(tv, SWT.BORDER);
		options.setContentProvider(ArrayContentProvider.getInstance());
		options.getTable().setLinesVisible(false);

		TableColumn singleColumn = new TableColumn(options.getTable(), SWT.NONE);
		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		tableColumnLayout.setColumnData(singleColumn, new ColumnWeightData(100));
		tv.setLayout(tableColumnLayout);
	
		Composite hp = new Composite(main, SWT.BORDER);
		GridLayout gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		hp.setLayout(gl);
		helpPage = new Browser(hp, SWT.NONE);
		helpPage.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		options.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateHelpPage();
			}
		});
		
		options.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				if (!getWizard().canFinish()){
					getWizard().getContainer().showPage(getNextPage());
				}
				
			}
		});
		options.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				setPageComplete(getSelection() != null);
			}
		});
		
		setPageComplete(false);
		main.setWeights(new int[]{35,65});
		setTitle(Messages.ListHelpWizardPage_PageTitle);
		super.setControl(main);
	}

	/**
	 * 
	 * @return the current selected item
	 */
	public Object getSelection(){
		return ((StructuredSelection)options.getSelection()).getFirstElement();
	}
	
	/**
	 * Updates the browser with the help page associated with the current
	 * selection
	 */
	public abstract void updateHelpPage();
}
