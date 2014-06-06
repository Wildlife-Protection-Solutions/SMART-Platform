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
package org.wcs.smart.common.filter;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.internal.Messages;

/**
 * Composite with required controls for string filtering. Used inside filter dialogs
 * 
 * @author elitvin
 * @author Emily
 * @since 1.0.0
 */
public class StringFilterComposite extends Composite {

	private Button btnIncludeAll;
	private Button btnFilter;
	private Label lblValue;
	private ComboViewer comparatorViewer;
	private Text txtFilter;
	
	private TextField[] searchFields;
	private StringComparison[] compOptions;
	private ComboViewer searchField;

	/**
	 * Types of comparation.
	 */
	public enum StringComparison {
		EQUALS(Messages.StringComparison_Equals),
		CONTAINS(Messages.StringComparison_Contains);
		
		private String guiName;
		private StringComparison(String guiName){
			this.guiName = guiName;
		}
		public String getGuiName(){
			return this.guiName;
		}
	}
	
	
	
	/**
	 * @param parent
	 * @param style
	 */
	public StringFilterComposite(Composite parent, int style, TextField[] searchFields) {
		super(parent, style);
		this.searchFields = searchFields;
		this.compOptions = StringComparison.values();
		createControls();
	}
	
	/**
	 * @param parent
	 * @param style
	 */
	public StringFilterComposite(Composite parent, int style, TextField[] searchFields,
			StringComparison[] comps) {
		super(parent, style);
		this.searchFields = searchFields;
		this.compOptions = comps;
		createControls();
	}


	private void createControls() {
		this.setLayout(new GridLayout(1, false));
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite idComp = new Composite(this, SWT.NONE);
		idComp.setLayout(new GridLayout(1, false));
		idComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		btnIncludeAll = new Button(idComp, SWT.RADIO);
		btnIncludeAll.setText(Messages.StringFilterComposite_IncludeAll_Label);
		
		btnFilter = new Button(idComp, SWT.RADIO);
		btnFilter.setText(Messages.StringFilterComposite_Filter_Label);
		
		Composite comp = new Composite(this, SWT.NONE);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		comp.setLayout(new GridLayout(3, false));
		
		if (searchFields.length <= 1){
			lblValue = new Label(comp, SWT.NONE);
			lblValue.setText(searchFields[0].getGuiName());
		}else{
			searchField = new ComboViewer(comp, SWT.DROP_DOWN | SWT.READ_ONLY);
			searchField.setContentProvider(ArrayContentProvider.getInstance());
			searchField.setInput(searchFields);
			searchField.setLabelProvider(new LabelProvider(){
				public String getText(Object element){
					return ((TextField)element).getGuiName();
				}
			});
			searchField.setSelection(new StructuredSelection(searchFields[0]));
		}
		
		comparatorViewer = new ComboViewer(comp, SWT.READ_ONLY);
		comparatorViewer.setContentProvider(ArrayContentProvider.getInstance());
		comparatorViewer.setInput(compOptions);
		comparatorViewer.setSelection(new StructuredSelection(StringComparison.CONTAINS));		
		comparatorViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof StringComparison){
					return ((StringComparison)element).getGuiName();
				}
				return super.getText(element);
			}
		});
		
		txtFilter = new Text(comp, SWT.BORDER);
		txtFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnFilter.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setFilteringEnabled(true);
			}
		});
		btnIncludeAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setFilteringEnabled(false);
			}
		});
	}

	/**
	 * Sets the "include all" label
	 * @param text
	 */
	public void setIncludeAllRadioLabel(String text) {
		btnIncludeAll.setText(text);
	}

	/**
	 * Sets the filter link radio label
	 * @param text
	 */
	public void setFilterRadioLabel(String text) {
		btnFilter.setText(text);
	}

	/**
	 * Updates the controls to the given values 
	 * @param comparison comparison operator
	 * @param text search value
	 * @param field field to search, can be <code>null</code> if only one field to search
	 */
	public void applyState(StringComparison comparison, String text, TextField field) {
		boolean enabled = comparison != null && text != null;
		if (enabled) {
			txtFilter.setText(text);
			comparatorViewer.setSelection(new StructuredSelection(comparison));
		} else {
			comparatorViewer.setSelection(new StructuredSelection(compOptions[0]));
		}
		setFilteringEnabled(enabled);
		if (field != null && searchField != null){
			searchField.setSelection(new StructuredSelection(field));
		}
	}
	
	private void setFilteringEnabled(boolean enabled) {
		btnFilter.setSelection(enabled);
		btnIncludeAll.setSelection(!enabled);
		txtFilter.setEnabled(enabled);
		comparatorViewer.getControl().setEnabled(enabled);
		if (lblValue != null){
			lblValue.setEnabled(enabled);
		}
		if (searchField != null){
			searchField.getCombo().setEnabled(enabled);
		}
	}

	/**
	 * 
	 * @return the string comparison operator
	 */
	public StringComparison getComparisonForModel() {
		if (btnFilter.getSelection()) {
			return (StringComparison)((IStructuredSelection)comparatorViewer.getSelection()).getFirstElement();
		}
		return null;
	}

	/**
	 * 
	 * @return the filter value
	 */
	public String getFilterValueForModel() {
		if (btnFilter.getSelection()) {
			return txtFilter.getText();
		}
		return null;
	}
	
	/**
	 * 
	 * @return the field selected for searching
	 */
	public TextField getSelectedField(){
		if (searchField != null){
			return (TextField) ((IStructuredSelection)searchField.getSelection()).getFirstElement();
		}else{
			return searchFields[0];
		}
	}

	/**
	 * Class to describe the field fields.
	 * @author Emily
	 *
	 */
	public static class TextField{
		private String guiName;
		private String dbName;
		/**
		 * 
		 * @param guiName the name to display to the user
		 * @param dbFieldName the hibernate object field to
		 * use when generating hql
		 */
		public TextField(String guiName, String dbFieldName){
			this.guiName = guiName;
			this.dbName = dbFieldName;
		}
		public String getGuiName(){
			return this.guiName;
		}
		public String getDbFieldName() {
			return this.dbName;
		}
	}
}
