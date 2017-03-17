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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
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
import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.report.engine.content.IForeignContent;
import org.eclipse.birt.report.engine.extension.IRowSet;
import org.eclipse.birt.report.engine.extension.ReportItemPresentationBase;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.extension.ExtendedElementException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.mapgraphic.MapGraphic;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.command.MapCommand;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.StyleBlackboard;
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
import org.wcs.smart.birt.BirtConstants;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.report.birt.map.AddLayersCommand;
import org.wcs.smart.report.birt.map.BirtMapFactory;
import org.wcs.smart.report.birt.map.BirtMapUtils;
import org.wcs.smart.report.birt.map.BirtMapViewportModelImpl;
import org.wcs.smart.report.birt.map.MapLayerInfo;
import org.wcs.smart.report.birt.map.RestoreMapSettings;
import org.wcs.smart.report.birt.map.item.SmartMapItem;
import org.wcs.smart.report.birt.map.udig.MapGeoResource;
import org.wcs.smart.report.birt.map.udig.MapQueryService;
import org.wcs.smart.udig.catalog.smart.IDatabaseConnectionProvider;
import org.wcs.smart.udig.catalog.smart.ISessionService;
import org.wcs.smart.udig.catalog.smart.SmartService;
import org.wcs.smart.udig.catalog.smart.SmartServiceExtension;
import org.wcs.smart.util.UuidUtils;

/**
 * For rendering map in a report.
 * 
 * @author Emily
 *
 */
public class SmartMapPresentationImpl extends ReportItemPresentationBase {

	public static final int MIN_DPI = 72;
	public static final int DEFAULT_DPI = 96;
	public static final int MAX_DPI = 300;
	
	private SmartMapItem mapItem;

	/**
	 * @see org.eclipse.birt.report.engine.extension.ReportItemPresentationBase#setModelObject(org.eclipse.birt.report.model.api.ExtendedItemHandle)
	 */
	public void setModelObject(ExtendedItemHandle modelHandle) {
		super.setModelObject(modelHandle);
		try {
			mapItem = (SmartMapItem) modelHandle.getReportItem();
		} catch (ExtendedElementException e) {
			Logger.getLogger(SmartMapPresentationImpl.class.getName()).log(Level.SEVERE, e.getMessage(), e);
		}
	}

	/**
	 * @see org.eclipse.birt.report.engine.extension.ReportItemPresentationBase#getOutputType()
	 */
	public int getOutputType() {
		return OUTPUT_AS_IMAGE;
	}

	/**
	 * create query for non-listing report item
	 * 
	 * @param item
	 *            report item
	 * @return a report query
	 */

	/**
	 * @see org.eclipse.birt.report.engine.extension.ReportItemPresentationBase#onRowSets(org.eclipse.birt.report.engine.extension.IRowSet[])
	 */
	public Object onRowSets(IRowSet[] rowSets) throws BirtException {
		
		int localdpi = 96;
		if (mapItem.getDPI() != null){
			localdpi = mapItem.getDPI();
			if (localdpi < 72 || localdpi > 4000) localdpi = 96;
		}
			
		int iwidth = BirtMapUtils.getWidthInPx(modelHandle, localdpi);
		int iheight = BirtMapUtils.getHeightInPx(modelHandle, localdpi);

		List<GeoSmart> layers = new ArrayList<GeoSmart>();
		try {
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
			MapConfiguration queryResults = (MapConfiguration) ((IForeignContent) content).getRawValue();

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
			
			AddLayersCommand cmd = new AddLayersCommand(toAdd, renderedMap.getMapLayers().size());
			executeCommmand(renderedMap, cmd);
			StringBuilder layerErrors = new StringBuilder();
			for (Layer l : cmd.getLayers()) {
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
					}
				}
				if (l.getGeoResource().getMessage() != null) {
					layerErrors.append(l.getName() + ": " + l.getGeoResource().getMessage().getMessage()); //$NON-NLS-1$
					layerErrors.append("\n"); //$NON-NLS-1$
				}
			}
		
			ReferencedEnvelope bounds = null;
			if (mapItem.getMapBounds() == null) {
				bounds = renderedMap.getBounds(new NullProgressMonitor());
			} else {
				bounds = mapItem.getMapBounds();
			}
			
			// --  reorder layers so mapgraphic layers are at the top
			List<Layer> maplayers = ((Map)renderedMap).getLayersInternal();
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
			((BirtMapViewportModelImpl)renderedMap.getViewportModel()).setBounds(bounds);

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
			return writeImage(image);

		} catch (Exception ex) {
			Logger.getLogger(SmartMapPresentationImpl.class.getName()).log(Level.SEVERE, "BIRT Map Rendering Error", ex); //$NON-NLS-1$
			try {
				BufferedImage image = new BufferedImage(iwidth, iheight,BufferedImage.TYPE_INT_ARGB);
				Graphics2D g = image.createGraphics();
				try {
					addErrorMessage(g,
							"Map Errors:\n" + ex.getMessage(), iwidth, iheight); //$NON-NLS-1$
				} finally {
					g.dispose();
				}
				return writeImage(image);
			} catch (Exception ex2) {
				Logger.getLogger(SmartMapPresentationImpl.class.getName()).log(Level.SEVERE, "BIRT Map Rendering Error", ex2); //$NON-NLS-1$
			}
			return null;
		} finally {
			cleanUp(layers);
		}
	}

	/*
	 * Executes map command
	 */
	private void executeCommmand(IMap map, MapCommand command) throws Exception{
		command.setMap(map);
		command.run(new NullProgressMonitor());
	}
	
	
	
	/*
	 * write image to byte array and returns associated stream
	 */
	private ByteArrayInputStream writeImage(BufferedImage image)
			throws IOException {
		ImageIO.setUseCache(false);
		
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
			ImageIO.write(image, "png", ios); //$NON-NLS-1$
			ios.flush();
			ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray());
			return bis;
		}
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
			List<?> defaultmap = session.createCriteria(BasemapDefinition.class)
					.add(Restrictions.eq("conservationArea", reportca)) //$NON-NLS-1$
					.add(Restrictions.eq("isDefault", true)).list(); //$NON-NLS-1$
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
	 * cleans up resources, removing from catalog as required
	 */
	private void cleanUp(List<GeoSmart> layers) {
		NullProgressMonitor monitor = new NullProgressMonitor();
		for (GeoSmart layer : layers) {
			if (layer.georesource != null) {
				layer.georesource.dispose(monitor);
			}
		}
	}

	// structure for aggregating smart
	class GeoSmart {
		DataSetHandle handle;
		IGeoResource georesource;
		Layer layer;
		MapLayerInfo info;
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
}