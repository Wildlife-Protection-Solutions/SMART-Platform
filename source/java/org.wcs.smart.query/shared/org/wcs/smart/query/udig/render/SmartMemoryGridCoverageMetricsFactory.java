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
package org.wcs.smart.query.udig.render;

import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.render.AbstractRenderMetrics;
import org.locationtech.udig.project.render.ICompositeRenderContext;
import org.locationtech.udig.project.render.IRenderContext;
import org.locationtech.udig.project.render.IRenderMetricsFactory;
import org.locationtech.udig.project.render.IRenderer;
import org.opengis.coverage.grid.GridCoverage;
import org.wcs.smart.query.model.Query;



/**
 * The Render metrics factory for grid coverage renderer that includes
 * cell bounds.
 *
 * This renderer is only valid for georesources that can resolve to a GridCoverage and Smart 
 * Query.
 */
public class SmartMemoryGridCoverageMetricsFactory implements IRenderMetricsFactory {

    /**
     * Ensures that we can get an AbstractGridCoverage2DReader out of this class.
     * 
     * @see org.locationtech.udig.project.render.RenderMetricsFactory#canRender(org.locationtech.udig.project.render.RenderTools)
     * @param context
     * @return true if we can render the provided context using BasicGridCoverageRenderer
     */
    public boolean canRender( IRenderContext context ) {
        if( context instanceof ICompositeRenderContext ){
            return false;
        }
        IGeoResource geoResource = context.getGeoResource();
        if (geoResource.canResolve(GridCoverage.class) && 
        		geoResource.canResolve(Query.class)){
        	return true;
        }
		return false;
    }

    /**
     * Strategy object used to indicate how well a renderer can draw.
     * 
     * @see org.locationtech.udig.project.render.RenderMetricsFactory#createMetrics(org.locationtech.udig.project.render.RenderTools)
     * @param context
     * @return render metrics for the provided context
     */
    public AbstractRenderMetrics createMetrics( IRenderContext context ) {
        return new SmartMemoryCoverageMetrics(context, this);
    }

    public Class< ? extends IRenderer> getRendererType() {
        return SmartMemoryGridCoverageRenderer.class;
    }

}
