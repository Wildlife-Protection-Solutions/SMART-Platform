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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
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
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.conversion.csv.tool.CategoryMapBuilder;
import org.wcs.smart.conversion.lookup.DataModelLookup;
import org.wcs.smart.conversion.model.CategoryMap;
import org.wcs.smart.conversion.model.MappedAttribute;
import org.wcs.smart.conversion.model.MappedAttributeType;
import org.wcs.smart.conversion.model.MappedCategory;
import org.wcs.smart.conversion.model.SmartMapping;
import org.wcs.smart.conversion.ui.support.SmartCategoryEditingSupport;
import org.wcs.smart.conversion.ui.support.SmartCategoryLabelProvider;
import org.wcs.smart.conversion.util.ConnectionUtil;
import org.wcs.smart.conversion.util.Ct2AttributeTypeUtil;
import org.wcs.smart.conversion.util.FileUtil;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class CategoryMapComposite extends Composite implements ILanguageChangedListener {

	private TableViewer viewer;
	private DataModelLookup lookup;
	private SmartCategoryLabelProvider catLabelProvider;
	private CategoryMapBuilder catMapBuilder;
	
	private List<MappedAttribute> columns;
	private SmartMapping input;
	
	private CtCategoryEAComposite extraAttrCmp;

	
	public CategoryMapComposite(Composite parent, DataModelLookup lookup, SmartMapping ct2Smart) {
		super(parent, SWT.NONE);
		this.lookup = lookup;
		catLabelProvider = new SmartCategoryLabelProvider(lookup);
			
		try {
			catMapBuilder = new CategoryMapBuilder(ConnectionUtil.getConnection());
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		GridLayout gd = new GridLayout(1, false);
		gd.marginBottom = 0;
		gd.marginHeight = 0;
		gd.marginLeft = 0;
		gd.marginRight = 0;
		gd.marginTop = 0;
		gd.marginWidth = 0;
		this.setLayout(gd);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Group group = new Group(this, SWT.NONE);
		group.setText("Category mappings");
		group.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		group.setLayout(new GridLayout(1, false));
		
		viewer = new TableViewer(group, SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true); 

		viewer.setContentProvider(new ArrayContentProvider());
		GridData tgridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tgridData.heightHint = 150;
		viewer.getControl().setLayoutData(tgridData);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {		
				viewerSelectionChanged();
			}
		});

		if (ct2Smart != null) {
			columns = extractColumns(ct2Smart);
			rebuildColumns();
		}
		
		Button btnAdd = new Button(group, SWT.PUSH);
		btnAdd.setText("External Import");
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				importExternalCategories();
			}
		});
		
		extraAttrCmp = new CtCategoryEAComposite(this, lookup);
	}

	protected void viewerSelectionChanged() {
		Object obj = ((IStructuredSelection)viewer.getSelection()).getFirstElement();
		if (obj instanceof MappedCategory) {
			MappedCategory c = (MappedCategory)obj;
			extraAttrCmp.setInput(c);
			extraAttrCmp.setVisible(true);
			layout();
		}
	}

	public void setInput(SmartMapping ct2Smart) {
		input = ct2Smart;
		List<MappedAttribute> items = extractColumns(ct2Smart);
		
		if (!isEqual(columns, items) || ct2Smart.getMappedCategory().isEmpty()) {
			columns = items;
			try {
				List<MappedCategory> oldCats = new ArrayList<MappedCategory>(ct2Smart.getMappedCategory().size());
				oldCats.addAll(ct2Smart.getMappedCategory());
				ct2Smart.getMappedCategory().clear();
				List<MappedCategory> newCats = catMapBuilder.extractCategoryValues(columns);
				CategoryMatcher matcher = new CategoryMatcher();
				matcher.match(newCats, oldCats);
				ct2Smart.getMappedCategory().addAll(newCats);
			} catch (SQLException e) {
				e.printStackTrace();
			}
			rebuildColumns();
		}
		viewer.setInput(ct2Smart.getMappedCategory());
		extraAttrCmp.setVisible(false);
	}

	public List<MappedAttribute> extractColumns(SmartMapping ct2Smart) {
		List<MappedAttribute> items = new ArrayList<MappedAttribute>();
		for (MappedAttribute a : ct2Smart.getMappedAttribute()) {
			if (MappedAttributeType.CATEGORY.equals(a.getType()))
				items.add(a);
		}
		return items;
	}
	
	public void rebuildColumns() {
		viewer.getTable().setRedraw(false);
		for (TableColumn column : viewer.getTable().getColumns()) {
			column.dispose();
		}
		
		for (MappedAttribute a : columns) {
			TableViewerColumn tCol = createTableViewerColumn(Ct2AttributeTypeUtil.getN(a), 200);
			tCol.setLabelProvider(new CtCategoryLabelProvider(a));
		}
		
		TableViewerColumn catCol = createTableViewerColumn("SMART Category", 250);
		catCol.setLabelProvider(catLabelProvider);
		catCol.setEditingSupport(new SmartCategoryEditingSupport(viewer, lookup, catLabelProvider));

		TableViewerColumn iCol = createTableViewerColumn("Ignore", 100);
		iCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof MappedCategory) {
					MappedCategory v = (MappedCategory) element;
					return Boolean.TRUE.equals(v.isIgnore()) ? "Yes" : "No";
				}
				return super.getText(element);
			}
		});
		iCol.setEditingSupport(new IgnoreCategoryValueEditingSupport(viewer));
		
		viewer.getTable().setRedraw(true);
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
	
	@Override
	public void languageChanged(String langCode) {
		catLabelProvider.languageChanged(langCode);
		viewer.refresh();
	}

	private boolean isEqual(List<MappedAttribute> source, List<MappedAttribute> dest) {
		if (source == null)
			return dest == null;
		if (dest == null)
			return source == null;
		
		if (source.size() != dest.size())
			return false;
		
		for (int i = 0; i < source.size(); i++) {
			if (!source.get(i).equals(dest.get(i)))
				return false;
		}
		
		return true;
	}

	protected void importExternalCategories() {
		FileDialog dlg = new FileDialog(getShell(), SWT.OPEN);
		dlg.setFilterNames(new String[] {"XML file"});
		dlg.setFilterExtensions(new String[] {"*.xml"});
		String fn = dlg.open();
		if (fn != null) {
			try {
				SmartMapping from = FileUtil.loadSmartMapping(new File(fn));
//				input.getMappedCategory().clear();
//				List<MappedCategory> newCats = catMapBuilder.extractCategoryValues(columns);
				CategoryMatcher matcher = new CategoryMatcher();
				matcher.match(input.getMappedCategory(), from.getMappedCategory());
//				input.getMappedCategory().addAll(newCats);
				MessageDialog.openInformation(getShell(), "External import", "Matching category mappings are imported from extarnal mapping.");
			} catch (Exception e) {
				MessageDialog.openError(getShell(), "Error", "Error importing external category mapping data");
				e.printStackTrace();
			}
			rebuildColumns();
			viewer.setInput(input.getMappedCategory());
			extraAttrCmp.setVisible(false);
		}
		
	}

	private class CtCategoryLabelProvider extends ColumnLabelProvider {
		private MappedAttribute a;
		
		public CtCategoryLabelProvider(MappedAttribute a) {
			this.a = a;
		}
		
		@Override
		public String getText(Object element) {
			if (element instanceof MappedCategory) {
				MappedCategory c = (MappedCategory) element;
				for (CategoryMap cmap : c.getCategoryMap()) {
					if (a.getI().equals(cmap.getAi())) {
						return Ct2AttributeTypeUtil.getVn(cmap);
					}
				}
				return "";
			}
			return super.getText(element);
		}
		
	}
	
	private class IgnoreCategoryValueEditingSupport extends EditingSupport {

		private final String[] NO_YES_ITEMS = new String[] {"No", "Yes"};
		
		private ComboBoxCellEditor cbEditor;
		
		public IgnoreCategoryValueEditingSupport(TableViewer viewer) {
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
			if (arg0 instanceof MappedCategory) {
				MappedCategory v = (MappedCategory) arg0;
				if (Boolean.TRUE.equals(v.isIgnore()))
					return 1;
			}
			return 0;
		}

		@Override
		protected void setValue(Object arg0, Object arg1) {
			if (arg0 instanceof MappedCategory && arg1 instanceof Integer) {
				MappedCategory a = (MappedCategory) arg0;
				Integer i = (Integer) arg1;
				a.setIgnore(i.equals(1));
				getViewer().refresh();
			}
		}
		
	}
	
}
