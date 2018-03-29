/*
 * Copyright (C) 2016 Wildlife Conservation Society
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

package org.wcs.smart.asset.ui.views.map;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.map.engine.parser.Parser;
import org.wcs.smart.asset.ui.views.map.IOverviewTableColumn.ColumnType;

/**
 * Composite for configuring combined columns for asset overview column
 * @author Emily
 *
 */
public class CombinedColumnComposite extends Composite {

	private Text txtAttributeFilters;
	private ListViewer columnOptions;
	
	private CombinedOverviewColumn newColumn = null;
	private CombinedOverviewColumn toUpdate = null;
	
	private CategoryColumnDialog dialog;


	/**
	 * Creates a new dialog for editing an existing column
	 * @param parentShell
	 * @param toUpdate can be null if we are creating a new one
	 */
	public CombinedColumnComposite(Composite parent, CategoryColumnDialog dialog,  IOverviewTableColumn toUpdate) {
		super(parent, SWT.NONE);
		this.toUpdate = (CombinedOverviewColumn) toUpdate;
		this.dialog = dialog;
		
		createComposite();
	}
	
	/**
	 * 
	 * @return the new column created; this will return null if we are updating a column
	 */
	public CombinedOverviewColumn getNewColumn() {
		return newColumn;
	}
	
	public boolean validate() {
		String text = txtAttributeFilters.getText().trim();
		if (!text.isEmpty()) {
			
			try(InputStream is = new ByteArrayInputStream(text.getBytes())){
				Parser parser = new Parser(is);
				parser.CombinedExpression();
			} catch (Exception e) {
				e.printStackTrace();
				dialog.setErrorMessage(e.getMessage());
				return false;
			}
		}
		dialog.setErrorMessage(null);
		return true;
	}
	
	public void cancelPressed() {
		newColumn = null;
	}
	
	public void okPressed() {		
		if (toUpdate == null) {
			newColumn = new CombinedOverviewColumn(dialog.getName().trim(), txtAttributeFilters.getText());
		}else {
			toUpdate.updateValues(dialog.getName().trim(), txtAttributeFilters.getText());
		}
	}
	
	
	protected Control createComposite() {
		Composite parent = this;
		parent.setLayout(new GridLayout(2, true));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		Label l = new Label(parent, SWT.NONE);
		l.setText(Messages.CombinedColumnComposite_DefinitionLabel);
		
		l = new Label(parent, SWT.NONE);
		l.setText(Messages.CombinedColumnComposite_ColumnsLabel);
		
		txtAttributeFilters = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.WRAP);
		txtAttributeFilters.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txtAttributeFilters.addListener(SWT.Modify, e->dialog.validate());
		
		columnOptions = new ListViewer(parent, SWT.FULL_SELECTION | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL );
		columnOptions.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		columnOptions.setContentProvider(ArrayContentProvider.getInstance());
		columnOptions.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof IOverviewTableColumn) return ((IOverviewTableColumn) element).getName();
				return super.getText(element);
			}
		});
		columnOptions.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				addSelection();
			}
		});
		List<IOverviewTableColumn> columns = new ArrayList<>();
		for (IOverviewTableColumn c : dialog.getAllColumns()) {
			if (!(c instanceof CombinedOverviewColumn) && (c.getType() == ColumnType.INTEGER || c.getType() == ColumnType.NUMBER)){
				columns.add(c);
			}
		}
		columnOptions.setInput(columns);

		if (toUpdate != null) {	
			txtAttributeFilters.setText(toUpdate.getDefinition());
		}
		
		return parent;
	}
	
	
	private void addSelection() {
		Object option = columnOptions.getStructuredSelection().getFirstElement();
		String part = null;
		
		if (option instanceof IOverviewTableColumn) {
			IOverviewTableColumn item = (IOverviewTableColumn) option;
			part = "[" + item.getKey() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}
		if (part != null) {
			if (txtAttributeFilters.getText().isEmpty()) {
				txtAttributeFilters.setText(part);
			}else {
				txtAttributeFilters.setText(txtAttributeFilters.getText() + " < + | - | * | / > " + part); //$NON-NLS-1$
			}
		}
	}
	
}