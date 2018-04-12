/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.search;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.model.IntelEntitySearch;

/**
 * Intelligence search interface 
 * 
 * @author Emily
 *
 */
public interface IIntelEntitySearch {

	
	/**
	 * Maximum number of results returned in an entity search.
	 */
	public static final int MAX_RESULT_CNT = 1_000_000;
	
	/**
	 * Perform the search
	 * @param session
	 * @param locale
	 * @param monitor
	 * @return
	 */
	public IntelSearchResult doSearch(Session session, Locale locale, IProgressMonitor monitor) throws Exception;
	
	/**
	 * Serialize the search for saving the search to the database.
	 * 
	 * @return
	 */
	public String serialize();


	/**
	 * Parses the search string into a basic or advanced entity search, searching the 
	 * given conservation area.
	 * 
	 * @param searchString
	 * @param ca
	 * @return
	 */
	public static IIntelEntitySearch parseSearchString(String searchString, Collection<ConservationArea> cas) {
		if (searchString.startsWith(IntelEntitySearch.Type.BASIC.key + IntelEntitySearch.SEPARATOR)){
			//basic search
			return BasicEntitySearch.parse(searchString, cas);
		}else if (searchString.startsWith(IntelEntitySearch.Type.ADVANCED.key + IntelEntitySearch.SEPARATOR)){
			//advanced search
			return AdvancedEntitySearch.parse(searchString, cas);
		}
		
		return null;
	}
	
	public static IIntelEntitySearch parseSearchString(String searchString, ConservationArea ca) {
		return parseSearchString(searchString, Collections.singletonList(ca));
	}
}
