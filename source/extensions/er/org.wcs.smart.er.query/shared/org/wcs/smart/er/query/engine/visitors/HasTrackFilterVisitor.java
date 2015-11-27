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
package org.wcs.smart.er.query.engine.visitors;

import org.wcs.smart.er.query.filter.SamplingUnitAttributeFilter;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.filter.SamplingUnitFilter.Source;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;

/**
 * Has track filter visitor.  Determines if a filter
 * requires the track to execute the query.
 * 
 * @author Emily
 *
 */
public class HasTrackFilterVisitor implements IFilterVisitor{
	private boolean hasTrack = false;

	@Override
	public void visit(IFilter filter) {
		if (hasTrack)
			return;
		if (filter instanceof SamplingUnitFilter) {
			if (((SamplingUnitFilter) filter).getSource() == Source.TRACK) {
				hasTrack = true;
			}
		} else if (filter instanceof SamplingUnitAttributeFilter) {
			if (((SamplingUnitAttributeFilter) filter).getSource() == Source.TRACK) {
				hasTrack = true;
			}
		}
	}
	
	public boolean hasTrack(){
		return this.hasTrack;
	}
}
