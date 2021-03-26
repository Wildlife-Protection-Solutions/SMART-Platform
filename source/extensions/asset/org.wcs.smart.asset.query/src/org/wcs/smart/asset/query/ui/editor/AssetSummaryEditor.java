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
package org.wcs.smart.asset.query.ui.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.map.udig.QueryService;
import org.wcs.smart.asset.query.model.AssetQueryFactory;
import org.wcs.smart.asset.query.model.AssetSummaryQuery;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.QueryExecutor;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.common.ui.ISummaryEditor;
import org.wcs.smart.query.common.ui.SummaryEditor;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.ui.editor.IMapQueryEditor;
import org.wcs.smart.query.ui.editor.IQueryEditor;
import org.wcs.smart.query.ui.editor.QueryEditorInput;

/**
 * Editor for displaying query results. The editor includes two pages a tabular
 * results page and a map results page.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AssetSummaryEditor extends MultiPageEditorPart implements IQueryEditor, ISummaryEditor, MapPart, IMapQueryEditor{

	public static final String ID = "org.wcs.smart.asset.query.ui.SummaryEditor"; //$NON-NLS-1$

	private SummaryEditor page1 = null;
	private SummaryMapPagePart page2 = null;

	private Job runQueryJob = new Job(Messages.AssetSummaryEditor_runjobname) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			setName(Messages.AssetSummaryEditor_RunTaskName + page1.getQuery().getName());
			try {
				IProgressMonitor mymonitor = page1.getResultArea().createProgressMonitor();
				IQueryResult results = QueryExecutor.INSTANCE.executeQuery(page1.getQuery(), null, mymonitor);
				
				if (monitor.isCanceled() || mymonitor.isCanceled()){
					page1.getResultArea().updateAndShowTable(null);
					return Status.CANCEL_STATUS;
				}
				page1.getResultArea().updateAndShowTable((SummaryQueryResult)results);
				page2.refresh();
			} catch (Exception ex) {
				QueryPlugIn.displayLog(Messages.AssetSummaryEditor_ErrorLogMsg, ex);
			}
			return Status.OK_STATUS;
		}
	};
	
	@Override
	public void refreshQuery() {
		page1.refreshQuery();
		page2.refresh();
	}

	@Override
	public QueryProxy getQueryProxy() {
		return page1.getQueryProxy();
	}

	@Override
	public QueryEditorInput getInputInternal() {
		return page1.getInputInternal();
	}

	@Override
	public void validate() {
		page1.validate();
	}

	@Override
	public void reparseQuery() {
		page1.reparseQuery();
		
	}

	@Override
	public void setDirty(boolean dirty) {
		page1.setDirty(dirty);
	}

	@Override
	protected void createPages() {
		QueryEditorInput input = ((QueryEditorInput) getEditorInput());
		super.setPartName(input.getName());
		showBusy(true);
		try {
			page1 = new SummaryEditor() {
				@Override
				public Query createNewQuery() {
					return AssetQueryFactory.createSummaryQuery(input.getType().getKey());
				}
				
				@Override
				public Job getRunQueryJob() {
					return runQueryJob;
				}
			};
			
			int pageIndex = 0;
			addPage(pageIndex, page1, input);
			setPageText(pageIndex, Messages.AssetSummaryEditor_SummaryResultsTabName);
			setPageImage(pageIndex, QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.TABLE_ICON));
			
			pageIndex++;
			page2 = new SummaryMapPagePart(this);
			addPage(pageIndex, page2, input);
			setPageText(pageIndex, Messages.AssetSummaryEditor_MapTabName);
			setPageImage(pageIndex, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.MAP_ICON));
		}catch (Exception ex) {
			QueryPlugIn.log("Could not open query editor", ex); //$NON-NLS-1$
			throw new RuntimeException("Could not open query editor" + ex.getMessage(), ex); //$NON-NLS-1$
		}finally {
			showBusy(false);
		}
	}

	public void updatePartName(){
		super.setPartName(page1.getEditorInput().getName());
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		page1.doSave(monitor);
		updatePartName();
	}

	@Override
	public void doSaveAs() {
		page1.doSaveAs();
		updatePartName();
		
	}
	@Override
	public void setDateFilter(DateFilter dateFilter) {
		page1.setDateFilter(dateFilter);
	}
	
	@Override
	public boolean isSaveAsAllowed() {
		return page1.isSaveAsAllowed();
	}

	@Override
	public IQueryService createQueryService() {
		return new QueryService((AssetSummaryQuery)getQueryProxy().getQuery());
	}

	@Override
	public void showMapPage(ReferencedEnvelope env) {
		if (env != null){
			page2.setInitialZoom(env);
			getMap().sendCommandSync(new SetViewportBBoxCommand(env));
		}
		for (int i = 0; i < getPageCount(); i ++){
			if (getEditor(i) == page2){
				setActivePage(i);
				return;
			}
		}
	}

	@Override
	public Map getMap() {
		return page2.getMap();
	}

	@Override
	public void openContextMenu() {
		page2.openContextMenu();
		
	}

	@Override
	public void setFont(Control textArea) {
		page2.setFont(textArea);		
	}

	@Override
	public void setSelectionProvider(IMapEditorSelectionProvider selectionProvider) {
		page2.setSelectionProvider(selectionProvider);
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return page2.getStatusLineManager();
	}

}
