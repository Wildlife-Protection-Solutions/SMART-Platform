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
package org.wcs.smart.udig.legend;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.udig.legend.ui.LegendGraphic;
import org.locationtech.udig.mapgraphic.MapGraphic;
import org.locationtech.udig.mapgraphic.MapGraphicContext;
import org.locationtech.udig.mapgraphic.internal.MapGraphicContextImpl;
import org.locationtech.udig.project.IBlackboard;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.render.impl.RendererImpl;
import org.locationtech.udig.project.render.ICompositeRenderContext;
import org.locationtech.udig.project.render.IMultiLayerRenderer;
import org.locationtech.udig.project.render.IRenderContext;
import org.locationtech.udig.project.render.RenderException;

/**
 * A custom mapgraphic renderer that attempts to render mapgraphics without
 * using display thread.
 * 
 */
public class SmartMapgraphicRenderer extends RendererImpl implements
		IMultiLayerRenderer {

	public static final String BLACKBOARD_IMAGE_KEY = "CACHED_IMAGE"; //$NON-NLS-1$
	public static final String BLACKBOARD_IMAGE_BOUNDS_KEY = "CACHED_IMAGE_BOUNDS"; //$NON-NLS-1$

	@Override
	public String getName() {
		return super.getName();
	}

	/*
	 * Renders the mapgraphic for the entire screen
	 * 
	 * @returns array[0] = new image; array[1] = image bounds
	 */
	private Object[] backgroundRenderImage(ICompositeRenderContext context,
			List<IOException> exceptions) {
		BufferedImage cache = new BufferedImage(context.getMapDisplay()
				.getWidth(), context.getMapDisplay().getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		ReferencedEnvelope imageBounds = context.getViewportModel().getBounds();

		for (IRenderContext l : context.getContexts()) {
			Graphics2D copy = (Graphics2D) cache.createGraphics();
			// final NonDisposableGraphics graphics = new
			// NonDisposableGraphics(copy);
			try {
				if (!l.getLayer().isVisible())
					continue;
				MapGraphic mg = l.getGeoResource().resolve(MapGraphic.class,
						null);
				MapGraphicContext mgContext = new MapGraphicContextImpl(l, copy);
				if (mg instanceof LegendGraphic) {
					(new LegendGraphicWriter()).draw(mgContext);
				} else {
					mg.draw(mgContext);
				}
			} catch (IOException e) {
				exceptions.add(e);
			} finally {
				copy.dispose();
			}
			setState(RENDERING);
		}
		return new Object[] { cache, imageBounds };
	}

	/**
	 * @see org.locationtech.udig.project.internal.render.impl.RendererImpl#render(java.awt.Graphics2D,
	 *      IProgressMonitor)
	 * @param destination
	 */
	@Override
	public void render(Graphics2D destination, IProgressMonitor monitor) {
		/*
		 * entry point for printing cannot used cached image when printing; so
		 * draw directly to destination
		 */

		List<IOException> exceptions = new ArrayList<IOException>();
		for (IRenderContext l : getContext().getContexts()) {
			Graphics2D copy = (Graphics2D) destination.create();
			// final NonDisposableGraphics graphics = new
			// NonDisposableGraphics(copy);
			try {
				if (!l.getLayer().isVisible())
					continue;
				MapGraphic mg = l.getGeoResource().resolve(MapGraphic.class,
						null);
				MapGraphicContext mgContext = new MapGraphicContextImpl(l,
						destination);
				if (mg instanceof LegendGraphic) {
					(new LegendGraphicWriter()).draw(mgContext);
				} else {
					mg.draw(mgContext);
				}
			} catch (IOException e) {
				exceptions.add(e);
			} finally {
				copy.dispose();
			}
			setState(RENDERING);
		}
		if (!exceptions.isEmpty()) {
			RenderException exception = new RenderException(
					exceptions.size()
							+ " exceptions we raised while drawing map graphics", exceptions.get(0)); //$NON-NLS-1$
			exception.fillInStackTrace();
		}
		setState(DONE);

	}

	/**
	 * @see org.locationtech.udig.project.internal.render.impl.RendererImpl#getContext()
	 */
	@Override
	public synchronized ICompositeRenderContext getContext() {
		return (ICompositeRenderContext) super.getContext();
	}

	@Override
	public synchronized void setContext(IRenderContext newContext) {
		super.setContext(newContext);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.locationtech.udig.project.render.Renderer#render(com.vividsolutions
	 * .jts.geom.Envelope)
	 */
	@Override
	public void render(IProgressMonitor monitor) {
		// this is the entry point for non printing
		/*
		 * For non-printing we will have a layer and can used the cached image
		 * on the layer.
		 */
		List<IOException> exceptions = new ArrayList<IOException>();
		ILayer layer = context.getLayer();
		IBlackboard blackboard = layer.getBlackboard();
		BlackboardItem cached = (BlackboardItem) blackboard
				.get(BLACKBOARD_IMAGE_KEY);
		if (cached != null) {
			if (!cached.layersEqual(getContext().getLayers())) {
				cached = null;
			}
		}

		if (cached == null) {
			Object values[] = backgroundRenderImage(getContext(), exceptions);
			cached = new BlackboardItem((BufferedImage) values[0],
					(ReferencedEnvelope) values[1], getContext().getLayers());
			layer.getBlackboard().put(BLACKBOARD_IMAGE_KEY, cached);

		}
		BufferedImage cache = cached.image;
		ReferencedEnvelope imageBounds = cached.env;

		// we need to extract from the cache which is the size of the map
		// display
		// the part of the image which is appropriate for the given bounds
		ReferencedEnvelope request = getContext().getImageBounds();

		double pixelperunitx = cache.getWidth() / imageBounds.getWidth();
		double pixelperunity = cache.getHeight() / imageBounds.getHeight();

		int lx = (int) Math.round((request.getMinX() - imageBounds.getMinX())
				* pixelperunitx);
		int ly = (int) Math.round((request.getMaxY() - imageBounds.getMaxY())
				* pixelperunity);

		AffineTransform transform = new AffineTransform(1f, 0f, 0f, 1f, -lx, ly);
		Graphics2D denstination = getContext().getImage().createGraphics();
		denstination.drawImage(cache, transform, null);

		if (!exceptions.isEmpty()) {
			RenderException exception = new RenderException(
					exceptions.size()
							+ " exceptions we raised while drawing map graphics", exceptions.get(0)); //$NON-NLS-1$
			exception.fillInStackTrace();
		}
		setState(DONE);
	}

	@Override
	public boolean isCacheable() {
		return false;
	}

	@Override
	public void setState(int newState) {
		if (newState == RENDER_REQUEST) {
			getContext().getLayer().getBlackboard().put(BLACKBOARD_IMAGE_KEY, null);
		}
		super.setState(newState);
	}

	@Override
	public void refreshImage() throws RenderException {
		getContext().clearImage();
		render(new NullProgressMonitor());
	}

	static class BlackboardItem {
		BufferedImage image;
		ReferencedEnvelope env;
		Collection<ILayer> layers;

		public BlackboardItem(BufferedImage image, ReferencedEnvelope env,
				Collection<ILayer> layers) {
			this.image = image;
			this.env = env;
			this.layers = layers;
		}

		public boolean layersEqual(Collection<ILayer> layers) {
			if (this.layers.size() != layers.size())
				return false;
			for (Iterator<ILayer> iterator = this.layers.iterator(); iterator
					.hasNext();) {
				ILayer layer = (ILayer) iterator.next();
				if (!layers.contains(layer))
					return false;
			}
			return true;
		}

	}
}
