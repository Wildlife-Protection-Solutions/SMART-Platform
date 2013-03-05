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
package org.wcs.smart.query.parser.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.parser.internal.filter.AttributeInfo;
import org.wcs.smart.query.parser.internal.filter.IFilter;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.util.SmartUtils;

/**
 * A Conservation Area filter.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ConservationAreaFilter implements IFilter {

	/**
	 * Parses a conservation area filter from
	 * the string representation of the filter
	 * 
	 * @param caFilterAsString
	 * @return
	 */
	public static ConservationAreaFilter parseFilter(String caFilterAsString) {
		ConservationAreaFilter filter = new ConservationAreaFilter();
		try {
			String[] bits = caFilterAsString.split(","); //$NON-NLS-1$
			for (int i = 0; i < bits.length; i++) {
				filter.addConservationArea(SmartUtils.decodeHex(bits[i].trim()));

			}
		} catch (Exception ex) {
			throw new IllegalStateException(
					Messages.ConservationAreaFilter_InvalidCaFilter, ex);
		}
		return filter;
	}
	
	private ArrayList<byte[]> filters = new ArrayList<byte[]>();

	/**
	 * Creates a new empty conservation area filter
	 */
	public ConservationAreaFilter(){
		
	}
	
	
	/**
	 * 
	 * @return the ids of the conservation areas in the filter
	 */
	public ArrayList<byte[]> getConservationAreaFilterIds(){
		return filters;
	}
	
	/**
	 * Creates a new default conservation area filter.
	 * <p>By default this filter includes the logged in conservation
	 * area for single analysis, or all selected conservation areas
	 * for multiple ca analysis</p>
	 * 
	 * 
	 */
	public ConservationAreaFilter(boolean init){
		this();
		if (init){
			if (!SmartDB.isMultipleAnalysis()){
				addConservationArea(SmartDB.getCurrentConservationArea());
			}else{
				for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
					addConservationArea(ca);
				}
			}
		}
	}
	
	/**
	 * Adds a conservation area to the filter
	 * @param newCa conservation area
	 */
	public void addConservationArea(ConservationArea newCa){
		filters.add(newCa.getUuid());
	}
	
	/**
	 * Adds a conservation area uuid to the filter
	 * @param uuid the conservation area uuid
	 */
	public void addConservationArea(byte[] uuid){
		filters.add(uuid);
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#asString()
	 */
	@Override
	public String asString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < filters.size(); i ++){
			if (i != 0){
				sb.append(","); //$NON-NLS-1$
			}
			sb.append( SmartUtils.encodeHex( filters.get(i) ) );
		}
		return sb.toString();
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#asSql(java.util.HashMap)
	 */
	@Override
	public String asSql(HashMap<Class<?>, String> tableMapping) {
		return asSql(tableMapping.get(Patrol.class));
	}

	
	/**
	 * Creates the sql given the provided conservation
	 * area class prefix.  Assumes the column name
	 * is ca_uuid;
	 * 
	 * @param caClassPrefix
	 * @return
	 */
	public String asSql(String caClassPrefix) {
		if (filters.size() == 0){
			return ""; //$NON-NLS-1$
		}
		StringBuilder sb = new StringBuilder();
		sb.append(caClassPrefix);
		sb.append(".ca_uuid IN ("); //$NON-NLS-1$
		for (int i = 0; i < filters.size(); i++) {
			if (i != 0){
				sb.append(","); //$NON-NLS-1$
			}
			String uuid = SmartUtils.encodeHex(filters.get(i));
			sb.append("x'" + uuid + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		sb.append(")"); //$NON-NLS-1$
		return sb.toString();
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return false;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#getAttributeFilters(java.util.HashSet)
	 */
	@Override
	public void getAttributeFilters(HashSet<AttributeInfo> attributes) {
	}
	
	/**
	 * There are no drop items for conservation area filters
	 * @return null
	 */
	@Override
	public DropItem[] getDropItems(Session session) throws Exception{
		return null;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.filter.IFilter#getChildren()
	 */
	@Override
	public List<IFilter> getChildren() {
		return null;
	}	
}
