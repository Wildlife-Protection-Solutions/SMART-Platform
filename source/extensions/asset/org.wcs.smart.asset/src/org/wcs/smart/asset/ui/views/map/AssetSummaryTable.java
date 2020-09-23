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

import java.text.Collator;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetCoreLabelProvider;
import org.wcs.smart.asset.AssetUtils;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttributeValue;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetDeploymentAttributeValue;
import org.wcs.smart.asset.model.AssetTypeAttribute;
import org.wcs.smart.asset.model.AssetTypeDeploymentAttribute;
import org.wcs.smart.asset.ui.AssetTypeLabelProvider;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Asset summary table in overview map
 * 
 * @author Emily
 *
 */
public class AssetSummaryTable {

	private enum Column {
		ASSET_ID(Messages.AssetSummaryTable_IDColumnName),
		ASSET_STATUS(Messages.AssetSummaryTable_StatusColumnName),
		ASSET_TYPE(Messages.AssetSummaryTable_TypeColumnName),
		DEP_START(Messages.AssetSummaryTable_StartColumnName),
		DEP_END(Messages.AssetSummaryTable_EndColumnName),
		DEP_ACTIVE_TIME(Messages.AssetSummaryTable_ActiveTimeColumnName),
		DEP_TIME(Messages.AssetSummaryTable_TotalTimeColumnName);
				
		String label;
		
		Column(String l) {
			this.label = l;
		}
	}
	
	private TableViewer assetTable;
	private AssetTypeLabelProvider typeProvider = new AssetTypeLabelProvider();
	
	public AssetSummaryTable(Composite parent) {
		assetTable = new TableViewer(parent, SWT.FULL_SELECTION);
		assetTable.setContentProvider(ArrayContentProvider.getInstance());
		assetTable.getTable().setHeaderVisible(true);
		assetTable.getTable().setLinesVisible(true);
		assetTable.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		assetTable.getControl().addDisposeListener(e->typeProvider.dispose());
		
		assetTable.setComparator(new AssetViewerComparator());
	}
	
	public TableViewer getViewer() {
		return this.assetTable;
	}
	
