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

import java.util.Collection;
import java.util.Locale;

import org.eclipse.core.runtime.Platform;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Language;
import org.wcs.smart.util.I18nUtil;

/**
 * System configuration.
 * This configuration tracks the conservation areas available (for
 * cross conservation area analysis), the current language, the
 * current user, and collection of users (for ccaa analysis).
 * 
 * @author Emily
 *
 */
public class ConservationAreaConfiguration {

	private ConservationArea conservationArea;
	private Collection<ConservationArea> conservationAreas;
	private Collection<Employee> employees;
	private Employee ccaaUser;
	private ConservationArea mainConservationArea;
	private Language language;
	
	/**
	 * The set of conservation areas logged into
	 * @param conservationAreas
	 */
	public ConservationAreaConfiguration(
			ConservationArea ca,
			Collection<ConservationArea> conservationAreas,
			Employee ccaaUser,
			Collection<Employee> employees,
			Session session){			
		this.conservationAreas = conservationAreas;
		this.employees = employees;
		this.ccaaUser = ccaaUser;
		this.conservationArea = ca;
		computeMainConservationArea(session);
	}
	
	/**
	 * The employee object created against the CCAA database for storing
	 * queries/reports against.
	 * @return
	 */
	public Employee getCcaaUser(){
		return this.ccaaUser;
	}
	
	/**
	 * 
	 * @return List of employee objects that represent the
	 * same logged-in user in the different conservation areas.
	 */
	public Collection<Employee> getEmployees(){
		return this.employees;
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
	public Collection<ConservationArea> getConservationAreas() {
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
	 * The language associated with the conservation area
	 * that is the best match to the locale
	 * @return
	 */
	public Language getLanguage(){
		return this.language;
	}
	
	/**
	 * determines the main conservation area
	 * based on the current locale.
	 */
	private void computeMainConservationArea(Session session){
		Locale l = Locale.getDefault();
		try{
			l = I18nUtil.stringToLocale(Platform.getNL());
		}catch (Exception ex){
			//eatme
		}
		this.language = HibernateManager.findLanguage(session, l, conservationArea);
		
//		for (ConservationArea ca : conservationAreas){
//			Language language = HibernateManager.findLanguage(session, l, ca);
//			if (language != null){
//				mainConservationArea = ca;
////				this.displayLanguage = language;
//				break;
//			}
//		}
		if (mainConservationArea == null && conservationAreas.size() > 0){
			mainConservationArea = conservationAreas.iterator().next();
//			this.displayLanguage = mainConservationArea.getDefaultLanguage();
		}
		
	}
}
