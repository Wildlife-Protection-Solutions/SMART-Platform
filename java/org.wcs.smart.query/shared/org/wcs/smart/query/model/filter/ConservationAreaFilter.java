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
package org.wcs.smart.query.model.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.util.UuidUtils;

/**
 * A Conservation Area filter.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ConservationAreaFilter implements IFilter {

	public static final String CA_SPLITTER = ","; //$NON-NLS-1$
	
	/**
	 * Parses a conservation area filter from
	 * the string representation of the filter
	 * 
	 * @param caFilterAsString the string representation of the filter
	 * @param validCas a list of all possible conservation areas 
	 * @return
	 */
	public static ConservationAreaFilter parseFilter(String caFilterAsString, Collection<ConservationArea> validCas) {
		ConservationAreaFilter filter = new ConservationAreaFilter();
		if (caFilterAsString == null || caFilterAsString.trim().length() == 0){
			filter.setIncludeAll(true);
		}else{
			filter.setIncludeAll(false);		
			try {
				String[] bits = caFilterAsString.split(CA_SPLITTER);
				for (int i = 0; i < bits.length; i++) {
					try{
						filter.addConservationArea(UuidUtils.stringToUuid(bits[i].trim()));
					}catch (Exception ex){
						//eatme
					}

				}
			
			} catch (Exception ex) {
				throw new IllegalStateException("Could not parse conservation area filter.", ex); //$NON-NLS-1$
			}
		}

		ArrayList<UUID> missing = new ArrayList<UUID>();
		for (Iterator<UUID> iterator = filter.caFilters.iterator(); iterator.hasNext();) {
			UUID type = (UUID) iterator.next();
			boolean found = false;
			for (ConservationArea ca : validCas) {
				if (type.equals(ca.getUuid())) {
					found = true;
					break;
				}
			}
			if (!found) {
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
	
	private ArrayList<UUID> caFilters = new ArrayList<UUID>();
	
	/*
	 * Applicable for cross conservation area analysis when
	 * more than one CA is being analyzed.
	 * List of conservation area listed in the filter but that
	 * are not contained the the current list of conservation
	 * areas being analyzed.  This allows the conservation
	 * area filter to be retained when changing the conservation
	 * areas available in the cross ca analysis.
	 */
	private List<UUID> missingCas = null;
	
	private boolean includeAll = false;
	
	/**
	 * Creates a new empty conservation area filter
	 */
	public ConservationAreaFilter(){
		
	}
	/**
	 * Creates a new default conservation area filter.
	 * <p>By default this filter includes the logged in conservation
	 * area for single analysis, or all selected conservation areas
	 * for multiple ca analysis</p>
	 * 
	 * 
	 */
	public ConservationAreaFilter(boolean init, ConservationArea currentCa){
		this();
		if (init){
			if (!(currentCa.getUuid().equals(ConservationArea.MULTIPLE_CA))){
				addConservationArea(currentCa);
			}else{
				setIncludeAll(true);
			}
		}
	}
	
	/**
	 * Refreshed the list of conservation area  CA uuids that are in the current filter but 
	 * not int the current list of CA's being analysed.  
	 */
	public void refreshMissingList(Collection<ConservationArea> validCas){
		if (missingCas != null){
			caFilters.addAll(missingCas);
		}
		ArrayList<UUID> missing = new ArrayList<UUID>();
		for (Iterator<UUID> iterator = caFilters.iterator(); iterator.hasNext();) {
			UUID type = (UUID) iterator.next();
			boolean found = false;
			for (ConservationArea ca : validCas){
				if (type.equals(ca.getUuid())){
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
	public ArrayList<UUID> getConservationAreaFilterIds(){
		return caFilters;
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
	public void addConservationArea(UUID uuid){
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
	 * if no CA's are missing
	 */
	public List<UUID> getMissingCas(){
		return missingCas;
	}
	
	/**
	 * Sets the CAs that are in the current filter but not in the 
	 * list of CA's being analysed.
	 * @param missing
	 */
	public void setMissingConservationAreas(List<UUID> missing){
		this.missingCas = missing;
	}
	
	/**
	 * @see org.wcs.smart.query.parser.filter.IFilter#asString()
	 */
	@Override
	public String asString() {
		if (includeAll || caFilters.isEmpty()){
			//if we are include all conservation areas then
			//filter is null
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < caFilters.size(); i ++){
			if (i != 0){
				sb.append(CA_SPLITTER);
			}
			sb.append( UuidUtils.uuidToString( caFilters.get(i) ) );
		}
		return sb.toString();
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
	}

	public ConservationAreaFilter clone(){
		ConservationAreaFilter clone = new ConservationAreaFilter();
		clone.setIncludeAll(includeAll);
		clone.setMissingConservationAreas(missingCas);
		for (UUID uuid : getConservationAreaFilterIds()){
			clone.addConservationArea(uuid);
		}
		return clone;
	}
}
