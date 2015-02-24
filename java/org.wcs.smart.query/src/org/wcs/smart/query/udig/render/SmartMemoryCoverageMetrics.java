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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.locationtech.udig.core.MinMaxScaleCalculator;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.render.AbstractRenderMetrics;
import org.locationtech.udig.project.render.IRenderContext;
import org.locationtech.udig.project.render.IRenderMetricsFactory;
import org.locationtech.udig.style.sld.SLDContent;
import org.locationtech.udig.ui.graphics.SLDs;

import org.geotools.styling.Style;
import org.geotools.util.Range;

/**
 * Creates a Metrics object for the smart gridcoverage renderer
 * 
 */
public class SmartMemoryCoverageMetrics extends AbstractRenderMetrics {

    /*
     * list of styles the renderer is expecting to find and use
     */
    private static List<String> listExpectedStyleIds(){
        ArrayList<String> styleIds = new ArrayList<String>();
        styleIds.add(SLDContent.ID);   
        styleIds.add(SmartGridCellStyleContent.STYLE_ID);
        return styleIds;
    }
    
    /**
     * Construct <code>BasicGridCoverageMetrics</code>.
     *
     * @param context2
     * @param factory
     */
    public SmartMemoryCoverageMetrics( IRenderContext context2, SmartMemoryGridCoverageMetricsFactory factory) {
        super( context2, factory, listExpectedStyleIds());
		this.resolutionMetric = RES_PIXEL; // reads more then is required for the screen!
		this.latencyMetric = LATENCY_MEMORY_CACHE;
		this.timeToDrawMetric = DRAW_IMAGE_MEMORY;	
    }

    @Override
    public SmartMemoryGridCoverageRenderer createRenderer() {
        SmartMemoryGridCoverageRenderer r = new SmartMemoryGridCoverageRenderer();
        r.setContext(context);
        return r;
    }

    /**
     * @see org.locationtech.udig.project.render.RenderMetrics#getRenderContext()
     */
    @Override
    public IRenderContext getRenderContext() {
        return context;
    }

    /**
     * @see org.locationtech.udig.project.render.IRenderMetrics#getRenderMetricsFactory()
     */
    @Override
    public IRenderMetricsFactory getRenderMetricsFactory() {
        return factory;
    }

    @Override
    public boolean canAddLayer( ILayer layer ) {
        return false;
    }

    @Override
    public boolean canStyle( String styleID, Object value ) {
    	if (styleID.equals(SmartGridCellStyleContent.STYLE_ID)) return true;
    	
    	if( value != null && value instanceof Style){              
            Style style = (Style) value;
            return SLDs.rasterSymbolizer( style ) != null;
        }
        return false;
    }

    @Override
    public Set<Range<Double>> getValidScaleRanges() {
        Object value = context.getLayer().getStyleBlackboard().get(SLDContent.ID);
        if( value == null ) {
            return new HashSet<Range<Double>>();
        }
        if( value instanceof Style ){
            Style style = (Style) value;
            return MinMaxScaleCalculator.getValidScaleRanges(style);
        }
        else {           
            return new HashSet<Range<Double>>();
        }
    }

}
