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
package org.wcs.smart.report.birt.map.execute;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.data.engine.api.IDataQueryDefinition;
import org.eclipse.birt.data.engine.api.IQueryDefinition;
import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.report.engine.content.IContent;
import org.eclipse.birt.report.engine.content.IForeignContent;
import org.eclipse.birt.report.engine.extension.IBaseResultSet;
import org.eclipse.birt.report.engine.extension.IExecutorContext;
import org.eclipse.birt.report.engine.extension.IReportItemExecutor;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.styling.Style;
import org.geotools.styling.visitor.RescaleStyleVisitor;
import org.hibernate.Session;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.mapgraphic.MapGraphic;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.command.MapCommand;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.StyleEntry;
import org.locationtech.udig.project.internal.impl.MapImpl;
import org.locationtech.udig.project.internal.render.CompositeRenderContext;
import org.locationtech.udig.project.internal.render.RenderContext;
import org.locationtech.udig.project.internal.render.RenderFactory;
import org.locationtech.udig.project.internal.render.RenderManager;
import org.locationtech.udig.project.internal.render.Renderer;
import org.locationtech.udig.project.internal.render.RendererCreator;
import org.locationtech.udig.project.internal.render.SelectionLayer;
import org.locationtech.udig.project.internal.render.ViewportModel;
import org.locationtech.udig.project.internal.render.impl.RenderContextImpl;
import org.locationtech.udig.project.internal.render.impl.RendererCreatorImpl;
import org.locationtech.udig.project.render.ILabelPainter;
import org.locationtech.udig.project.render.RenderException;
import org.locationtech.udig.project.render.displayAdapter.IMapDisplay;
import org.locationtech.udig.project.render.displayAdapter.MapDisplayEvent;
import org.locationtech.udig.style.sld.SLDContent;
import org.wcs.smart.birt.BirtConstants;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.SmartStyle;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.report.birt.map.AddLayersCommand;
import org.wcs.smart.report.birt.map.BirtMapFactory;
import org.wcs.smart.report.birt.map.BirtMapUtils;
import org.wcs.smart.report.birt.map.BirtMapViewportModelImpl;
import org.wcs.smart.report.birt.map.BoundsSetting;
import org.wcs.smart.report.birt.map.BoundsSetting.BoundsOption;
import org.wcs.smart.report.birt.map.ExtensionManager;
import org.wcs.smart.report.birt.map.IRasterCreator;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.MapLayerInfo.LayerType;
import org.wcs.smart.report.birt.map.RestoreMapSettings;
import org.wcs.smart.report.birt.map.item.LayerItem;
import org.wcs.smart.report.birt.map.item.SmartMapItem;
import org.wcs.smart.report.birt.map.udig.MapGeoResource;
import org.wcs.smart.report.birt.map.udig.MapQueryService;
import org.wcs.smart.udig.catalog.smart.IDatabaseConnectionProvider;
import org.wcs.smart.udig.catalog.smart.ISessionService;
import org.wcs.smart.udig.catalog.smart.SmartService;
import org.wcs.smart.udig.catalog.smart.SmartServiceExtension;
import org.wcs.smart.udig.style.SmartLayerStyle;
import org.wcs.smart.udig.style.StyleManager;
import org.wcs.smart.util.UuidUtils;

/**
 * Executor for SMART Birt Map.  Gets the query
 * results, renders the map and sets content 
 * as a byte array
 * 
 * 
 * @author Emily
 *
 */
public class MapItemExecutor implements IReportItemExecutor{

	class GeoSmart {
		DataSetHandle handle;
		IGeoResource georesource;
		Layer layer;
		MapLayerInfo info;
	}
	
	private ExtendedItemHandle modelHandle;
	private IExecutorContext context;
	private IReportItemExecutor parent;
	
	private List<Path> cleanUp;
	private List<IRasterCreator> creators;
	
	private IForeignContent content = null;
	private int defaultDpi = 96;
	
	@Override
	public void setModelObject(Object handle) {
		if ( handle instanceof ExtendedItemHandle ){
			this.modelHandle = (ExtendedItemHandle)handle;
		}
	}

	@Override
	public void setContext(IExecutorContext context) {
		this.context = context;
		Integer dpi = (Integer) context.getAppContext().get(BirtConstants.DEFAULT_DPI_PARAM);
		if (dpi != null) {
			defaultDpi = dpi;
		}
	}

