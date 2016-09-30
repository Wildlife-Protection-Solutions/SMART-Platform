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
package org.wcs.smart.i2.udig;

import org.eclipse.emf.common.util.EList;
import org.geotools.data.Query;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.internal.impl.LayerImpl;
import org.opengis.filter.Filter;

/**
 * A custom uDig layer that allows for the content of the layer to be filtered.
 * @author Emily
 *
 */
public class ContentFilterLayerImpl extends LayerImpl {

	private Filter contentFilter;
	
	public ContentFilterLayerImpl(){
		
	}
	
	public void setContentFilter(Filter contentFilter){
		this.contentFilter = contentFilter;
	}
	
	public Filter getContentFilter(){
		return this.contentFilter;
	}

	public void setGeoResources(EList<IGeoResource> resources){
		geoResources = resources;
	}
	
	@Override
	public Query getQuery(boolean selection) {
		try {
			if (selection) {
				return new Query(getSchema().getName().getLocalPart(), getFilter());
			} else {
				if (getContentFilter() == null) return Query.ALL;
				return new Query(getSchema().getName().getLocalPart(), getContentFilter());
			}
		} catch (Exception e) {
			if (selection) {
				Query q = new Query();
				q.setFilter(Filter.EXCLUDE);
				return q;
			} else {
				return Query.ALL;
			}
		}
	}
}
