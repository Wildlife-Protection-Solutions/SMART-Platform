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
package org.wcs.smart.ui.map.location;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.XMLMemento;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.styling.Style;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.command.navigation.ZoomExtentCommand;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.render.ViewportModel;
import org.locationtech.udig.project.render.displayAdapter.IMapDisplayListener;
import org.locationtech.udig.project.render.displayAdapter.MapDisplayEvent;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseWheelEvent;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseWheelListener;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.locationtech.udig.project.ui.viewers.MapViewer;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ISmartPoint;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.udig.SetBasemapTool;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapInfoAreaComposite;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.location.tool.SelectionTool;
import org.wcs.smart.ui.map.tool.PanTool;
import org.wcs.smart.ui.map.tool.ZoomExtentTool;
import org.wcs.smart.ui.map.tool.ZoomInTool;
import org.wcs.smart.ui.map.tool.ZoomOutTool;
import org.wcs.smart.ui.map.tool.ZoomTool;
import org.wcs.smart.util.GeometryUtils;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Map Composite
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class MapComposite extends Composite implements MapPart {

	private static final String SMART_POINT_SPEC = "fid:String,id:Integer,selected:Boolean,geom:Point:srid=4326"; //$NON-NLS-1$
	private static final String SMART_POINT_TYPE_NAME = "smart.ISmartPoint"; //$NON-NLS-1$

	private SimpleFeatureType featureType;
	private ListFeatureCollection featureCollection;
	private FeatureStore<SimpleFeatureType,SimpleFeature> store;
	private Layer pointLayer = null;
	private IGeoResource pointResource;
	
	private MapViewer mapViewer;

	private ISmartPointDataProvider dataProvider;
	private String styleSld = null;
	
	private Job refreshJob = new Job(Messages.MapComposite_MapResizeJob_Title){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (!isDisposed() && mapViewer != null){
				mapViewer.getMap().getRenderManager().refresh(null);
			}
			return Status.OK_STATUS;
		}
	};
	
	/**
	 * @param parent
	 * @param style
	 */
	public MapComposite(Composite parent, int style) {
		super(parent, style);
		createControls();
	}
	
	@Override
	public void dispose(){
		super.dispose();
		mapViewer.getRenderManager().stopRendering();
		mapViewer.getRenderManager().dispose();
		mapViewer.dispose();
		mapViewer = null;
	}
	
	public void setStyleSld(String sld){
		this.styleSld = sld;
	}
	
	private void createControls() {

		GridLayout gd = new GridLayout(2, false);
		gd.marginBottom=0;
		gd.marginHeight = 0;
		gd.marginLeft = 0;
		gd.marginRight = 0;
		gd.marginTop = 0;
		gd.marginWidth = 0;
		this.setLayout(gd);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		mapViewer = new MapViewer(this,  SWT.SINGLE | SWT.DOUBLE_BUFFERED);
		mapViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Map map = (Map) ProjectFactory.eINSTANCE.createMap();
		map.setName(Messages.MapComposite_Map_Name);
		mapViewer.setMap(map);
		//set default crs
		mapViewer.getMap().getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
		mapViewer.getMap().getViewportModelInternal().setCRS(GeometryUtils.SMART_CRS);

		ApplicationGIS.getToolManager().setCurrentEditor(this);
		String[] thisTools = new String[] {
				SetBasemapTool.ID,
				ZoomExtentTool.ID,
				PanTool.ID,
				ZoomTool.ID,
				ZoomInTool.ID,
				ZoomOutTool.ID,
				SelectionTool.ID };

		MapToolComposite tools = new MapToolComposite(thisTools);
		tools.createComposite(this);
		new MapInfoAreaComposite(this, SWT.NONE, mapViewer) ;

		tools.selectTool(PanTool.ID);

		addPointsLayer();
		final LoadDefaultLayersJob defaultLayer = new LoadDefaultLayersJob(mapViewer.getMap());
		// we need to do this because this map is in a dialog box and
		// events does work correctly
		defaultLayer.addJobChangeListener(new JobChangeAdapter() {
			@Override
			public void done(IJobChangeEvent event) {
				if (isDisposed() || mapViewer == null) return;
				
				mapViewer.getMap().sendCommandSync(new ZoomExtentCommand());
				mapViewer.getMap().getRenderManager().refresh(null);
			}
		});
		defaultLayer.schedule();
		
		//if I am disposed before finished cancel job
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				defaultLayer.cancel();
			}
		});
	
		mapViewer.getViewport().addPaneListener(new IMapDisplayListener() {
			@Override
			public void sizeChanged(MapDisplayEvent event) {
				refreshJob.cancel();
				refreshJob.schedule(600);
			}
		});
		mapViewer.getViewport().addMouseWheelListener(new MapMouseWheelListener() {
			
			@Override
			public void mouseWheelMoved(MapMouseWheelEvent e) {
				refreshJob.cancel();
				refreshJob.schedule(600);
			}
		});

	}
	
	@SuppressWarnings("unchecked")
	private void addPointsLayer() {
        try {
			featureType = DataUtilities.createType(SMART_POINT_TYPE_NAME, SMART_POINT_SPEC);
			featureCollection = new ListFeatureCollection(featureType);
			pointResource = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(featureType);
		
			//dispose of temporary layer when composite is disposed
			super.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					try{
						if (pointLayer != null){
							CatalogPlugin.getDefault().getLocalCatalog().remove(pointLayer.getGeoResource().service(null));
						}
					}catch (Exception ex){
						SmartPlugIn.log("Error removing service", ex); //$NON-NLS-1$
					}
					
				}
			});
	        store = pointResource.resolve(FeatureStore.class, null);
	        pointResource.resolve(DataStore.class, null);
			List<IGeoResource> layers = new ArrayList<IGeoResource>();
			layers.add(pointResource);
			
			AddLayersCommand command = new AddLayersCommand(layers, 0) {
				@Override
				public void run(IProgressMonitor monitor) throws Exception {
					super.run(monitor);
					//set custom style for points layer
					Layer pointLayerEx = getLayers().get(0);
					String sld = getStylingConfig();
					XMLMemento memento = XMLMemento.createReadRoot(new StringReader(sld));
					SLDContent c = new SLDContent();
					Style style = (Style)c.load(memento);
					pointLayerEx.getStyleBlackboard().put(SLDContent.ID, style);
				}
			};
			getMap().sendCommandASync(command);
        } catch (Exception exception) {
			SmartPlugIn.displayLog(Messages.MapComposite_PointLayer_Add_Error, exception);
		}
		
	}

	public void updatePointsLayer() {
		if (store == null) {
			return; //most likely we failed to add points layer
		}
		try {
			featureCollection.clear();
			featureCollection.addAll(getSmartPointAsFeatures(featureType));
			
			try{
				store.removeFeatures(Filter.INCLUDE);
				store.addFeatures(featureCollection);
			}catch (ConcurrentModificationException ex){
				//try again - this should only happen once (udig removes listener)
				//see SMART bug 1672
				store.removeFeatures(Filter.INCLUDE);
				store.addFeatures(featureCollection);
			}
			
			
		} catch (IOException e) {
			SmartPlugIn.displayLog(Messages.MapComposite_PointLayer_Update_Error, e);
		}
		//refresh map - only refresh point layer 
		if (pointLayer == null){
			for (ILayer layer : getMap().getMapLayers()){
				if (layer.getGeoResource().getID().equals(pointResource.getID())){
					pointLayer = (Layer)layer;
				}
			}
		}
		if (pointLayer != null){
			pointLayer.refresh(null);
		}
		return;
	}
	
	private List<SimpleFeature> getSmartPointAsFeatures(SimpleFeatureType ftype) {
		if (getDataProvider() == null) {
			return Collections.emptyList();
		}
		List<? extends ISmartPoint> points = getDataProvider().getPoints();
		int size = points.size();
		List<SimpleFeature> features = new ArrayList<SimpleFeature>(size);
		for (int i = 0; i < size; i++) {
			ISmartPoint point = points.get(i);
			Object data[] = new Object[4];
			String name = ftype.getName() + "." + i; //$NON-NLS-1$
			data[0] = name;
			data[1] = i;
			data[2] = getDataProvider().isSelected(point);
			data[3] = GeometryFactoryProvider.getFactory().createPoint(new Coordinate(point.getX(), point.getY()));
			features.add(SimpleFeatureBuilder.build(ftype, data, name));
		}
		return features;
	}

	public void setDataProvider(ISmartPointDataProvider dataProvider) {
		this.dataProvider = dataProvider;
	}
	
	protected ISmartPointDataProvider getDataProvider() {
		return dataProvider;
	}
	
	@Override
	public Map getMap() {
		if (mapViewer == null){
			return null;
		}
		return mapViewer.getMap();
	}

	@Override
	public void openContextMenu() {
		mapViewer.openContextMenu();

	}

	@Override
	public void setFont(Control textArea) {
		mapViewer.setFont(textArea);

	}

	@Override
	public void setSelectionProvider(IMapEditorSelectionProvider selectionProvider) {
		if (mapViewer == null) return;
		mapViewer.setSelectionProvider(selectionProvider);

	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return null;
	}

	private String getStylingConfig() {
		if (styleSld != null){
			return styleSld;
		}
		return	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+ //$NON-NLS-1$
		"<styleEntry version=\"1.0\" type=\"SLDStyle\">"+ //$NON-NLS-1$
		"&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;"+ //$NON-NLS-1$
		"	&lt;sld:UserStyle xmlns=\"http://www.opengis.net/sld\""+ //$NON-NLS-1$
		"		xmlns:sld=\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis.net/ogc\""+ //$NON-NLS-1$
		"		xmlns:gml=\"http://www.opengis.net/gml\"&gt;"+ //$NON-NLS-1$
		"		&lt;sld:Name&gt;Default Styler&lt;/sld:Name&gt;"+ //$NON-NLS-1$
		"		&lt;sld:Title /&gt;"+ //$NON-NLS-1$
		"		&lt;sld:FeatureTypeStyle&gt;"+ //$NON-NLS-1$
		"			&lt;sld:Name&gt;simple&lt;/sld:Name&gt;"+ //$NON-NLS-1$
		"			&lt;sld:FeatureTypeName&gt;Feature&lt;/sld:FeatureTypeName&gt;"+ //$NON-NLS-1$
		"			&lt;sld:SemanticTypeIdentifier&gt;generic:geometry&lt;/sld:SemanticTypeIdentifier&gt;"+ //$NON-NLS-1$
		"			&lt;sld:SemanticTypeIdentifier&gt;simple&lt;/sld:SemanticTypeIdentifier&gt;"+ //$NON-NLS-1$

		//rule for not selected points (same as default)
		"			&lt;sld:Rule&gt;"+ //$NON-NLS-1$
		"				&lt;ogc:Filter&gt;"+ //$NON-NLS-1$
		"                        &lt;ogc:PropertyIsEqualTo&gt;"+ //$NON-NLS-1$
		"                            &lt;ogc:PropertyName&gt;selected&lt;/ogc:PropertyName&gt;"+ //$NON-NLS-1$
		"                            &lt;ogc:Literal&gt;false&lt;/ogc:Literal&gt;"+ //$NON-NLS-1$
		"                        &lt;/ogc:PropertyIsEqualTo&gt;"+ //$NON-NLS-1$
		"				&lt;/ogc:Filter&gt;"+ //$NON-NLS-1$
		"				&lt;sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
		"					&lt;sld:Graphic&gt;"+ //$NON-NLS-1$
		"						&lt;sld:Mark&gt;"+ //$NON-NLS-1$
		"							&lt;sld:Fill /&gt;"+ //$NON-NLS-1$
		"							&lt;sld:Stroke /&gt;"+ //$NON-NLS-1$
		"						&lt;/sld:Mark&gt;"+ //$NON-NLS-1$
		"						&lt;sld:Mark&gt;"+ //$NON-NLS-1$
		"							&lt;sld:Fill&gt;"+ //$NON-NLS-1$
		"								&lt;sld:CssParameter name=\"fill\"&gt;#1B9E77&lt;/sld:CssParameter&gt;"+ //$NON-NLS-1$
		"							&lt;/sld:Fill&gt;"+ //$NON-NLS-1$
		"							&lt;sld:Stroke /&gt;"+ //$NON-NLS-1$
		"						&lt;/sld:Mark&gt;"+ //$NON-NLS-1$
		"						&lt;sld:Size&gt;6.0&lt;/sld:Size&gt;"+ //$NON-NLS-1$
		"					&lt;/sld:Graphic&gt;"+ //$NON-NLS-1$
		"				&lt;/sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
		"			&lt;/sld:Rule&gt;"+ //$NON-NLS-1$
		
		//rule for selected points
		"			&lt;sld:Rule&gt;"+ //$NON-NLS-1$
		"				&lt;ogc:Filter&gt;"+ //$NON-NLS-1$
		"                        &lt;ogc:PropertyIsEqualTo&gt;"+ //$NON-NLS-1$
		"                            &lt;ogc:PropertyName&gt;selected&lt;/ogc:PropertyName&gt;"+ //$NON-NLS-1$
		"                            &lt;ogc:Literal&gt;true&lt;/ogc:Literal&gt;"+ //$NON-NLS-1$
		"                        &lt;/ogc:PropertyIsEqualTo&gt;"+ //$NON-NLS-1$
		"				&lt;/ogc:Filter&gt;"+ //$NON-NLS-1$
		"				&lt;sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
		"					&lt;sld:Graphic&gt;"+ //$NON-NLS-1$
		"						&lt;sld:Mark&gt;"+ //$NON-NLS-1$
		"							&lt;sld:Fill&gt;"+ //$NON-NLS-1$
		"								&lt;sld:CssParameter name=\"fill\"&gt;#FF000&lt;/sld:CssParameter&gt;"+ //$NON-NLS-1$
		"							&lt;/sld:Fill&gt;"+ //$NON-NLS-1$
		"							&lt;sld:Stroke /&gt;"+ //$NON-NLS-1$
		"						&lt;/sld:Mark&gt;"+ //$NON-NLS-1$
		"						&lt;sld:Size&gt;6.0&lt;/sld:Size&gt;"+ //$NON-NLS-1$
		"					&lt;/sld:Graphic&gt;"+ //$NON-NLS-1$
		"				&lt;/sld:PointSymbolizer&gt;"+ //$NON-NLS-1$
		"			&lt;/sld:Rule&gt;"+ //$NON-NLS-1$
		
		
		"		&lt;/sld:FeatureTypeStyle&gt;"+ //$NON-NLS-1$
		"	&lt;/sld:UserStyle&gt;"+ //$NON-NLS-1$
		"</styleEntry>"; //$NON-NLS-1$
	
	}
	
}
