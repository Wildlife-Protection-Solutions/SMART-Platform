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
package org.wcs.smart.query;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.internal.CaDataModelManagerImpl;
import org.wcs.smart.query.internal.MultiCaDataModelManagerImpl;


/**
 * Class responsible for loading the SMART data model
 * to support querying.
 * 
 * @author Emily
 *
 */
public class QueryDataModelManager {

	private static IDataModelManager instance = null;
	
	/**
	 * 
	 * @return the active data model manager for the
	 * currnet conservation area
	 */
	public static synchronized IDataModelManager getInstance(){
		if (instance == null){
			if (SmartDB.isMultipleAnalysis()){
				instance = new MultiCaDataModelManagerImpl();
			}else{
				instance = new CaDataModelManagerImpl();
			}
		}
		return instance;
	}
	
	/**
	 * If the conservation area provided is the same as the current 
	 * conservation area this will return a call to getInstance(); otherwise
	 * it will made a new manager for the given conservation area.
	 * 
	 * @param ca the conservation area to get the data model manager for
	 * 
	 * @return
	 */
	public static IDataModelManager getManager(ConservationArea ca){
		if (ca.equals(SmartDB.getCurrentConservationArea())){
			return getInstance();
		}
		if (ca.getIsCcaa()){
			return new MultiCaDataModelManagerImpl();
		}else{
			return new CaDataModelManagerImpl(ca);
		}
	}
}
