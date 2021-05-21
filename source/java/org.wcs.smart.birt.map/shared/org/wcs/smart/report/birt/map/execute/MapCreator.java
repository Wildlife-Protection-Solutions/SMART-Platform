package org.wcs.smart.report.birt.map.execute;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.command.navigation.ZoomCommand;
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

public enum MapCreator {
	INSTANCE;

	public void drawMap(Graphics2D g, java.awt.Dimension dimension, 
			IMap renderedMap, int dpi, ReferencedEnvelope re) throws Exception {
		final Map map = (Map) renderedMap;

		List<Layer> layersToRender = map.getLayersInternal();

		ReferencedEnvelope bounds = re;
		if (re == null) {
			bounds = (ReferencedEnvelope) renderedMap.getViewportModel().getBounds();
		}
		ReferencedEnvelope boundsCopy = new ReferencedEnvelope(bounds);
		RenderContext tools = configureMapForRendering(map, dimension, dpi, boundsCopy);

		RendererCreator decisive = new RendererCreatorImpl();
		decisive.setContext(tools);
		decisive.getLayers().addAll(layersToRender);
		SortedSet<RenderContext> sortedContexts = new TreeSet<RenderContext>(decisive.getConfiguration());
		
		if (re != null) renderedMap.sendCommandSync(new ZoomCommand(bounds));
		render(g, dimension, decisive, sortedContexts);
	}

	public void drawMap(Graphics2D g, java.awt.Dimension dimension, 
			IMap renderedMap, int dpi) throws Exception {

		final Map map = (Map) renderedMap;

		List<Layer> layersToRender = map.getLayersInternal();

		ReferencedEnvelope bounds = (ReferencedEnvelope) renderedMap.getViewportModel().getBounds();
		ReferencedEnvelope boundsCopy = new ReferencedEnvelope(bounds);
		RenderContext tools = configureMapForRendering(map, dimension, dpi, boundsCopy);

		RendererCreator decisive = new RendererCreatorImpl();
		decisive.setContext(tools);
		decisive.getLayers().addAll(layersToRender);
		SortedSet<RenderContext> sortedContexts = new TreeSet<RenderContext>(decisive.getConfiguration());
		render(g, dimension, decisive, sortedContexts);
	}

	// copied from Application.GIS drawMap
	private void render(Graphics2D g, Dimension displaySize, RendererCreator decisive,
			SortedSet<RenderContext> sortedContexts) throws InvocationTargetException {

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
				labelPainter.endLayer(layerId, g, new Rectangle(displaySize));
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

	public static RenderContext configureMapForRendering(Map map, final Dimension destinationSize, final int dpi,
			ReferencedEnvelope baseMapBounds) {
		RenderManager manager = RenderFactory.eINSTANCE.createRenderManagerViewer();
		map.setRenderManagerInternal(manager);

		RenderContext tools = new RenderContextImpl();
		tools.setMapInternal(map);
		tools.setRenderManagerInternal(manager);
		manager.setMapInternal(map);

		manager.setMapDisplay(new IMapDisplay() {
			public java.awt.Dimension getDisplaySize() {
				return new java.awt.Dimension(destinationSize.width, destinationSize.height);
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
				new java.awt.Dimension(destinationSize.width, destinationSize.height)));
		return tools;
	}
}