	@Override
	public void setParent(IReportItemExecutor parent) {
		if (content != null) {
			content.setParent(parent.getContent());
		}
		this.parent = parent;
		
	}

	@Override
	public IReportItemExecutor getParent() {
		return this.parent;
	}

	@Override
	public Object getModelObject() {
		return this.modelHandle;
	}

	@Override
	public IExecutorContext getContext() {
		return this.context;
	}

	@Override
	public IContent execute() throws BirtException {
		content = context.getReportContent().createForeignContent();
		if (getParent() != null) {
			content.setParent(getParent().getContent());
		}
		content.setRawType(IForeignContent.EXTERNAL_TYPE);
		try {
			content.setRawValue(executeQuery());
		}catch(Exception ex) {
			ex.printStackTrace();
			throw new BirtException(ex.getMessage());
		}
		return content;
	}

	@Override
	public IBaseResultSet[] getQueryResults() {
		return new IBaseResultSet[] {};
	}

	@Override
	public IContent getContent() {
		return content;
	}

	@Override
	public boolean hasNextChild() throws BirtException {
		return false;
	}

	@Override
	public IReportItemExecutor getNextChild() throws BirtException {
		return null;
	}

	@Override
	public void close() throws BirtException {
		//delete all temporary raster files
		for (Path f : cleanUp){
			try{
				Files.delete(f);
			}catch (Throwable t2) {
				try {
					f.toFile().deleteOnExit();//if we cannot delete now try on exit
				}catch (Throwable t) {
					Logger.getLogger(MapItemExecutor.class.getName()).log(Level.WARNING, t.getMessage(), t);
				}
			}
		}
	}
	
	private void cleanUp(List<GeoSmart> layers) {
		NullProgressMonitor monitor = new NullProgressMonitor();
		for (GeoSmart layer : layers) {
			if (layer.georesource != null) {
				layer.georesource.dispose(monitor);
			}
		}
	}

