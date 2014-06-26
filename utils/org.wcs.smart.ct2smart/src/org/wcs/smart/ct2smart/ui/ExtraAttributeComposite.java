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
package org.wcs.smart.ct2smart.ui;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.ExtraAttribute;
import org.wcs.smart.ct2smart.ui.support.SmartAttributeLabelProvider;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class ExtraAttributeComposite extends Composite {

	private TableViewer viewer;
	private DataModelLookup lookup;
	private Ct2Attribute attribute;

	private Button btnAdd;
	private Button btnRemove;
	
	public ExtraAttributeComposite(Composite parent, DataModelLookup lookup) {
		super(parent, SWT.NONE);
		this.lookup = lookup;
		
		GridLayout gd = new GridLayout(2, false);
		gd.marginBottom = 0;
		gd.marginHeight = 0;
		gd.marginLeft = 0;
		gd.marginRight = 0;
		gd.marginTop = 0;
		gd.marginWidth = 0;
		this.setLayout(gd);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		viewer = new TableViewer(this, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		createColumns();

		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true); 

		viewer.setContentProvider(new ArrayContentProvider());
		GridData tgridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tgridData.heightHint = 50;
		viewer.getControl().setLayoutData(tgridData);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {		
				btnRemove.setEnabled(!viewer.getSelection().isEmpty());
			}
		});
		
		Composite btnComposite = new Composite(this, SWT.NONE);
		btnComposite.setLayout(new GridLayout(1, false));
		btnComposite.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		
		btnAdd = new Button(btnComposite, SWT.PUSH);
		btnAdd.setText("Add");
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				addExtraAttribute();
			}
		});

		btnRemove = new Button(btnComposite, SWT.PUSH);
		btnRemove.setText("Remove");
		btnRemove.setEnabled(!viewer.getSelection().isEmpty());
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				deleteExtraAttribute();
			}
		});
	}

	protected void addExtraAttribute() {
		attribute.getExtraAttribute().add(new ExtraAttribute());
		viewer.refresh();
	}
	
	protected void deleteExtraAttribute() {
		Object toDel = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
		attribute.getExtraAttribute().remove(toDel);
		viewer.refresh();
	}

	public void setInput(Ct2Attribute attribute) {
		this.attribute = attribute;
		viewer.setInput(attribute.getExtraAttribute());
	}
	
	private void createColumns() {
		TableViewerColumn aCol = createTableViewerColumn("Attribute", 200, 0);
		SmartAttributeLabelProvider attrLabelProvider = new SmartAttributeLabelProvider(lookup) {
			@Override
			public String getText(Object element) {
				if (element instanceof ExtraAttribute) {
					ExtraAttribute a = (ExtraAttribute) element;
					return super.getText(a.getAttributeKey());
				}
				return super.getText(element);
			}
		};
		aCol.setLabelProvider(attrLabelProvider);
		List<AttributeType> attributes = lookup.getDataModel().getAttributes().getAttributes();
		aCol.setEditingSupport(new ExtraAttributeTableEditor(viewer, attributes, attrLabelProvider));

		
		TableViewerColumn vCol = createTableViewerColumn("Value", 200, 0);
		vCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				ExtraAttribute a = (ExtraAttribute) element;
				return a.getValueKey();
			}
		});
		
	}

	private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}
	
}
