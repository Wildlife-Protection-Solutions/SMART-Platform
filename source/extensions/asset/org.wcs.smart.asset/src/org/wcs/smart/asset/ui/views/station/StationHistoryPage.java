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
package org.wcs.smart.asset.ui.views.station;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetUtils;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.ui.handler.OpenAssetHandler;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Station editor history page
 * 
 * @author Emily
 *
 */
public class StationHistoryPage {

	private TableViewer tblDeployments;
	
	public StationHistoryPage(StationEditor editor) {

	}
	
	public void createControl(Composite parent, FormToolkit toolkit) {
		toolkit.createLabel(parent, "All Asset Deployments at this Station");
		tblDeployments = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
		tblDeployments.getTable().setHeaderVisible(true);
		tblDeployments.getTable().setLinesVisible(true);
		tblDeployments.setContentProvider(ArrayContentProvider.getInstance());
		
		for (Column c : Column.values()) {
			createColumn(c);
		}
		tblDeployments.setInput(new String[] {DialogConstants.LOADING_TEXT});
		tblDeployments.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblDeployments.addDoubleClickListener(e->gotoAsset());
		
		Menu mnu = new Menu(tblDeployments.getTable());
		
		MenuItem mnuOpenAsset = new MenuItem(mnu, SWT.PUSH);
		mnuOpenAsset.setText("Goto Asset...");
		mnuOpenAsset.addListener(SWT.Selection, e->gotoAsset());
		
		mnu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				mnuOpenAsset.setEnabled(!tblDeployments.getSelection().isEmpty());
			}
			
			@Override
			public void menuHidden(MenuEvent e) { }
		});
		
		tblDeployments.getTable().setMenu(mnu);
		
	}
	
	private void gotoAsset() {
		Object item = ((IStructuredSelection)tblDeployments.getSelection()).getFirstElement();
		if (item instanceof AssetDeployment) {
			(new OpenAssetHandler()).openAsset(((AssetDeployment) item).getAsset());
		}
	}
	
	private void createColumn(Column c) {
		TableViewerColumn column = new TableViewerColumn(tblDeployments, SWT.NONE);
		column.getColumn().setText(c.guiName);
		column.getColumn().setWidth(150);
		column.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof AssetDeployment) return c.getValue((AssetDeployment) element);
				return super.getText(element);
			}
		});
	}
		
	public void initialize(AssetStation station) {
		Job j = new Job("load history records") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				AssetStation thisStation = station;
				
				List<AssetDeployment> deployments = new ArrayList<>();
				
				try(Session s = HibernateManager.openSession()){
					deployments.addAll(
							s.createQuery("SELECT a FROM AssetDeployment a join a.stationLocation b WHERE b.station = :station", AssetDeployment.class)
							.setParameter("station", thisStation)
							.list());
					deployments.forEach(d->{
						d.getAsset().getAssetType().getName();
						d.getStationLocation().getStation().getId();
					});
				}
				deployments.sort((a,b)->{
					if (a.getEndDate() == null && b.getEndDate() == null) return b.getStartDate().compareTo(a.getStartDate());
					if (a.getEndDate() == null) return -1;
					if (b.getEndDate() == null) return 1;
					return b.getEndDate().compareTo(a.getStartDate());
				});
				Display.getDefault().syncExec(()->{
					tblDeployments.setInput(deployments);
				});
				return Status.OK_STATUS;
			}
		};
		j.schedule();
	}
	
	private enum Column{
		ASSET_TYPE("Asset Type"),
		ASSET_ID("Asset ID"),
		LOCATION("Location"),
		START_DATE("Start Date"),
		END_DATE("End Date"),
		TOTAL_TIME("Total Time");
		
		public String guiName;
		
		private Column(String colName) {
			this.guiName = colName;
		}
		
		public String getValue(AssetDeployment record) {
			switch(this) {
			case ASSET_TYPE: return record.getAsset().getAssetType().getName();
			case ASSET_ID: return record.getAsset().getId();
			case LOCATION: return record.getStationLocation().getId();
			case START_DATE: return DateFormat.getDateTimeInstance().format(record.getStartDate());
			case END_DATE: return record.getEndDate() == null ? "Current" : DateFormat.getDateTimeInstance().format(record.getEndDate());
			case TOTAL_TIME:
				long start = record.getStartDate().getTime();
				long end = (new Date()).getTime();
				if (record.getEndDate() != null) end = record.getEndDate().getTime();
				return AssetUtils.formatTime( (end-start)/1000.0);
			}
			return "";
		}
	}
}
