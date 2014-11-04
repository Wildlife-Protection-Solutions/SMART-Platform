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
package org.wcs.smart.er.query.engine;

import org.wcs.smart.er.query.filter.SamplingUnitAttributeFilter;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.filter.SamplingUnitFilter.Source;
import org.wcs.smart.query.common.engine.visitors.HasObservationFilterVisitor;
import org.wcs.smart.query.model.filter.IFilter;

/**
 * Has observation filter visitor for survey visitors.
 * 
 * @author Emily
 *
 */
public class SurveyHasObservationFilterVisitor extends HasObservationFilterVisitor {

	private boolean hasSu = false;

	@Override
	public void visit(IFilter filter) {
		super.visit(filter);
		if (super.hasAttributeFilter() || super.hasCategoryFilter() || hasSu)
			return;
		if (filter instanceof SamplingUnitFilter) {
			if (((SamplingUnitFilter) filter).getSource() == Source.OBSERVATION) {
				hasSu = true;
			}
		} else if (filter instanceof SamplingUnitAttributeFilter) {
			if (((SamplingUnitAttributeFilter) filter).getSource() == Source.OBSERVATION) {
				hasSu = true;
			}
		}
	}

	/**
	 * 
	 * @return true if has sampling unit observation filter
	 */
	public boolean hasSamplingUnitObservationFilter() {
		return hasSu;
	}
	
	/**
	 * True if filter has category, attribute, observer or sampling unit observation
	 * filter.
	 * 
	 * @return true if the filter filters on any observeration related item
	 */
	public boolean hasObservationFilter(){
		return hasCategoryFilter() || 
				hasAttributeFilter() || 
				hasObserverFilter() || 
				hasSamplingUnitObservationFilter();
	}
}
