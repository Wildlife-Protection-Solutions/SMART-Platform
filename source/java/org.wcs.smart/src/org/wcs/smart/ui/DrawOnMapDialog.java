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

package org.wcs.smart.ui;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.styling.Fill;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbolizer;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.command.navigation.ZoomExtentCommand;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.commands.selection.SelectLayerCommand;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.internal.DialogMap;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.internal.tool.display.ToolManager;
import org.locationtech.udig.project.ui.internal.tool.display.ToolProxy;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.udig.AddLayerTool;
import org.wcs.smart.udig.SetBasemapTool;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.location.MapComposite;

/**
 * Map dialog for selecting points on map
 * 
 * @author Emily
 *
 */
public class DrawOnMapDialog extends SmartStyledDialog implements DialogMap {

	public enum Type {
		LINESTRING, POLYGON
	};

	private Type type;
	private List<Listener> dispose = new ArrayList<>();
	
	private MapComposite mapComposite;
	private IGeoResource editResource;
	
	private Geometry geometry;
	private Geometry initialGeometry;
	
	public DrawOnMapDialog(Shell parentShell, Type type, Geometry initialGeometry) {
		super(parentShell);
		this.type = type;
		this.initialGeometry = initialGeometry;
	}

	public MapPart getMapPart() {
		return mapComposite;
	}

	public List<Listener> getDisposeListeners() {
		return dispose;
	}

	public Geometry getGeometry() {
		return this.geometry;
	}
	