	private synchronized List<IRasterCreator> getRasterCreators(){
		if (creators == null){
			try{
				creators = ExtensionManager.INSTANCE.getRasterCreators();
			}catch (Exception ex){
				creators = new ArrayList<IRasterCreator>();
				Logger.getLogger(MapItemExecutor.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
			}
		}
		return creators;
	}
	
	protected byte[] executeQuery( ) throws Exception {
		cleanUp = new ArrayList<>();
		
		MapConfiguration configuration = new MapConfiguration(96);
		
		DesignElementHandle elementHandle = modelHandle.getContainer();
		SmartMapItem mapItem = (SmartMapItem) modelHandle.getReportItem();
		IDataQueryDefinition[] query = context.getQueries( modelHandle );
		
		//I am assuming here that the queries are returned in the same order as the map Item layers property
		StringBuilder exceptions = new StringBuilder();
		
		for ( int i = 0; i < mapItem.getLayersProperty().getContentCount(); i++ ){
			LayerItem layer = mapItem.getLayer(i);
			IQueryDefinition q2 = (IQueryDefinition) query[i];	
			if (q2 == null) continue;
			MapLayerInfo info = new MapLayerInfo(layer);
			
			IBaseResultSet qresult = null;
			try {
				qresult = context.executeQuery( null, q2, elementHandle );
			}catch (SmartQueryExecutionException ex) {
				Logger.getLogger(MapItemExecutor.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
				if (exceptions.length() != 0) exceptions.append("\n"); //$NON-NLS-1$
				exceptions.append(layer.getLayerName() + ": " + ex.getLocalizedMessage()); //$NON-NLS-1$
				continue;
			}
			
			if (qresult != null){
				//if qresult is null we were unable to execute the query for some reason
				configuration.addQuery(qresult,info);
				String queryText = ((OdaDataSetHandle)layer.getHandle().getDataSet()).getQueryText();
	
				double minValue = 0;
				double maxValue = 0;
		
				if (info.getLayerType() == LayerType.RASTER){
					String datasetId = (((OdaDataSetHandle)layer.getHandle().getDataSet()).getExtensionID());
					for (IRasterCreator creator : getRasterCreators()){
						try{
							if (creator.canProcess(context, datasetId, queryText)){
								Path f = creator.createRaster(context, datasetId, qresult);
								if (f != null){
									info.setRasterFile(f);
									minValue = creator.getRasterMinValue();
									maxValue = creator.getRasterMaxValue();
								}
								cleanUp.addAll(creator.getFilesToCleanUp());
								break;
							}
						}catch (Exception ex){
							Logger.getLogger(MapItemExecutor.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
							if (exceptions.length() != 0) exceptions.append("\n"); //$NON-NLS-1$
							exceptions.append(layer.getLayerName() + ": " + ex.getLocalizedMessage()); //$NON-NLS-1$
						}
					}
				}
				//Configure layer styles
				processStyles(info, layer, queryText, minValue, maxValue);
			}
		}		
		
		if (exceptions.length() > 0) {
			throw new BirtException(exceptions.toString());
		}
		
		ByteArrayOutputStream stream = createMap(configuration, mapItem);
		return stream.toByteArray();
	}

	private void processStyles(MapLayerInfo def, LayerItem layer,
			String queryText, double minValue, double maxValue) {
		StyleBlackboard styleBlackboard = null;
		try{
			if (def.getMapStyle() == null
					|| def.getMapStyle().trim().isEmpty()){
				//no style is provided; so lets try to load the default style from the query
				Session session =  (Session)context.getAppContext().get(BirtConstants.SESSION_PARAM);
				String xid = ((OdaDataSetHandle)layer.getHandle().getDataSet()).getExtensionID();
				styleBlackboard = BirtStyleManager.INSTANCE.getStyle(xid, queryText, def.getLayerType(), session);
			}else{
				//parse provided style
				String styleString = def.getMapStyle();
				styleBlackboard = BirtMapUtils.parseStyleString(styleString);
			}
			
			if (styleBlackboard != null){
				//parse out smart saved style
				Session session =  (Session)context.getAppContext().get(BirtConstants.SESSION_PARAM);
				StyleBlackboard ss = processSmartStyle(styleBlackboard, session);
				if (ss != null){
					def.setMapStyleBlackboard(ss);
				}else{
					def.setMapStyleBlackboard(styleBlackboard);
				}
			}else{
				//for raster layers we want to generate a defaul style 
				if (layer.getLayerType() == LayerType.RASTER){
					def.setMapStyleBlackboard(createDefaultRasterStyle(minValue, maxValue));
				}
			}
		}catch (Exception ex){
			Logger.getLogger(MapItemExecutor.class.getName()).log(Level.WARNING, "Error loading layer style for report map.", ex); //$NON-NLS-1$
		}
	}

	private StyleBlackboard createDefaultRasterStyle(double minValue, double maxValue){
		String sld = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" //$NON-NLS-1$
				+ "<styleEntry type=\"SLDStyle\" version=\"1.0\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;" //$NON-NLS-1$
				+ "&lt;sld:StyledLayerDescriptor xmlns=&quot;http://www.opengis.net/sld&quot; xmlns:sld=&quot;http://www.opengis.net/sld&quot; xmlns:ogc=&quot;http://www.opengis.net/ogc&quot; xmlns:gml=&quot;http://www.opengis.net/gml&quot; version=&quot;1.0.0&quot;&gt;" //$NON-NLS-1$
				+ "&lt;sld:UserLayer&gt; " //$NON-NLS-1$
				+ "&lt;sld:LayerFeatureConstraints&gt; " //$NON-NLS-1$
				+ "&lt;sld:FeatureTypeConstraint/&gt; " //$NON-NLS-1$
				+ "&lt;/sld:LayerFeatureConstraints&gt; " //$NON-NLS-1$
				+ "&lt;sld:UserStyle&gt; " //$NON-NLS-1$
				+ "&lt;sld:Name&gt;000051&lt;/sld:Name&gt; " //$NON-NLS-1$
				+ "&lt;sld:Title/&gt; &lt;sld:FeatureTypeStyle&gt; " //$NON-NLS-1$
				+ "&lt;sld:Name&gt;name&lt;/sld:Name&gt; &lt;sld:Rule&gt; " //$NON-NLS-1$
				+ "&lt;sld:RasterSymbolizer&gt; &lt;sld:Geometry&gt; " //$NON-NLS-1$
				+ "&lt;ogc:PropertyName&gt;grid&lt;/ogc:PropertyName&gt; " //$NON-NLS-1$
				+ "&lt;/sld:Geometry&gt; &lt;sld:ColorMap&gt; " //$NON-NLS-1$
				+ "&lt;sld:ColorMapEntry color=&quot;#FFFFFF&quot; opacity=&quot;0.0&quot; quantity=&quot;-9999&quot; label=&quot;-no data-&quot;/&gt; "; //$NON-NLS-1$
		sld +="&lt;sld:ColorMapEntry color=&quot;#FFECEC&quot; opacity=&quot;0.8&quot; quantity=&quot;" + minValue; //$NON-NLS-1$
		sld += "&quot;/&gt; &lt;sld:ColorMapEntry color=&quot;#FF0000&quot; opacity=&quot;0.8&quot; quantity=&quot;" //$NON-NLS-1$
				+ maxValue
				+ "&quot;/&gt; &lt;/sld:ColorMap&gt; &lt;/sld:RasterSymbolizer&gt; &lt;/sld:Rule&gt; &lt;/sld:FeatureTypeStyle&gt; &lt;/sld:UserStyle&gt; &lt;/sld:UserLayer&gt;&lt;/sld:StyledLayerDescriptor&gt;</styleEntry>"; //$NON-NLS-1$
		try {
			XMLMemento memento = XMLMemento.createReadRoot(new StringReader(sld));
			SLDContent c = new SLDContent();
			Style style = (Style)c.load(memento);
			
			StyleBlackboard sb = ProjectFactory.eINSTANCE.createStyleBlackboard();
			sb.put(SLDContent.ID, style);
			return sb;
		} catch (Exception ex) {
			Logger.getLogger(MapItemExecutor.class.getName()).log(Level.FINE, "Could not create raster default style.", ex); //$NON-NLS-1$
			return null;
		}
	}
	private StyleBlackboard processSmartStyle(StyleBlackboard blackboard, Session session) throws WorkbenchException, IOException{
		//check for SMART saved style
		final UUID styleUuid = (UUID) blackboard.get(SmartLayerStyle.STYLE_ID);
		if (styleUuid != null) {
			//load from database			
			SmartStyle ss = (SmartStyle) session.get(SmartStyle.class, styleUuid);
			if (ss != null){
				return StyleManager.INSTANCE.fromString(ss.getStyleString());
			}
		}
		return null;
	}

	private ByteArrayOutputStream createMap(MapConfiguration queryResults, SmartMapItem mapItem) throws Exception {
		
		int localdpi = 96;
		if (mapItem.getDPI() != null){
			localdpi = mapItem.getDPI();
			if (localdpi < 72 || localdpi > 4000) localdpi = 96;
		}
		
		int iwidth = BirtMapUtils.getWidthInPx(modelHandle, localdpi);
		int iheight = BirtMapUtils.getHeightInPx(modelHandle, localdpi);
		List<GeoSmart> layers = new ArrayList<GeoSmart>();

		
		ConservationArea reportca = (ConservationArea) context.getAppContext().get(BirtConstants.CA_PARAM);
		
		// -- Create Map --
		IMap renderedMap = BirtMapFactory.createMap();
		((MapImpl) renderedMap).getContextModel().eSetDeliver(false);
		((MapImpl)renderedMap).eSetDeliver(false);
		
		// -- Restore Basemap --
		IDatabaseConnectionProvider provider = new IDatabaseConnectionProvider() {
			
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Session openSession() {
				return (Session)context.getAppContext().get(BirtConstants.SESSION_PARAM);
			}
			
			@Override
			public Locale getLocale() {
				return context.getLocale();
			}
			
			@Override
			public void finishSession(Session session) {
				//do nothing we don't want to close the session as it is reused
			}
		};
		
		BasemapDefinition def = getBasemap(mapItem.getBasemapName());
		if (def != null) {
			(new RestoreMapSettings()).applyTo((Map) renderedMap, def, reportca, provider);
		} else if (reportca != null){
			if (mapItem.getBasemapName() != null
					&& mapItem.getBasemapName().equals(
							SmartMapItem.DEFAULT_BASEMAP_KEY)) {
				// default basemap; but no default is set so use the SMART
				// default of the 5 layers
				HashMap<String, Serializable> params = new HashMap<String, Serializable>();
				params.put(SmartServiceExtension.CA_UUID_KEY, reportca.getUuid());
				SmartService ss = (SmartService) (new SmartServiceExtension()).createService(null, params);
				if (ss instanceof ISessionService){
	            	((ISessionService) ss).setConnectionProvider(provider);
	            }
				List<? extends IGeoResource> defaultLayers = ss.resources(null);
				AddLayersCommand alCommand = new AddLayersCommand(defaultLayers, 0);
				executeCommmand(renderedMap, alCommand);
			}
		}

		// -- Add dataset layers --
		List<IGeoResource> toAdd = new ArrayList<IGeoResource>();
		MapQueryService service = new MapQueryService();

		for (int i = 0; i < queryResults.getQueryCount(); i++) {
			

			IQueryResults results = (IQueryResults) queryResults.getQueryResults(i).getQueryResults();
			
			MapGeoResource resource = new MapGeoResource(results, 
					queryResults.getInfo(i), 
					service);
			service.addResource(resource);
			
			GeoSmart geoSmart = new GeoSmart();
			geoSmart.info = queryResults.getInfo(i);
			geoSmart.georesource = resource;
			

			layers.add(geoSmart);
			toAdd.add(resource);
		}
		
		StringBuilder layerErrors = new StringBuilder();
		
		AddLayersCommand addCommand = null;
		ReferencedEnvelope zoomToBounds = null;
		
		if (!toAdd.isEmpty()) {
			addCommand = new AddLayersCommand(toAdd, renderedMap.getMapLayers().size());
			executeCommmand(renderedMap, addCommand);
			
			for (Layer l : addCommand.getLayers()) {
				// setup name and style
				for (GeoSmart smrt : layers) {
					if (smrt.georesource != null && smrt.georesource.equals(l.getGeoResource())) {
						
						l.setName(smrt.info.getLayerName());
						if (smrt.info.getMapStyleBlackboard() != null){
							StyleBlackboard sb = smrt.info.getMapStyleBlackboard();
							l.getStyleBlackboard().clear();
							l.getStyleBlackboard().addAll(sb);
						}else if (smrt.info.getMapStyle() != null) {
							// use user defined style
							StyleBlackboard sb = BirtMapUtils.parseStyleString(smrt.info.getMapStyle());
							if (sb != null) {
								l.getStyleBlackboard().clear();
								l.getStyleBlackboard().addAll(sb);
							}
						}
						
						if (smrt.info.getIncludeZoom()) {
							if (zoomToBounds == null) {
								zoomToBounds = l.getBounds(new NullProgressMonitor(), renderedMap.getViewportModel().getCRS());
							}else {
								zoomToBounds.expandToInclude(l.getBounds(new NullProgressMonitor(), renderedMap.getViewportModel().getCRS()));
							}
						}
					}
				}
				if (l.getGeoResource().getMessage() != null) {
					layerErrors.append(l.getName() + ": " + l.getGeoResource().getMessage().getMessage()); //$NON-NLS-1$
					layerErrors.append("\n"); //$NON-NLS-1$
				}
			}
		}
	
		BoundsSetting bounds = mapItem.getMapBounds();
		if (bounds == null) bounds = new BoundsSetting();
		
		if(bounds.getOption() == BoundsOption.MAP_EXTENTS) {
			if (renderedMap.getMapLayers().isEmpty()) {
				zoomToBounds = new ReferencedEnvelope(renderedMap.getViewportModel().getCRS());
			}else {
				zoomToBounds = renderedMap.getBounds(new NullProgressMonitor());
			}
		} else if (bounds.getOption() == BoundsOption.CUSTOM) {
			zoomToBounds = bounds.getEnvelope();
			
		} else if (bounds.getOption() == BoundsOption.ALL_QUERY_LAYERS){
			for (Layer l : addCommand.getLayers()) {
				if (zoomToBounds == null) {
					zoomToBounds = l.getBounds(new NullProgressMonitor(), renderedMap.getViewportModel().getCRS());
				}else {
					zoomToBounds.expandToInclude(l.getBounds(new NullProgressMonitor(), renderedMap.getViewportModel().getCRS()));
				}
			}
			if (zoomToBounds == null) {
				//not layers; zoom to map extents
				if (renderedMap.getMapLayers().isEmpty()) {
					zoomToBounds = new ReferencedEnvelope(renderedMap.getViewportModel().getCRS());
				}else {
					zoomToBounds = renderedMap.getBounds(new NullProgressMonitor());
				}
			}
		} else if (bounds.getOption() == BoundsOption.LAYER){
			//set above
		}
		

		List<Layer> maplayers = ((Map)renderedMap).getLayersInternal();
		
		//scale style to match map dpi settings (scales symbols and fonts)
		int newdpi = mapItem.getDPI();
		double scale = newdpi / (double)defaultDpi;
		for (Layer l : maplayers) {
			for (StyleEntry cc : l.getStyleBlackboard().getContent()) {
				if ( cc.getStyle() != null && cc.getStyle() instanceof Style) {
					Style style = (Style)cc.getStyle();
					RescaleStyleVisitor scaledstyle = new RescaleStyleVisitor(scale);
					style.accept(scaledstyle);
					cc.setStyle(scaledstyle.getCopy());
				}
			}
			
		}
		
		// --  reorder layers so mapgraphic layers are at the top		
		List<Layer> orderedLayers = new ArrayList<Layer>();
		int cnt = 0;
		for (Layer l : maplayers) {
			if (l.getGeoResource().canResolve(MapGraphic.class)) {
				orderedLayers.add(l);
			} else {
				orderedLayers.add(cnt, l);
				cnt++;
			}
		}
		((MapImpl) renderedMap).getLayersInternal().clear();
		((MapImpl) renderedMap).getLayersInternal().addAll(0, orderedLayers);
		((BirtMapViewportModelImpl)renderedMap.getViewportModel()).setBounds(zoomToBounds);

		//draw map
		BufferedImage image = new BufferedImage(iwidth, iheight, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		
		try {
			drawMap(g, new java.awt.Dimension(iwidth, iheight), renderedMap, localdpi);
			if (layerErrors.length() > 0){
				addErrorMessage(g, layerErrors.toString(), iwidth, iheight); 
			}
			for (Throwable t : queryResults.getExceptions()){
				addErrorMessage(g, t.getMessage(), iwidth, iheight); 
			}
			
		} finally {
			g.dispose();
		}			
		
		//dispose of all resources used in map
		for (Layer l : orderedLayers){
			l.getGeoResource().dispose(new NullProgressMonitor());
		}
		cleanUp(layers);
		return writeImage(image);
	}
	
	/*
	 * looksup the basemap definition basemap is the hex encoded uuid
	 */
	private BasemapDefinition getBasemap(String basemap) {
		if (basemap == null || basemap.length() == 0) {
			return null;
		}
		// we do not close the session as we assume this session object
		// is managed by the SmartConnection
		Session session = (Session)context.getAppContext().get(BirtConstants.SESSION_PARAM);
		if (basemap.equals(SmartMapItem.DEFAULT_BASEMAP_KEY)) {
			ConservationArea reportca = (ConservationArea) context.getAppContext().get(BirtConstants.CA_PARAM);
			
			List<BasemapDefinition> defaultmap = 
					QueryFactory.buildQuery(session, BasemapDefinition.class, 
							new Object[] {"conservationArea", reportca},  //$NON-NLS-1$
							new Object[] {"isDefault", true}).getResultList(); //$NON-NLS-1$
			
			if (defaultmap.size() > 0){
				return (BasemapDefinition) defaultmap.get(0);
			}
		}

		UUID uuid = null;
		try {
			uuid = UuidUtils.stringToUuid(basemap);
		} catch (Exception ex) {
			// eatme
		}
		if (uuid != null) {
			return (BasemapDefinition)session.get(BasemapDefinition.class, uuid);
		}
		return null;
	}
	/*
	 * write image to byte array and returns associated stream
	 */
	private ByteArrayOutputStream writeImage(BufferedImage image)
			throws IOException {
		ImageIO.setUseCache(false);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
			ImageIO.write(image, "png", ios); //$NON-NLS-1$
			ios.flush();
		}
		return baos;
	}
	
	/*
	 * Executes map command
	 */
	private void executeCommmand(IMap map, MapCommand command) throws Exception{
		command.setMap(map);
		command.run(new NullProgressMonitor());
	}
	
	
	protected IMap drawMap(Graphics2D g, java.awt.Dimension dimension,
			IMap renderedMap, int dpi) throws Exception {

		final Map map = (Map) renderedMap;
		
		List<Layer> layersToRender = map.getLayersInternal();

		ReferencedEnvelope bounds = (ReferencedEnvelope) renderedMap.getViewportModel().getBounds();
		ReferencedEnvelope boundsCopy = new ReferencedEnvelope(bounds);
		RenderContext tools = configureMapForRendering(map,dimension, dpi, boundsCopy);

		RendererCreator decisive = new RendererCreatorImpl();
		decisive.setContext(tools);
		decisive.getLayers().addAll(layersToRender);
		SortedSet<RenderContext> sortedContexts = new TreeSet<RenderContext>(decisive.getConfiguration());
		render(g, dimension, decisive, sortedContexts);
		return renderedMap;
	}
	
	//copied from Application.GIS drawMap
		private void render(Graphics2D g, Dimension displaySize, RendererCreator decisive,
				SortedSet<RenderContext> sortedContexts)
				throws InvocationTargetException {

			RenderContext mainContext = decisive.getContext();

			ILabelPainter labelPainter = mainContext.getLabelPainter();
			labelPainter.clear();
			labelPainter.start();

			Iterator<RenderContext> iter = sortedContexts.iterator();
			while (iter.hasNext()) {
				RenderContext context = (RenderContext) iter.next();

				ILayer layer = context.getLayer();
				String layerId = getLayerId(layer);

				if (!(layer instanceof SelectionLayer)) {
					labelPainter.startLayer(layerId);
				}
				try {
					if (context instanceof CompositeRenderContext) {
						CompositeRenderContext compositeContext = (CompositeRenderContext) context;
						List<ILayer> layers = compositeContext.getLayers();
						boolean visible = false;
						for (ILayer tmpLayer : layers) {
							visible = visible || tmpLayer.isVisible();
						}
						if (!visible)
							continue;
					} else if (!layer.isVisible())
						continue;
					Renderer renderer = decisive.getRenderer(context);
					try {
						Graphics2D graphics = (Graphics2D) g.create();
						renderer.render(graphics, new NullProgressMonitor());
					} catch (RenderException e) {
						throw new InvocationTargetException(e);
					}
				} finally {
					labelPainter.endLayer(layerId, g, new Rectangle(
							displaySize));
				}
			}
			labelPainter.end(g, new Rectangle(displaySize));
			labelPainter.clear();
		}

		private String getLayerId(ILayer layer) {
			String layerId = layer.getID().toString();
			if (layer instanceof SelectionLayer)
				layerId = layerId + "-Selection"; //$NON-NLS-1$
			return layerId;
		}
		
		public static RenderContext configureMapForRendering(Map map, final Dimension destinationSize, final int dpi, ReferencedEnvelope baseMapBounds) {
			RenderManager manager = RenderFactory.eINSTANCE.createRenderManagerViewer();
			map.setRenderManagerInternal(manager);

		    RenderContext tools = new RenderContextImpl();
		    tools.setMapInternal(map);
		    tools.setRenderManagerInternal(manager);
		    manager.setMapInternal(map);
		    
		    manager.setMapDisplay(new IMapDisplay(){
		            public java.awt.Dimension getDisplaySize() {
		                return new java.awt.Dimension(destinationSize.width,
		                        destinationSize.height);
		            }

		            public int getWidth() {
		                return destinationSize.width;
		            }

		            public int getHeight() {
		                return destinationSize.height;
		            }

		            public int getDPI() {
		                return dpi;
		            }
		        });

		    ViewportModel model = map.getViewportModelInternal();
		    manager.setViewportModelInternal(model);
		    model.setCRS(map.getViewportModel().getCRS());
		    model.zoomToBox(map.getViewportModel().getBounds());
		    model.setMapInternal(map);
		    model.sizeChanged(new MapDisplayEvent(null, new java.awt.Dimension(0, 0),
		                        new java.awt.Dimension(destinationSize.width,
		                        	destinationSize.height)));
		    return tools;
		}
	
		// draws the error string on the map starting at 0,0;
		// can be a multi line string but will
		// truncate any one string that is longer than the width
		private void addErrorMessage(Graphics2D g, String error, int width,
				int height) {
			g.setFont(g.getFont().deriveFont(Font.BOLD));
			if (error == null || error.isEmpty()) error = "Unknown Error"; //$NON-NLS-1$
			String[] bits = error.split("\n"); //$NON-NLS-1$

			int fh = g.getFontMetrics().getHeight();
			int startx = 0;
			int maxwidth = 0;
			for (String bit : bits) {
				double fw = g.getFontMetrics().getStringBounds(bit, g).getWidth();
				if (fw > maxwidth) {
					maxwidth = (int) fw;
				}
			}
			if (maxwidth > width) {
				maxwidth = width;
			}

			int starty = 0;
			g.setColor(new Color(255, 255, 255, 230));
			g.fillRect(startx, starty, maxwidth + 3, fh * bits.length + 3);

			g.setColor(Color.RED);
			int y = starty + fh;
			for (String bit : bits) {
				g.drawString(bit, startx + 2, y);
				y += fh;
			}
		}
	
}
