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
package org.wcs.smart.conversion.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.conversion.lookup.DataModelLookup;
import org.wcs.smart.conversion.model.MappedAttribute;
import org.wcs.smart.conversion.model.MappedAttributeValue;
import org.wcs.smart.conversion.ui.support.SmartAttributeValueEditingSupport;
import org.wcs.smart.conversion.ui.support.SmartAttributeValueLabelProvider;
import org.wcs.smart.conversion.util.Ct2AttributeTypeUtil;

import au.com.bytecode.opencsv.CSVWriter;

public class ValueMapComposite extends Composite implements ILanguageChangedListener {
	
	private static final Logger logger = LogManager.getLogger(ValueMapComposite.class); 

	private TableViewer viewer;
	private DataModelLookup lookup;
	private MappedAttribute attribute;
	
	private Ct2AttributeValueLabelProvider valLabelProvider;

	private Connection connection;

	public ValueMapComposite(Composite parent, DataModelLookup lookup, Connection c) {
		super(parent, SWT.NONE);
		this.lookup = lookup;
		this.connection = c;
		
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
		tgridData.heightHint = 150;
		viewer.getControl().setLayoutData(tgridData);

		Composite btnCmp = new Composite(this, SWT.NONE);
		btnCmp.setLayout(new GridLayout(1, false));
		btnCmp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		Button btnCsvExport = new Button(btnCmp, SWT.PUSH);
		btnCsvExport.setText("Csv Export");
		btnCsvExport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				exportCsv();
			}
		});
		
		Button btnReload = new Button(btnCmp, SWT.PUSH);
		btnReload.setText("Reload Values");
		btnReload.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				reloadValues();
			}
		});
	}

	protected void exportCsv() {
		FileDialog dlg = new FileDialog(getShell(), SWT.SAVE);
		dlg.setFilterNames(new String[] {"CSV file"});
		dlg.setFilterExtensions(new String[] {"*.csv"}); //$NON-NLS-1$
		String fn = dlg.open();
		if (fn != null) {
			if (!fn.endsWith(".csv")) { //$NON-NLS-1$
				fn += ".csv"; //$NON-NLS-1$
			}
			CSVWriter writer = null;
			File file = new File(fn);
			try {
				writer = new CSVWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"), ',', '"',System.getProperty("line.separator")); //$NON-NLS-1$ //$NON-NLS-2$

				// WriteHeaders
				String[] headerColumns = new String[] {"Datafile Value", "SMART Value", "SMART key", "Ignore"};
				writer.writeNext(headerColumns);

				//for each row write one record
				for (MappedAttributeValue attr : attribute.getMappedAttributeValue()) {
					String csvout[] = new String[headerColumns.length];
					csvout[0] = Ct2AttributeTypeUtil.getN(attr);
					csvout[1] = valLabelProvider.getText(attr);
					csvout[2] = attr.getMapTo();
					csvout[3] = String.valueOf(Boolean.TRUE.equals(attr.isIgnore()));
					writer.writeNext(csvout);
					
				}
				writer.close();
				MessageDialog.openInformation(getShell(), "CSV Export", "CSV Export completed.");
			} catch (IOException ex) {
				try {
					if (writer != null) {
						writer.close();
					}
				} catch (IOException e) {
					logger.error("Error closing writer.", e); //$NON-NLS-1$
				}
				logger.error("Error exporting CSV.", ex); //$NON-NLS-1$
			}
		}
	}

	protected void reloadValues() {
		boolean reloaded = doValueReloading(false);
		if (reloaded) {
			MessageDialog.openInformation(getShell(), "Reload values", "Values sucessfully reloaded.");
		} else {
			MessageDialog.openError(getShell(), "Reload values", "Failed to reloaded attribute values. See log for details.");
		}
		setInput(attribute);
	}

	protected boolean doValueReloading(boolean preserveMapping) {
		if (attribute == null) {
			return false;
		}
		List<MappedAttributeValue> valuesBackup = new ArrayList<MappedAttributeValue>(attribute.getMappedAttributeValue());
		attribute.getMappedAttributeValue().clear();
		try {
			String sql = "select id, n from CSV_TO_SMART.ATTRIBUTES where n = '" + attribute.getI() + "'";  //$NON-NLS-1$ //$NON-NLS-2$
			logger.debug("run SQL:" + sql); //$NON-NLS-1$
			ResultSet attrRs = connection.createStatement().executeQuery(sql);
			if (attrRs.next()) {
				String valuesSql = "select distinct a"+attrRs.getString(1)+" from CSV_TO_SMART.CSV"; //$NON-NLS-1$ //$NON-NLS-2$
				logger.debug("run SQL:" + valuesSql); //$NON-NLS-1$
				ResultSet valRs = connection.createStatement().executeQuery(valuesSql);
				while (valRs.next()) {
					MappedAttributeValue ctAttrValue = new MappedAttributeValue();
					ctAttrValue.setI(valRs.getString(1));
//					ctAttrValue.setN(s);
					ctAttrValue.setMapTo(""); //$NON-NLS-1$
					attribute.getMappedAttributeValue().add(ctAttrValue);
				}
				if (preserveMapping) {
					preserveMapping(attribute.getMappedAttributeValue(), valuesBackup);
				}
				return true;
			}
		} catch (SQLException e) {
			attribute.getMappedAttributeValue().clear();
			attribute.getMappedAttributeValue().addAll(valuesBackup);
			logger.error("Failed to reloaded attribute values.", e); //$NON-NLS-1$
		}
		return false;
	}
	
	private void preserveMapping(List<MappedAttributeValue> target, List<MappedAttributeValue> source) {
		Map<String, MappedAttributeValue> srcMap = new HashMap<String, MappedAttributeValue>();
		for (MappedAttributeValue v : source) {
			srcMap.put(v.getI(), v);
		}
		for (MappedAttributeValue v : target) {
			MappedAttributeValue match = srcMap.get(v.getI());
			if (match != null) {
				v.setIgnore(match.isIgnore());
				v.setMapTo(match.getMapTo());
//				v.setN(match.getN());
			}
		}
	}

	public void setInput(MappedAttribute attribute) {
		this.attribute = attribute;
		doValueReloading(true);
		viewer.setInput(attribute.getMappedAttributeValue());
	}
	
	private void createColumns() {
		TableViewerColumn ctCol = createTableViewerColumn("Datafile Value", 250);
		ColumnLabelProvider ctLabelProvider = new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof MappedAttributeValue) {
					MappedAttributeValue v = (MappedAttributeValue) element;
					return Ct2AttributeTypeUtil.getN(v);
				}
				return super.getText(element);
			}
		};
		ctCol.setLabelProvider(ctLabelProvider);
		
		TableViewerColumn vCol = createTableViewerColumn("SMART Value", 250);
		valLabelProvider = new Ct2AttributeValueLabelProvider(lookup);
		vCol.setLabelProvider(valLabelProvider);
		vCol.setEditingSupport(new Ct2AttributeValueEditingSupport(viewer, lookup, valLabelProvider));

		TableViewerColumn iCol = createTableViewerColumn("Ignore", 100);
		iCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof MappedAttributeValue) {
					MappedAttributeValue v = (MappedAttributeValue) element;
					return Boolean.TRUE.equals(v.isIgnore()) ? "Yes" : "No";
				}
				return super.getText(element);
			}
		});
		iCol.setEditingSupport(new IgnoreValueEditingSupport(viewer));
		
	}

	private TableViewerColumn createTableViewerColumn(String title, int bound) {
		final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		final TableColumn column = viewerColumn.getColumn();
		column.setText(title);
		column.setWidth(bound);
		column.setResizable(true);
		column.setMoveable(true);
		return viewerColumn;
	}
	
	private class Ct2AttributeValueLabelProvider extends SmartAttributeValueLabelProvider {

		public Ct2AttributeValueLabelProvider(DataModelLookup lookup) {
			super(lookup);
		}

		@Override
		public String getText(Object element) {
			if (element instanceof MappedAttributeValue) {
				MappedAttributeValue v = (MappedAttributeValue) element;
				return getNameForKey(attribute.getMapTo(), v.getMapTo());
			}
			return super.getText(element);
		}
		
	}
	
	@Override
	public void languageChanged(String langCode) {
		valLabelProvider.languageChanged(langCode);
		viewer.refresh();
	}

	private class Ct2AttributeValueEditingSupport extends SmartAttributeValueEditingSupport {
		
		private String attrMapTo;

		public Ct2AttributeValueEditingSupport(TableViewer viewer, DataModelLookup lookup, SmartAttributeValueLabelProvider labelProvider) {
			super(viewer, lookup, labelProvider);
		}
		
		@Override
		protected CellEditor getCellEditor(Object arg0) {
			if (arg0 instanceof MappedAttributeValue) {
				attrMapTo = attribute.getMapTo(); //we need to use this one as on setInput() attribute may change and editor value may be updated afterwards
				return getAttributeEditor(attrMapTo);
			}
			return super.getCellEditor(arg0);
		}
		
		@Override
		protected Object getValue(Object arg0) {
			if (arg0 instanceof MappedAttributeValue) {
				MappedAttributeValue v = (MappedAttributeValue) arg0;
				return getEditorValue(attrMapTo, v.getMapTo());
			}
			return super.getValue(arg0);
		}
		
		@Override
		protected void setValue(Object arg0, Object arg1) {
			if (arg0 instanceof MappedAttributeValue) {
				MappedAttributeValue a = (MappedAttributeValue) arg0;
				a.setMapTo(getModelValue(attrMapTo, arg1));
				getViewer().refresh();
			} else {
				super.setValue(arg0, arg1);
			}
		}
	}

	private class IgnoreValueEditingSupport extends EditingSupport {

		private final String[] NO_YES_ITEMS = new String[] {"No", "Yes"};
		
		private ComboBoxCellEditor cbEditor;
		
		public IgnoreValueEditingSupport(TableViewer viewer) {
			super(viewer);
			Table table = viewer.getTable();
			cbEditor = new ComboBoxCellEditor(table, new String[0], SWT.DROP_DOWN | SWT.READ_ONLY);

		}

		@Override
		protected boolean canEdit(Object arg0) {
			return true;
		}

		@Override
		protected CellEditor getCellEditor(Object arg0) {
		cbEditor.setItems(NO_YES_ITEMS);
		return cbEditor ;
		}

		@Override
		protected Object getValue(Object arg0) {
			if (arg0 instanceof MappedAttributeValue) {
				MappedAttributeValue v = (MappedAttributeValue) arg0;
				if (Boolean.TRUE.equals(v.isIgnore()))
					return 1;
			}
			return 0;
		}

		@Override
		protected void setValue(Object arg0, Object arg1) {
			if (arg0 instanceof MappedAttributeValue && arg1 instanceof Integer) {
				MappedAttributeValue a = (MappedAttributeValue) arg0;
				Integer i = (Integer) arg1;
				a.setIgnore(i.equals(1));
				getViewer().refresh();
			}
		}
		
	}
	
}