	@SuppressWarnings("unchecked")
	@Override
	protected void okPressed() {
		try {
			getMapPart().getMap().getEditManagerInternal().commitTransaction();
			
			FeatureCollection<?, SimpleFeature> fc = editResource.resolve(FeatureStore.class, new NullProgressMonitor()).getFeatures();
			List<Geometry> geoms = new ArrayList<>();
			
			try(FeatureIterator<SimpleFeature> it = fc.features()){
				while(it.hasNext()) {
					SimpleFeature sf = it.next();
					
					Geometry geom = (Geometry) sf.getDefaultGeometry();
					if (type == Type.POLYGON && geom instanceof Polygon) {
						geoms.add(geom);
					}else if (type == Type.LINESTRING && geom instanceof LineString) {
						geoms.add(geom);
					}
					
				}
			}
			if (geoms.isEmpty()) {
				this.geometry = null;
			}else if (type == Type.LINESTRING){
				LineString[] ls = new LineString[geoms.size()];
				for (int i = 0; i < ls.length; i ++) {
					ls[i] = (LineString) geoms.get(i);
				}
				this.geometry = GeometryFactoryProvider.getFactory().createMultiLineString(ls);
			}else if (type == Type.POLYGON){
				Polygon[] polys = new Polygon[geoms.size()];
				for (int i = 0; i < polys.length; i ++) {
					polys[i] = (Polygon) geoms.get(i);
				}
				this.geometry = GeometryFactoryProvider.getFactory().createMultiPolygon(polys);
			}else {
				this.geometry = null;
			}
			
		} catch (IOException e) {
			SmartPlugIn.displayLog(e.getMessage(), e);
			return;
		}

		super.okPressed();
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 600);
	}

	

	@Override
	protected Control createDialogArea(Composite parent) {

		parent = (Composite) super.createDialogArea(parent);

		Composite temp = new Composite(parent, SWT.NONE);
		temp.setLayout(new GridLayout(1, false));
		temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		String[] editTools = new String[] {};
		if (type == Type.POLYGON) {
			editTools = new String[] {
					AddLayerTool.ID,
					SetBasemapTool.ID, 
					MapToolComposite.SEPERATOR_TOOL_ID,
					MapToolComposite.UDIG_ZOOM_EXTENT_ID,
					MapToolComposite.UDIG_PAN_ID,
					MapToolComposite.UDIG_ZOOM_ID,
					MapToolComposite.UDIG_ZOOM_IN_ID,
					MapToolComposite.UDIG_ZOOM_OUT_ID,
					MapToolComposite.SEPERATOR_TOOL_ID,
					MapToolComposite.UDIG_GEOM_SELECTEDIT,
					MapToolComposite.UDIG_ADD_VERTEX,
					MapToolComposite.UDIG_REMOVE_VERTEX,
					MapToolComposite.UDIG_POLYGON_HOLE,
					MapToolComposite.UDIG_POLYGON_CREATE
			};
		}else if (type == Type.LINESTRING) {
			editTools = new String[] {
					AddLayerTool.ID,
					SetBasemapTool.ID, 
					MapToolComposite.SEPERATOR_TOOL_ID,
					MapToolComposite.UDIG_ZOOM_EXTENT_ID,
					MapToolComposite.UDIG_PAN_ID,
					MapToolComposite.UDIG_ZOOM_ID,
					MapToolComposite.UDIG_ZOOM_IN_ID,
					MapToolComposite.UDIG_ZOOM_OUT_ID,
					MapToolComposite.SEPERATOR_TOOL_ID,
					
					MapToolComposite.UDIG_GEOM_SELECTEDIT,
					MapToolComposite.UDIG_ADD_VERTEX,
					MapToolComposite.UDIG_REMOVE_VERTEX,
					MapToolComposite.UDIG_LINE_CREATE
			};
		}
		
		
		mapComposite = new MapComposite(temp, SWT.NONE, editTools) {
			@Override
			protected void addDefaultLayers() {
				
				final LoadDefaultLayersJob defaultLayer = new LoadDefaultLayersJob(getMap());
				defaultLayer.addJobChangeListener(new JobChangeAdapter() {
					@Override
					public void done(IJobChangeEvent event) {
						if (isDisposed() || getMapViewer() == null)
							return;
						initEditingLayer();
						//getMapViewer().getMap().sendCommandSync(new ZoomExtentCommand());
						//getMapViewer().getMap().getRenderManager().refresh(null);
					}
				});
				defaultLayer.schedule();
				
				
				// if I am disposed before finished cancel job
				addDisposeListener(new DisposeListener() {
					@Override
					public void widgetDisposed(DisposeEvent e) {
						defaultLayer.cancel();
					}
				});
			}
		};
		
		mapComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		
		init(mapComposite);
				
		mapComposite.addListener(SWT.Dispose, e -> dispose());
		getShell().setText(Messages.DrawOnMapDialog_DialogTitle);
		return parent;
	}

	@Override
	public boolean isResizable() {
		return true;
	}
	
	private void dispose() {
		
		// switch to pan tool to remove any editor listeners that stick around
		ToolProxy found = ((ToolManager) ApplicationGIS.getToolManager())
				.findToolProxy(MapToolComposite.UDIG_PAN_ID);
		try {
			ApplicationGIS.getToolManager().getToolAction(found.getId(), found.getCategoryId()).run();
		}catch (NullPointerException ex) {
			//
		}

		getMapPart().getMap().getEditManagerInternal().setEditFeature(null, null);

		try {
			CatalogPlugin.getDefault().getLocalCatalog().remove(editResource.service(new NullProgressMonitor()));
		} catch (Exception e1) {
			SmartPlugIn.log(e1.getMessage(), e1);
		}
	}

	private void initEditingLayer() {
		try {
			String typeStr ="the_geom:Polygon:srid=4326"; //$NON-NLS-1$
			if (type == Type.LINESTRING) {
				typeStr ="the_geom:LineString:srid=4326"; //$NON-NLS-1$
			}
			SimpleFeatureType editType = DataUtilities.createType(
					"tempattributeedit" + System.nanoTime(), typeStr); //$NON-NLS-1$
			
			editResource = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(editType);
	
			AddLayersCommand command = new AddLayersCommand(Collections.singletonList(editResource), mapComposite.getMap().getLayersInternal().size()) {
				@SuppressWarnings("unchecked")
				public void run(IProgressMonitor monitor) throws Exception {
					super.run(monitor);
					if (getLayers() == null || getLayers().isEmpty())
						return;
	
					Layer editLayer = getLayers().get(0);
					editLayer.getStyleBlackboard().put(SLDContent.ID, getLayerStyle());
					
					//select for editing
					getMap().sendCommandSync(new SelectLayerCommand(editLayer));
	
					//add any initial features
					
					SimpleFeatureBuilder builder = new SimpleFeatureBuilder(editType);
					List<SimpleFeature> features = new ArrayList<>();
					if (initialGeometry != null) {
						if (type == Type.LINESTRING && initialGeometry instanceof MultiLineString) {
							MultiLineString mls = (MultiLineString)initialGeometry;
							for (int i = 0; i < mls.getNumGeometries(); i ++) {
								LineString ls = (LineString) mls.getGeometryN(i);
								
								builder.set("the_geom", ls); //$NON-NLS-1$
								SimpleFeature feature = builder.buildFeature("fid_" + i); //$NON-NLS-1$
								features.add(feature);
							}
						}
						if (type == Type.POLYGON && initialGeometry instanceof MultiPolygon) {
							MultiPolygon mls = (MultiPolygon)initialGeometry;
							for (int i = 0; i < mls.getNumGeometries(); i ++) {
								Polygon ls = (Polygon) mls.getGeometryN(i);
								
								builder.set("the_geom", ls); //$NON-NLS-1$
								SimpleFeature feature = builder.buildFeature("fid_" + i); //$NON-NLS-1$
								features.add(feature);
							}
						}
					}
					if (!features.isEmpty()) {
						ListFeatureCollection fc = new ListFeatureCollection(editType);
						fc.addAll(features);
						FeatureStore<SimpleFeatureType, SimpleFeature> store = editResource.resolve(FeatureStore.class, monitor);
						try{
							store.removeFeatures(Filter.INCLUDE);
							store.addFeatures(fc);
						}catch (ConcurrentModificationException ex){
							//try again - this should only happen once (udig removes listener)
							//see SMART bug 1672
							store.removeFeatures(Filter.INCLUDE);
							store.addFeatures(fc);
						}
	
						getMap().getEditManagerInternal().commitTransaction();
					}	
	
					getMap().sendCommandSync(new ZoomExtentCommand());
	
				}
			};
	
			mapComposite.getMap().sendCommandASync(command);
		}catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private Style getLayerStyle() {
		StyleFactory sf = CommonFactoryFinder.getStyleFactory();
		StyleBuilder sb = new StyleBuilder(sf);
		
		Symbolizer sym = null;
		if (type == Type.POLYGON) {
			Fill fill = sb.createFill(new Color(255, 238, 238), 0.5);
			Stroke stroke = sb.createStroke(new Color(255, 51, 51), 1);
			sb.createPolygonSymbolizer(stroke, fill);
			sym = sb.createPolygonSymbolizer(stroke, fill);
		}
		
		if (type == Type.LINESTRING) {			
			Stroke stroke = sb.createStroke(new Color(255, 51, 51), 1);
			sym = sb.createLineSymbolizer(stroke);
		}
		
		Rule rr = sb.createRule(new Symbolizer[] { sym });
		org.geotools.styling.FeatureTypeStyle fts = sf.createFeatureTypeStyle();
		fts.setName("FeatureStyle"); //$NON-NLS-1$
		fts.rules().add(rr);
		Style style = sf.createStyle();
		style.featureTypeStyles().add(fts);
		return style;
	}
}
