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
package org.wcs.smart.hibernate;

import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.Platform;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.util.SmartUtils;

/**
 * System configuration when performing cross conservation analysis.
 * This configuration tracks the conservation areas available
 * and the "main" conservation area used for querying shared objects
 * (such as the data model).
 * 
 * @author Emily
 *
 */
public class MultipleCaAnalysisConfiguration {

	private List<ConservationArea> conservationAreas;
	private ConservationArea mainConservationArea;

	/**
	 * The set of conservation areas logged into
	 * @param conservationAreas
	 */
	public MultipleCaAnalysisConfiguration(List<ConservationArea> conservationAreas){
		this.conservationAreas = conservationAreas;
		computeMainConservationArea();
	}

	/**
	 * 
	 * @return the main conservation area to be used for
	 * shared items
	 */
	public ConservationArea getMainConservationArea(){
		return this.mainConservationArea;
	}
	
	/**
	 * 
	 * @return the list of conservation areas
	 */
	public List<ConservationArea> getConservationAreas(){
		return this.conservationAreas;
	}
	
	/**
	 * 
	 * @return the number of conservation areas logged into
	 */
	public int getCaCount(){
		return this.conservationAreas.size();
	}
	
	
	/**
	 * determines the main conservation area
	 * based on the current locale.
	 */
	private void computeMainConservationArea(){
		Locale l = Locale.getDefault();
		try{
			l = SmartUtils.stringToLocale(Platform.getNL());
		}catch (Exception ex){
			//eatme
		}
		for (ConservationArea ca : conservationAreas){
			Language language = HibernateManager.findLanguage(l, ca);
			if (language != null){
				mainConservationArea = ca;
				break;
			}
		}
		if (mainConservationArea == null){
			mainConservationArea = conservationAreas.get(0);
		}
		SmartDB.getCurrentConservationArea().setLanguages(mainConservationArea.getLanguages());
		
	}
}
