package org.wcs.smart.ct2smart.ui;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeValue;
import org.wcs.smart.ct2smart.ui.support.SmartAttributeValueEditingSupport;
import org.wcs.smart.ct2smart.ui.support.SmartAttributeValueLabelProvider;

public class ValueMapComposite extends Composite implements ILanguageChangedListener {

	private TableViewer viewer;
	private DataModelLookup lookup;
	private Ct2Attribute attribute;
	
	private Ct2AttributeValueLabelProvider valLabelProvider;

	public ValueMapComposite(Composite parent, DataModelLookup lookup) {
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
		tgridData.heightHint = 150;
		viewer.getControl().setLayoutData(tgridData);
		
	}

	public void setInput(Ct2Attribute attribute) {
		this.attribute = attribute;
		viewer.setInput(attribute.getCt2AttributeValue());
	}
	
	private void createColumns() {
		TableViewerColumn ctCol = createTableViewerColumn("CyberTracker Value", 250);
		ColumnLabelProvider ctLabelProvider = new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Ct2AttributeValue) {
					Ct2AttributeValue v = (Ct2AttributeValue) element;
					return v.getN();
				}
				return super.getText(element);
			}
		};
		ctCol.setLabelProvider(ctLabelProvider);
		
		TableViewerColumn vCol = createTableViewerColumn("SMART Value", 250);
		valLabelProvider = new Ct2AttributeValueLabelProvider(lookup);
		vCol.setLabelProvider(valLabelProvider);
		vCol.setEditingSupport(new Ct2AttributeValueEditingSupport(viewer, lookup, valLabelProvider));

		TableViewerColumn iCol = createTableViewerColumn("Ignore", 250);
		iCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Ct2AttributeValue) {
					Ct2AttributeValue v = (Ct2AttributeValue) element;
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
			if (element instanceof Ct2AttributeValue) {
				Ct2AttributeValue v = (Ct2AttributeValue) element;
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
			if (arg0 instanceof Ct2AttributeValue) {
				attrMapTo = attribute.getMapTo(); //we need to use this one as on setInput() attribute may change and editor value may be updated afterwards
				return getAttributeEditor(attrMapTo);
			}
			return super.getCellEditor(arg0);
		}
		
		@Override
		protected Object getValue(Object arg0) {
			if (arg0 instanceof Ct2AttributeValue) {
				Ct2AttributeValue v = (Ct2AttributeValue) arg0;
				return getEditorValue(attrMapTo, v.getMapTo());
			}
			return super.getValue(arg0);
		}
		
		@Override
		protected void setValue(Object arg0, Object arg1) {
			if (arg0 instanceof Ct2AttributeValue) {
				Ct2AttributeValue a = (Ct2AttributeValue) arg0;
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
			if (arg0 instanceof Ct2AttributeValue) {
				Ct2AttributeValue v = (Ct2AttributeValue) arg0;
				if (Boolean.TRUE.equals(v.isIgnore()))
					return 1;
			}
			return 0;
		}

		@Override
		protected void setValue(Object arg0, Object arg1) {
			if (arg0 instanceof Ct2AttributeValue && arg1 instanceof Integer) {
				Ct2AttributeValue a = (Ct2AttributeValue) arg0;
				Integer i = (Integer) arg1;
				a.setIgnore(i.equals(1));
				getViewer().refresh();
			}
		}
		
	}
	
}
