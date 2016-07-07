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
package org.wcs.smart.query.compound.ui;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.ILayerListener;
import org.locationtech.udig.project.LayerEvent;
import org.locationtech.udig.project.LayerEvent.EventType;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.MemoryQueryResult;
import org.wcs.smart.query.common.engine.QueryExecutor;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IMappableQueryType;
import org.wcs.smart.query.model.QueryStyleParser;
import org.wcs.smart.query.model.StyledQuery;
import org.wcs.smart.udig.style.StyleManager;

/**
 * Job for executing a query from a compound query and adding
 * the results to the compound query map.
 * 
 * @author Emily
 *
 */
public class RunCompoundQueryLayerJob extends Job{
	
	private QueryItem item;
	private CompoundQueryEditor mapEditor;
	private MapLayerTracker tracker;
	
	private ILayerListener styleListener = new ILayerListener() {
		
		@Override
		public void refresh(LayerEvent event) {
			if (event.getType() == EventType.STYLE){
				try{
					updateStyle(event.getSource());
				}catch (Exception ex){
					QueryPlugIn.log("Error setting query layer style. " + ex.getMessage(), ex); //$NON-NLS-1$
				}
			}
		}
		
		private void updateStyle(ILayer layer) throws Exception{
			
			String dataType = layer.getGeoResource().getIdentifier().getRef();
			if (dataType == null){
				dataType = layer.getGeoResource().getID().toString();
			}
			
			String currentStyle = item.getCompoundMapQueryLayer().getQueryStyle();
			java.util.Map<String, StyleBlackboard> queryStyles = null;
			if (currentStyle != null){
				queryStyles = StyleManager.INSTANCE.fromStringMap(currentStyle);
			}else{
				queryStyles = new HashMap<String, StyleBlackboard>();
			}
			queryStyles.put(dataType, (StyleBlackboard) layer.getStyleBlackboard());
			item.getCompoundMapQueryLayer().setQueryStyle(StyleManager.INSTANCE.asString(queryStyles));
			
			mapEditor.getSite().getShell().getDisplay().syncExec(new Runnable(){
				@Override
				public void run() {
					mapEditor.setDirty(true);
				}});
		}
	};
	
	public RunCompoundQueryLayerJob(QueryItem item, CompoundQueryEditor mapEditor, MapLayerTracker tracker){
		super(MessageFormat.format(Messages.CompoundQueryLayerJob_JobName, item.getQueryName()));
		this.item = item;
		this.tracker = tracker;
		this.mapEditor = mapEditor;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		Session s = HibernateManager.openSession();
		try{
			item.getQuery().setDateFilter(item.getDateFilter());
			ProgressMonitorWrapper wrapper = new ProgressMonitorWrapper(monitor, item.getProgressBar());
			IQueryResult results = QueryExecutor.INSTANCE.executeQuery(item.getQuery(), s, wrapper);
			item.getQuery().setCachedResults(results);
			
			if (results instanceof IPagedQueryResultSet){
				item.setTotalCount(((IPagedQueryResultSet) results).getItemCount());
			}else if (results instanceof MemoryQueryResult<?>){
				item.setTotalCount(((MemoryQueryResult<?>)results).getData().size());
			}else{
				item.setTotalCount(-1);
			}
			item.setStatus(QueryItem.Status.DONE);
			
		}catch(Exception ex){
			item.setStatus(QueryItem.Status.ERROR);
			item.setErrorMessage(ex.getMessage());
			QueryPlugIn.log(ex.getMessage(), ex);
			
		}finally{
			s.close();
		}
		
		try{		
			if (item.getQueryType() instanceof IMappableQueryType){
				//always create new service as query object will have changed
				IService qService = (IService)((IMappableQueryType)item.getQueryType()).createQueryService(item.getQuery(), mapEditor);
				tracker.addService((IQueryService) qService);
				addLayers(qService, tracker, monitor);
			}
		}catch (Exception ex){
			QueryPlugIn.displayLog(MessageFormat.format(Messages.RunCompoundQueryLayerJob_MapLayerError,  item.getQueryName()), ex);
		}
		
		Display.getDefault().asyncExec(new Runnable(){
			@Override
			public void run() {
				mapEditor.refreshQueryTable();
				mapEditor.getMap().getRenderManager().refresh(null);
			}
			
		});
		return Status.OK_STATUS;
	}
	
	private void addLayers(IService service, MapLayerTracker tracker, IProgressMonitor monitor) throws IOException{
		List<IGeoResource> layers = (List<IGeoResource>) service.resources(monitor);
		
		AddLayersCommand command = new AddLayersCommand(layers) {
			@Override
			public void run(IProgressMonitor monitor) throws Exception {
				super.run(monitor);
				for (ILayer layer : getLayers()){
					((Layer)layer).setName(item.getQueryName() + " (" + layer.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (item.getQuery() instanceof StyledQuery){
					String styleString = item.getCompoundMapQueryLayer().getQueryStyle();
					if (styleString == null){
						styleString = ((StyledQuery)item.getQuery()).getStyle();
					}
					if (styleString != null){
						for (ILayer layer : getLayers()){
							try{
								String dataType = layer.getGeoResource().getIdentifier().getRef();
								if (dataType == null){
									dataType = layer.getGeoResource().getID().toString();
								}
								QueryStyleParser.INSTANCE.applyStyle(styleString, dataType, (StyleBlackboard) layer.getStyleBlackboard());
								//do this to ensure the correct events are fired
								((Layer)layer).setStyleBlackboard((StyleBlackboard)layer.getStyleBlackboard());
							}catch (Exception ex){
								QueryPlugIn.log(ex.getMessage(), ex);
							}
						}
					}
					
					//add style listeners
					for (final ILayer layer : getLayers()){
						layer.addListener(styleListener);
						tracker.addLayer(layer);
					}
				}
			}
		};
		
		Map map = mapEditor.getMap();
		if (map == null || 
			map.getRenderManager() == null || 
			map.getRenderManagerInternal().isDisposed()) return;
		
		map.sendCommandASync(command);
	}

}
