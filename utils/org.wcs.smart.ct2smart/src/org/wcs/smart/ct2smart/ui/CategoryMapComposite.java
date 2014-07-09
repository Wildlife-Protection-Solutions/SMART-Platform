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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.ct2smart.matcher.model.Ct2Attribute;
import org.wcs.smart.ct2smart.matcher.model.Ct2AttributeType;
import org.wcs.smart.ct2smart.matcher.model.Ct2Smart;
import org.wcs.smart.ct2smart.matcher.model.CtCategory;
import org.wcs.smart.ct2smart.matcher.model.CtCategoryMap;
import org.wcs.smart.ct2smart.ui.support.SmartCategoryEditingSupport;
import org.wcs.smart.ct2smart.ui.support.SmartCategoryLabelProvider;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class CategoryMapComposite extends Composite implements ILanguageChangedListener {

	private TableViewer viewer;
	private DataModelLookup lookup;
	private SmartCategoryLabelProvider catLabelProvider;
	
	private List<Ct2Attribute> columns;
	
	public CategoryMapComposite(Composite parent, DataModelLookup lookup) {
		super(parent, SWT.NONE);
		this.lookup = lookup;
		catLabelProvider = new SmartCategoryLabelProvider(lookup);
		
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

		final Table table = viewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true); 

		viewer.setContentProvider(new ArrayContentProvider());
		GridData tgridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		tgridData.heightHint = 150;
		viewer.getControl().setLayoutData(tgridData);
		
	}

	public void setInput(Ct2Smart ct2Smart) {
		List<Ct2Attribute> items = new ArrayList<Ct2Attribute>();
		for (Ct2Attribute a : ct2Smart.getCt2Attribute()) {
			if (Ct2AttributeType.CATEGORY.equals(a.getType()))
				items.add(a);
		}
		
		if (!isEqual(columns, items)) {
			columns = items;
			rebuildColumns();
		}
		viewer.setInput(ct2Smart.getCtCategory());
	}

	public void rebuildColumns() {
		viewer.getTable().setRedraw(false);
		for (TableColumn column : viewer.getTable().getColumns()) {
			column.dispose();
		}
		
		for (Ct2Attribute a : columns) {
			TableViewerColumn tCol = createTableViewerColumn(a.getN(), 200);
			tCol.setLabelProvider(new CtCategoryLabelProvider(a));
		}
		
		TableViewerColumn catCol = createTableViewerColumn("SMART Category", 250);
		catCol.setLabelProvider(catLabelProvider);
		catCol.setEditingSupport(new SmartCategoryEditingSupport(viewer, lookup, catLabelProvider));
		
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

	private boolean isEqual(List<Ct2Attribute> source, List<Ct2Attribute> dest) {
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
	
	private class CtCategoryLabelProvider extends ColumnLabelProvider {
		private Ct2Attribute a;
		
		public CtCategoryLabelProvider(Ct2Attribute a) {
			this.a = a;
		}
		
		@Override
		public String getText(Object element) {
			if (element instanceof CtCategory) {
				CtCategory c = (CtCategory) element;
				for (CtCategoryMap cmap : c.getCtCategoryMap()) {
					if (a.getI().equals(cmap.getAi()))
						return cmap.getVn();
				}
				return "<null>";
			}
			return super.getText(element);
		}
		
	}
}
