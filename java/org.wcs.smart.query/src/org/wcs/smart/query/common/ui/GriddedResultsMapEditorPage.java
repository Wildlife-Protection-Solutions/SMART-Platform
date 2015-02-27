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

package org.wcs.smart.query.common.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.ILayerListener;
import org.locationtech.udig.project.LayerEvent;
import org.locationtech.udig.project.LayerEvent.EventType;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.command.navigation.ZoomExtentCommand;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.commands.ChangeCRSCommand;
import org.locationtech.udig.project.internal.commands.DeleteLayerCommand;
import org.locationtech.udig.project.internal.commands.DeleteLayersCommand;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.model.udig.RasterService;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.GridResultItem;
import org.wcs.smart.query.model.StyledQuery;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.util.JobUtil;

/**
 * Presents the map with the GridCoverage which result of queryS
 * 
 * @author Mauricio Pazos
 */
public class GriddedResultsMapEditorPage extends SmartMapEditorPart {

	private static final String RESOURCE_KEY = "raster"; //$NON-NLS-1$
	
	private GriddedEditor parentEditor;
	private RasterService rasterService = null;
	private LoadDefaultLayersJob loadDefaultLayers = null;

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
		
		private void updateStyle(ILayer layer) throws IOException{
			StyledQuery sq = ((StyledQuery)parentEditor.getQueryProxy().getQuery());
			sq.updateStyle(RESOURCE_KEY, (StyleBlackboard) layer.getStyleBlackboard());
			getSite().getShell().getDisplay().syncExec(new Runnable(){
				@Override
				public void run() {
					parentEditor.setDirty(true);
				}});
		}
	};
	/*
	 * Job for adding Raster layer to map
	 */
	private Job addLayerJob = new Job(
			Messages.GriddedResultsMapEditorPage_AddLayersJobeName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {

			try {
				// retrieves the last query result
				GriddedQuery query = (GriddedQuery) parentEditor.getQuery();
				Collection<GridResultItem> queryResults = (Collection<GridResultItem>) parentEditor
						.getQuery().getCachedResults(monitor);
				if ((queryResults == null) || queryResults.isEmpty())
					return Status.OK_STATUS;

				Map map = getMap();
				if (rasterService == null) {
					String id = query.getId();
					if (id == null) {
						id = String.valueOf(System.nanoTime());
					}
					rasterService = new RasterService(query);
					rasterService.getReader(monitor); // this will create the
														// raster
					if (rasterService.getMessage() != null) {
						QueryPlugIn
								.displayLog(
										Messages.GriddedResultsMapEditorPage_ErrorCreatingRaster
												+ rasterService.getMessage()
														.getMessage(),
										rasterService.getMessage());
						return Status.OK_STATUS;
					}
				}
				List<? extends IGeoResource> rasterResourceList = rasterService
						.resources(monitor);
				assert !rasterResourceList.isEmpty();

				if (map == null)
					return Status.CANCEL_STATUS;
				updateMap(map, rasterResourceList);

			} catch (Exception e) {

				return new Status(
						IStatus.ERROR,
						Messages.GriddedResultsMapEditorPage_UnknownLabel,
						IStatus.ERROR,
						Messages.GriddedResultsMapEditorPage_LoadingErrorMessage,
						e);
			}
			return Status.OK_STATUS;
		}

		/**
		 * Adds the new raster resource to the map
		 * 
		 * @param map
		 * @param rasterResourceList
		 */
		private void updateMap(final Map map,
				final List<? extends IGeoResource> rasterResourceList) {

			List<IGeoResource> layers = new LinkedList<IGeoResource>();
			layers.addAll(rasterResourceList);

			map.getRenderManagerInternal().disableRendering();
			// add the new layers to the map
			AddLayersCommand command = new AddLayersCommand(layers){
				 public void run( IProgressMonitor monitor ) throws Exception {
					super.run(monitor);

					if (parentEditor.getQueryProxy().getQuery() instanceof StyledQuery) {
						// update layer style
						final StyledQuery sq = ((StyledQuery) parentEditor.getQueryProxy().getQuery());
						ILayer layer = getLayers().get(0);
						if (sq.getStyle() != null) {
							sq.applyStyle(RESOURCE_KEY, (StyleBlackboard) layer.getStyleBlackboard());
							//do this to ensure the correct events are fired
							((Layer)layer).setStyleBlackboard((StyleBlackboard)layer.getStyleBlackboard());
						}

						// add style listener
						layer.addListener(styleListener);
					}	 
				 }
			};
			map.sendCommandSync(command);
			// setup styles
			map.getRenderManagerInternal().enableRendering();
			map.getRenderManager().refresh(null);

			

		}
	};

	/**
	 * Job to refresh the service and map.
	 */
	private Job refreshJob = new Job(
			Messages.GriddedResultsMapEditorPage_RefreshJobName) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (rasterService != null) {
				try {
					// remove existing layers
					List<? extends IGeoResource> layers = rasterService
							.resources(monitor);
					if (layers.size() > 0) {
						IGeoResource rasterLayers = layers.get(0);
						for (ILayer layer : getMap().getLayersInternal()) {
							// if(layer.getID().equals(rasterLayers.getIdentifier()
							// + "@type@geotiff")){
							if (layer.getID().sameFile(
									rasterLayers.getIdentifier())) {
								DeleteLayerCommand cmd = new DeleteLayerCommand(
										(Layer) layer);
								getMap().sendCommandASync(cmd);
								break;
							}
						}
					}
					// refresh and add new layers
					rasterService.refresh(null);
					addLayerJob.schedule();
				} catch (Exception ex) {
					String message = Messages.GriddedResultsMapEditorPage_Error_CreatingMapRaster
							+ ex.getLocalizedMessage();
					QueryPlugIn.displayLog(message, ex);
				}
			}
			// clear selection
			mapViewer.getRenderManager().refresh(null);
			return Status.OK_STATUS;
		}

	};

	/**
	 * Creates a new query map editor page
	 * 
	 * @param parent
	 *            parent editor
	 */
	public GriddedResultsMapEditorPage(GriddedEditor parent) {
		this.parentEditor = parent;
	}

	/**
	 * @see org.wcs.smart.ui.map.SmartMapEditorPart#getParentEditor()
	 */
	@Override
	public MultiPageEditorPart getParentEditor() {
		return this.parentEditor;
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite,
	 *      org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (!(input instanceof QueryEditorInput)) {
			throw new RuntimeException("Invalid editor input."); //$NON-NLS-1$
		}
		super.init(site, input);

		addInitialZoomFunction();
	}

	/**
	 * Creates the map
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);

		loadDefaultLayers = new LoadDefaultLayersJob(getMap(), false);
		loadDefaultLayers.schedule();
	}

	/**
	 * @see org.wcs.smart.ui.map.SmartMapEditorPart#dispose()
	 */
	@Override
	public void dispose() {
		JobUtil.stopJobs(loadDefaultLayers, addLayerJob, refreshJob);
		loadDefaultLayers = null;
		refreshJob = null;
		
		super.dispose();

		if (rasterService != null) {
			try {
				CatalogPlugin.getDefault().getLocalCatalog().remove(rasterService);
			} catch (Exception ex) {
			}
			this.rasterService.dispose(null);
			this.rasterService = null;
		}
	}

	/**
	 * Refresh the service on the map
	 */
	public void refresh(boolean firstRun) {
		if (rasterService == null) {
			addLayerJob.schedule();
		} else if (refreshJob != null) {
			refreshJob.schedule();
		}

		// update the map CRS as necessary
		if (firstRun) {
			// set crs of map to crs of query
			// only do this on the first run;
			Job setCrs = new Job(
					Messages.GriddedResultsMapEditorPage_SetCrsJobName) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try {
						loadDefaultLayers.join();
						if (!CRS.equalsIgnoreMetadata(getMap()
								.getViewportModel().getCRS(), parentEditor
								.getQueryInternal()
								.getCoordinateReferenceSystem())) {
							ChangeCRSCommand crs = new ChangeCRSCommand(
									parentEditor.getQueryInternal()
											.getCoordinateReferenceSystem());
							getMap().sendCommandASync(crs);
						}
					} catch (Exception ex) {
						// we will just log these
						QueryPlugIn
								.log(Messages.GriddedResultsMapEditorPage_ErrorSettingCrs
										+ ex.getLocalizedMessage(), ex);
					}
					return Status.OK_STATUS;
				}

			};
			setCrs.schedule();
		} else {
			// if map crs != query crs ask user if they want to update
			try {
				final CoordinateReferenceSystem querycrs = parentEditor
						.getQueryInternal().getCoordinateReferenceSystem();
				if (!CRS.equalsIgnoreMetadata(getMap().getViewportModel()
						.getCRS(), querycrs)) {
					parentEditor.getSite().getShell().getDisplay()
							.syncExec(new Runnable() {
								@Override
								public void run() {
									if (MessageDialog
											.openQuestion(
													parentEditor.getSite()
															.getShell(),
													Messages.GriddedResultsMapEditorPage_CRS_DialogTitle,
													Messages.GriddedResultsMapEditorPage_CRS_DialogMessage)) {
										ChangeCRSCommand crs = new ChangeCRSCommand(
												querycrs);
										getMap().sendCommandASync(crs);
									}
								}
							});

				}
			} catch (Exception ex) {
				QueryPlugIn.log(
						Messages.GriddedResultsMapEditorPage_Error_CheckingCrs
								+ ex.getLocalizedMessage(), ex);
			}

		}
	}

	/**
	 * Dispose of current query service and refresh to create a new one as
	 * required.
	 */
	public void reset() {
		// remove layers
		if (rasterService != null) {
			try {
				List<ILayer> toRemove = new ArrayList<ILayer>();
				for (ILayer layer : getMap().getLayersInternal()){
					if (  ((IService)layer.getGeoResource().resolve(IService.class,null)) == rasterService){
						toRemove.add(layer);
					}
				}
				if (toRemove.size() > 0) {
					getMap().sendCommandSync(
							new DeleteLayersCommand(toRemove.toArray(new ILayer[toRemove.size()])));
				}
			} catch (Exception ex) {
				QueryPlugIn.log(ex.getMessage(), ex);
			}

			CatalogPlugin.getDefault().getLocalCatalog().remove(rasterService);
			rasterService.dispose(null);
			rasterService = null;

			refresh(false);
		}
	}
}
