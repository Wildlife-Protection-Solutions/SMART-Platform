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
package org.wcs.smart.query.parser.internal;

import java.util.Arrays;
import java.util.HashMap;

import org.wcs.smart.SmartUtils;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.patrol.model.Patrol;

/**
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author Emily
 * @since 1.0.0
 */
public class ConservationAreaFilter implements Filter {
	private ConservationArea[] conservationAreaFilter = {};

	
	public ConservationAreaFilter(){
		
	}
	public void addConservationArea(ConservationArea newCa){
		conservationAreaFilter = Arrays.copyOf(conservationAreaFilter, conservationAreaFilter.length + 1);
		conservationAreaFilter[conservationAreaFilter.length - 1] = newCa;
	}
	
	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#asString()
	 */
	@Override
	public String asString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (int i = 0; i < conservationAreaFilter.length; i ++){
			if (i != 0){
				sb.append(" OR ");
			}
		
			sb.append(" CA = '" + conservationAreaFilter[i].getId());
		}			
		sb.append(")");
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#asHql(java.util.HashMap, java.util.HashMap)
	 */
	@Override
	public String asHql(HashMap<Class<?>, String> tableMapping,
			HashMap<String, Object> parameters) {
		// TODO Auto-generated method stub
		throw new IllegalStateException("Not implemented.");
//		return null;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#asSql(java.util.HashMap)
	 */
	@Override
	public String asSql(HashMap<Class<?>, String> tableMapping) {
		if (conservationAreaFilter.length == 0){
			return "";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(tableMapping.get(Patrol.class));
		sb.append(".ca_uuid IN (");
		for (int i = 0; i < conservationAreaFilter.length; i++) {
			String uuid = SmartUtils.encodeHex(conservationAreaFilter[i].getUuid());
//			String uuid = Arrays.toString(conservationAreaFilter[i].getUuid()).replaceAll(" ", "").replaceAll(",", "").replaceAll("\\\\[", "").replaceAll("\\\\]", "");
			sb.append("x'" + uuid + "'");
		}
		sb.append(")");
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeTreeItemFilter()
	 */
	@Override
	public boolean hasAttributeTreeItemFilter() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeListItemFilter()
	 */
	@Override
	public boolean hasAttributeListItemFilter() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.query.parser.internal.Filter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return false;
	}
}
