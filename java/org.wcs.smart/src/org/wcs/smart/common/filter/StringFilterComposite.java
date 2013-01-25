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
	public StringFilterComposite(Composite parent, int style) {
		super(parent, style);
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
		lblValue = new Label(comp, SWT.NONE);
		
		comparatorViewer = new ComboViewer(comp, SWT.READ_ONLY);
		comparatorViewer.setContentProvider(ArrayContentProvider.getInstance());
		comparatorViewer.setInput(StringComparison.values());
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
				comparatorViewer.getCombo().setEnabled(true);
				txtFilter.setEnabled(true);
				lblValue.setEnabled(true);
			}
		});
		btnIncludeAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				comparatorViewer.getCombo().setEnabled(false);
				txtFilter.setEnabled(false);
				lblValue.setEnabled(false);
			}
		});
	}

	public void setIncludeAllRadioLabel(String text) {
		btnIncludeAll.setText(text);
	}

	public void setFilterRadioLabel(String text) {
		btnFilter.setText(text);
	}
	
	public void setValueLabel(String text) {
		lblValue.setText(text);
	}

	public void applyState(StringComparison comparison, String text) {
		boolean enabled = comparison != null && text != null;
		if (enabled) {
			txtFilter.setText(text);
			comparatorViewer.setSelection(new StructuredSelection(comparison));
		} else {
			comparatorViewer.setSelection(new StructuredSelection(StringComparison.CONTAINS));
		}
		setFilteringEnabled(enabled);
		
	}
	
	private void setFilteringEnabled(boolean enabled) {
		btnFilter.setSelection(enabled);
		btnIncludeAll.setSelection(!enabled);
		txtFilter.setEnabled(enabled);
		comparatorViewer.getControl().setEnabled(enabled);
		lblValue.setEnabled(enabled);
	}

	public StringComparison getComparisonForModel() {
		if (btnFilter.getSelection()) {
			return (StringComparison)((IStructuredSelection)comparatorViewer.getSelection()).getFirstElement();
		}
		return null;
	}

	public String getFilterValueForModel() {
		if (btnFilter.getSelection()) {
			return txtFilter.getText();
		}
		return null;
	}
	
	public Button getBtnIncludeAll() {
		return btnIncludeAll;
	}

	public Button getBtnFilter() {
		return btnFilter;
	}

	public ComboViewer getComparatorViewer() {
		return comparatorViewer;
	}

	public Text getTxtFilter() {
		return txtFilter;
	}

}
