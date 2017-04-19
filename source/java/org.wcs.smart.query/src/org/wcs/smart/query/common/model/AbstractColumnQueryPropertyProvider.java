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
package org.wcs.smart.query.common.model;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.hibernate.Session;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.ui.AbstractQueryPropertyProvider;

/**
 * Query property provider that lists the columns associated with the query.
 * @author Emily
 *
 */
public abstract class AbstractColumnQueryPropertyProvider extends AbstractQueryPropertyProvider {

	protected CheckboxTableViewer columnViewer;
	protected Button btnShowDataColumnsOnly;
	
	protected Link selectAll;
	protected Link deselectAll;
	
	public AbstractColumnQueryPropertyProvider() {
	}

	@Override
	public String getName(){
		return null;
	}
	
	@Override
	public abstract boolean isValid(IQueryType query);

	@Override
	public Composite createComposite(Composite parent, Query query){
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout(1, false));
		
		btnShowDataColumnsOnly = null;
		if (query instanceof IColumnAutoConfigQuery) {
			btnShowDataColumnsOnly = new Button(panel, SWT.CHECK);
			btnShowDataColumnsOnly.setText(Messages.AbstractColumnQueryPropertyProvider_ShowDataColumnsOnly);		
			btnShowDataColumnsOnly.setToolTipText(Messages.AbstractColumnQueryPropertyProvider_ShowDataColumnsOnlyTooltip);
			boolean isShowOnlyDataColumns = ((IColumnAutoConfigQuery) query).isShowDataColumnsOnly();
			btnShowDataColumnsOnly.setSelection(isShowOnlyDataColumns);
			btnShowDataColumnsOnly.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updateColumnTableState();
					fireChangeMade();
				}
			});
		}
		
		Label lblTableColumns = new Label(panel, SWT.NONE);
		lblTableColumns.setText(Messages.QueryPropertiesDialog_ColumnsLabel);
		lblTableColumns.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		createColumnTable(panel, query);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 150;
		columnViewer.getTable().setLayoutData(gd);
		
		Composite hyperlinkComposite = new Composite(panel, SWT.NONE);
		hyperlinkComposite.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false) );
		hyperlinkComposite.setLayout(new GridLayout(3, false));
		
		selectAll = new Link(hyperlinkComposite, SWT.NONE);
		selectAll.setText("<a>" + Messages.QueryPropertiesDialog_SelectAllLabel + "</a>");  //$NON-NLS-1$//$NON-NLS-2$
		selectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				columnViewer.setAllChecked(true);
				fireChangeMade();
			}
		});
		Label lbl = new Label(hyperlinkComposite, SWT.VERTICAL | SWT.SEPARATOR);
		gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.heightHint = selectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		lbl.setLayoutData(gd);
		deselectAll = new Link(hyperlinkComposite, SWT.NONE);
		deselectAll.setText("<a>" + Messages.QueryPropertiesDialog_DeSelectAllLabel + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		deselectAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				columnViewer.setAllChecked(false);
				fireChangeMade();
			}
		});
		
		updateColumnTableState();
		return panel;
	}
	
	private void createColumnTable(Composite parent, Query query){
		columnViewer = CheckboxTableViewer.newCheckList(parent, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		
		columnViewer.setContentProvider(ArrayContentProvider.getInstance());
		columnViewer.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof QueryColumn){
					return ((QueryColumn)element).getName();
				}
				return super.getText(element);
			}
		});

		//we don't care about projection here; this is just for visibility
		List<QueryColumn> cols = ((SimpleQuery)query).computeQueryColumns(Locale.getDefault(), null, null);
		columnViewer.setInput(cols);
		columnViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireChangeMade();
			}
		});
		
		for (QueryColumn col : cols){
			columnViewer.setChecked(col, col.isVisible());
		}
		
		columnViewer.getTable().addKeyListener(new KeyListener(){
			@SuppressWarnings("rawtypes")
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.SPACE){
					boolean value = columnViewer.getChecked(   ((IStructuredSelection)columnViewer.getSelection()).getFirstElement() );
					for (Iterator iterator = ((IStructuredSelection)columnViewer.getSelection()).iterator(); iterator.hasNext();) {
						Object tp = (Object) iterator.next();
						columnViewer.setChecked(tp, !value);
					}
					e.doit = false;
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});
		
	}

	protected void updateColumnTableState() {
		boolean enabled = btnShowDataColumnsOnly == null || btnShowDataColumnsOnly.isDisposed() || !btnShowDataColumnsOnly.getSelection();
		columnViewer.getTable().setEnabled(enabled);
		selectAll.setEnabled(enabled);
		deselectAll.setEnabled(enabled);
	}
	
	/**
	 * Called when save is selected on the dialog;
	 * allows query properties to be updated.
	 */
	@Override
	public String save(Query query, Session session){
		@SuppressWarnings("unchecked")
		List<QueryColumn> cols = (List<QueryColumn>) columnViewer.getInput();
		for (QueryColumn col : cols){
			col.setVisible( columnViewer.getChecked(col) );
		}
		persistShowDataColumnsOption(query, session);
		((SimpleQuery) query).updateVisibleColumns(cols);
		return null;
	}
	
	protected void persistShowDataColumnsOption(Query query, Session session) {
		if (btnShowDataColumnsOnly != null && query instanceof IColumnAutoConfigQuery) {
			IColumnAutoConfigQuery q = (IColumnAutoConfigQuery) query;
			q.setShowDataColumnsOnly(btnShowDataColumnsOnly.getSelection());
		}
	}	
}
