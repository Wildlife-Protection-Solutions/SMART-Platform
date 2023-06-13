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
package org.wcs.smart.query.common.model;

import org.wcs.smart.query.model.IPagedQuery;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * A class to represent an observation query.
 * <p>Observation queries query each observation
 * which consists of a category and a 
 * set of attributes.</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
@MappedSuperclass
public abstract class ObservationQuery extends SimpleQuery implements IPagedQuery, IColumnAutoConfigQuery {

	private static final long serialVersionUID = 1L;
	
	private boolean showDataColumnsOnly = true;

	/**
	 * Creates a new observation query with the default
	 * conservation area filter and no date filter
	 */
	protected ObservationQuery(){
		super();
	}

	@Column(name = "show_data_columns_only")
	public boolean isShowDataColumnsOnly() {
		return showDataColumnsOnly;
	}

	public void setShowDataColumnsOnly(Boolean showDataColumnsOnly) {
		this.showDataColumnsOnly = Boolean.TRUE.equals(showDataColumnsOnly); //null <==> false
	}

}
