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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.parser.internal.filter.AttributeInfo;
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
		if (caFilterAsString == null){
			filter.setIncludeAll(true);
		}else{
			filter.setIncludeAll(false);		
			try {
				String[] bits = caFilterAsString.split(","); //$NON-NLS-1$
				for (int i = 0; i < bits.length; i++) {
					try{
						filter.addConservationArea(SmartUtils.decodeHex(bits[i].trim()));
					}catch (Exception ex){
						//eatme
					}

				}
			
			} catch (Exception ex) {
				throw new IllegalStateException(
					Messages.ConservationAreaFilter_InvalidCaFilter, ex);
			}
		}
		ArrayList<byte[]> missing = new ArrayList<byte[]>();
		for (Iterator<byte[]> iterator = filter.caFilters.iterator(); iterator.hasNext();) {
			byte[] type = (byte[]) iterator.next();
			boolean found = false;
			for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
				if (Arrays.equals(type, ca.getUuid())){
					found = true;
					break;
				}
			}
			if (!found){
				missing.add(type);
				iterator.remove();
			}
			
		}
		if (missing.size() > 0){
			filter.setMissingConservationAreas(missing);
		}else{
			filter.setMissingConservationAreas(null);
		}
		return filter;
	}
	
	private ArrayList<byte[]> caFilters = new ArrayList<byte[]>();
	private List<byte[]> missingCas = null;
	
	private boolean includeAll = false;
	
	/**
	 * Creates a new empty conservation area filter
	 */
	public ConservationAreaFilter(){
		
	}
	
	public void refreshMissingList(){
		if (missingCas != null){
			caFilters.addAll(missingCas);
		}
		ArrayList<byte[]> missing = new ArrayList<byte[]>();
		for (Iterator<byte[]> iterator = caFilters.iterator(); iterator.hasNext();) {
			byte[] type = (byte[]) iterator.next();
			boolean found = false;
			for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
				if (Arrays.equals(type, ca.getUuid())){
					found = true;
					break;
				}
			}
			if (!found){
				missing.add(type);
				iterator.remove();
			}
			
		}
		if (missing.size() > 0){
			setMissingConservationAreas(missing);
		}else{
			setMissingConservationAreas(null);
		}
	}
	/**
	 * 
	 * @return the ids of the conservation areas in the filter
	 */
	public ArrayList<byte[]> getConservationAreaFilterIds(){
		return caFilters;
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
				setIncludeAll(true);
			}
		}
	}
	
	/**
	 * Adds a conservation area to the filter
	 * @param newCa conservation area
	 */
	public void addConservationArea(ConservationArea newCa){
		this.includeAll = false;
		caFilters.add(newCa.getUuid());
	}
	
	/**
	 * Adds a conservation area uuid to the filter
	 * @param uuid the conservation area uuid
	 */
	public void addConservationArea(byte[] uuid){
		this.includeAll = false;
		caFilters.add(uuid);
	}
	
	/**
	 * 
	 * @return true if all current conservation areas should be included
	 */
	public boolean includeAll(){
		return this.includeAll;
	}
	
	/**
	 * Sets if all current conservation areas should be included
	 * @param includeAll
	 */
	public void setIncludeAll(boolean includeAll){
		this.includeAll = includeAll;
	}
	
	/**
	 * 
	 * @return CA uuids that are in the current filter but 
	 * not the current list of CA's being analysed.  May return null
	 * if not Cas missing
	 */
	public List<byte[]> getMissingCas(){
		return missingCas;
	}
	
	/**
	 * Sets the CAs that are in the current filter but no in the 
	 * list of CA's being analysed.
	 * @param missing
	 */
	public void setMissingConservationAreas(List<byte[]> missing){
		this.missingCas = missing;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString() {
		if (includeAll){
			//if we are include all conservation areas then
			//filter is null
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < caFilters.size(); i ++){
			if (i != 0){
				sb.append(","); //$NON-NLS-1$
			}
			sb.append( SmartUtils.encodeHex( caFilters.get(i) ) );
		}
		return sb.toString();
	}

	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#asSql(java.util.HashMap)
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
		ArrayList<byte[]> localFilters = new ArrayList<byte[]>();
		if (includeAll){
			//include all current conservation areas
			if (SmartDB.getConservationAreaConfiguration() != null){
				for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
					localFilters.add(ca.getUuid());
				}
			}else{
				localFilters.add(SmartDB.getCurrentConservationArea().getUuid());
			}
		}else{
			//include only selected conservation areas
			localFilters.addAll(caFilters);
		}
		
		if (localFilters.size() == 0){
			return "false"; //$NON-NLS-1$
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append(caClassPrefix);
		sb.append(".ca_uuid IN ("); //$NON-NLS-1$
		for (int i = 0; i < localFilters.size(); i++) {
			if (i != 0){
				sb.append(","); //$NON-NLS-1$
			}
			String uuid = SmartUtils.encodeHex(localFilters.get(i));
			sb.append("x'" + uuid + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		sb.append(")"); //$NON-NLS-1$
		return sb.toString();
	}

	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#hasCategoryFilter()
	 */
	@Override
	public boolean hasCategoryFilter() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#hasAttributeFilter()
	 */
	@Override
	public boolean hasAttributeFilter() {
		return false;
	}

	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#hasEmployeeFilter()
	 */
	@Override
	public boolean hasEmployeeFilter() {
		return false;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#getAttributeFilters(java.util.HashSet)
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
	 * @see org.wcs.smart.query.parser.filter.IFilter#getChildren()
	 */
	@Override
	public List<IFilter> getChildren() {
		return null;
	}	
}