	public void configureTable(Session session) {
		
		List<AssetTypeDeploymentAttribute> dattributes = QueryFactory.buildQuery(session,  AssetTypeDeploymentAttribute.class,  
				new Object[] {"id.assetType.conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
		
		List<AssetTypeAttribute> aattributes = QueryFactory.buildQuery(session,  AssetTypeAttribute.class,  
				new Object[] {"id.assetType.conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
		
		List<TableColumn> columns = new ArrayList<>();
		columns.add(new TableColumn(Column.ASSET_STATUS));
		columns.add(new TableColumn(Column.ASSET_ID));
		columns.add(new TableColumn(Column.ASSET_TYPE));
		columns.add(new TableColumn(Column.DEP_START));
		columns.add(new TableColumn(Column.DEP_END));
		columns.add(new TableColumn(Column.DEP_ACTIVE_TIME));
		columns.add(new TableColumn(Column.DEP_TIME));
		
		HashSet<AssetAttribute> atts = new HashSet<>();
		for (AssetTypeDeploymentAttribute d : dattributes) {
			if (!atts.contains(d.getAttribute())) {
				columns.add(new TableColumn(d));
				atts.add(d.getAttribute());
			}
		}
		
		atts.clear();
		for (AssetTypeAttribute d : aattributes) {
			if (!atts.contains(d.getAttribute())) {
				columns.add(new TableColumn(d));
				atts.add(d.getAttribute());
			}
		}
		List<Asset> assets = QueryFactory.buildQuery(session,  Asset.class, 
				new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
				new Object[] {"isRetired", false}).list(); //$NON-NLS-1$
		final List<DataWrapper> data = new ArrayList<>();
		for (Asset a : assets) {
			DataWrapper dw = new DataWrapper(a);
			a.computeStatus(session);
			dw.deployment = a.findActiveDeployment(session);
			data.add(dw);
		}
		
		Display.getDefault().syncExec(()->{
			for (org.eclipse.swt.widgets.TableColumn cc : assetTable.getTable().getColumns()) {
				cc.dispose();
			}
			
			for (TableColumn c : columns) {
				TableViewerColumn tc = new TableViewerColumn(assetTable, SWT.NONE);
				tc.setLabelProvider(new ColumnLabelProvider() {
					public String getText(Object element) {
						if (element instanceof DataWrapper) {
							return c.getLabel((DataWrapper) element);
						}
						return super.getText(element);
					}
					public Image getImage(Object element) {
						if (element instanceof DataWrapper) {
							return c.getImage((DataWrapper) element);
						}
						return null;
					}
				});				
				tc.getColumn().setText(c.getName());
				tc.getColumn().pack();
				
				tc.getColumn().addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						AssetViewerComparator vc = (AssetViewerComparator)assetTable.getComparator(); 
						vc.setColumn(c);
						assetTable.getTable().setSortDirection(vc.getSortDirection() > 0 ? SWT.UP : SWT.DOWN);
						assetTable.getTable().setSortColumn(tc.getColumn());
						assetTable.refresh();
						
					}
				});
			}
			
			assetTable.setInput(data);
			
			//default sort on status
			AssetViewerComparator vc = (AssetViewerComparator)assetTable.getComparator(); 
			vc.setColumn(columns.get(0));
			assetTable.getTable().setSortDirection(vc.getSortDirection() > 0 ? SWT.UP : SWT.DOWN);
			assetTable.getTable().setSortColumn(assetTable.getTable().getColumn(0));
			assetTable.refresh();
			
			assetTable.getTable().getColumn(0).setWidth(26);
			for (int i = 1; i <= 6; i ++) {
				assetTable.getTable().getColumn(i).pack();
			}
		});
	}
	
	private class TableColumn {
		private Column c;
		private AssetTypeDeploymentAttribute d;
		private AssetTypeAttribute a;
		
		public TableColumn(Column c) {
			this.c = c;
		}
		public TableColumn(AssetTypeDeploymentAttribute d) {
			this.d = d;
		}
		public TableColumn(AssetTypeAttribute a) {
			this.a = a;
		}
		public String getName() {
			if (c != null) return c.label;
			if (d != null) return d.getAttribute().getName();
			if (a != null) return a.getAttribute().getName() + Messages.AssetSummaryTable_AssetAttributePostFix;
			return ""; //$NON-NLS-1$
		}
		public Image getImage(DataWrapper dw) {
			if (c != null) {
				if (c == Column.ASSET_STATUS) return AssetCoreLabelProvider.getStatusImage(dw.asset.getCachedStatus());
				if (c == Column.ASSET_TYPE) return typeProvider.getImage(dw.asset.getAssetType());
			}
			return null;
			
		}
		public String getLabel(DataWrapper dw) {
			if (c != null) {
				if (c == Column.ASSET_ID) return dw.asset.getId();
				if (c == Column.ASSET_STATUS) return dw.asset.getCachedStatus().getGuiName(Locale.getDefault());
				if (c == Column.ASSET_TYPE) return dw.asset.getAssetType().getName();
				if (c == Column.DEP_END) {
					if (dw.deployment == null) return ""; //$NON-NLS-1$
					if (dw.deployment.getEndDate() == null) return ""; //$NON-NLS-1$
					return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(dw.deployment.getEndDate());
					
				}
				if (c == Column.DEP_START) {
					if (dw.deployment == null) return ""; //$NON-NLS-1$
					return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(dw.deployment.getStartDate());
					
				}
				if (c == Column.DEP_TIME) {
					if (dw.deployment == null) return ""; //$NON-NLS-1$
					return AssetUtils.formatTime(dw.deployment.getTimeOutInSeconds());
				}
				if (c == Column.DEP_ACTIVE_TIME) {
					if (dw.deployment == null) return ""; //$NON-NLS-1$
					return AssetUtils.formatTime(dw.deployment.getActiveTimeOutInSeconds());
				}
			}
			if (d != null) {
				if (dw.deployment == null) return ""; //$NON-NLS-1$
				AssetDeploymentAttributeValue value = null;
				for (AssetDeploymentAttributeValue v : dw.deployment.getAttributeValues()) {
					if (v.getAttribute().equals(d.getAttribute())){
						value = v;
						break;
					}
				}
				if (value == null) return ""; //$NON-NLS-1$
				return value.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS);
			}
			if (a != null) {
				AssetAttributeValue value = null;
				for (AssetAttributeValue v : dw.asset.getAttributeValues()) {
					if (v.getAttribute().equals(a.getAttribute())){
						value = v;
						break;
					}
				}
				if (value == null) return ""; //$NON-NLS-1$
				return value.getAttributeValueAsString(Locale.getDefault(), SmartDB.DATABASE_CRS);
			}
			return ""; //$NON-NLS-1$
		}
	}
	
	private class DataWrapper {
		private Asset asset;
		private AssetDeployment deployment;
		
		public DataWrapper(Asset asset) {
			this.asset = asset;
		}
	}
	
	private class AssetViewerComparator extends ViewerComparator {
		private TableColumn column;
		private int direction = 1;

		public AssetViewerComparator() {
			this.column = null;
		}

		public int getSortDirection() {
			return direction;
		}

		public void setColumn(TableColumn column) {
			if (this.column == column) {
				direction = direction * -1;
			} else {
				this.column = column;
				direction = 1;
			}
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			if (this.column == null)
				return 0;
			DataWrapper p1 = (DataWrapper) e1;
			DataWrapper p2 = (DataWrapper) e2;
			return direction * Collator.getInstance().compare(column.getLabel(p1), column.getLabel(p2));
		}

	}
}
