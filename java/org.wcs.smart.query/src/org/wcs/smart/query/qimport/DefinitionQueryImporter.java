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
package org.wcs.smart.query.qimport;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;

import org.wcs.smart.query.model.WaypointQuery;
import org.wcs.smart.query.xml.QueryXmlManager;
import org.wcs.smart.query.xml.model.Query;
import org.wcs.smart.query.xml.model.QueryType;
import org.wcs.smart.query.xml.model.UuidItemType;

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
public class DefinitionQueryImporter {

	
	public static WaypointQuery importQuery(File file) throws Exception{
		
		InputStream fin = new BufferedInputStream(new FileInputStream(file));
		Query q = QueryXmlManager.readDataModel(fin);
		
		QueryType qt = q.getQuery();
		
		WaypointQuery wq = new WaypointQuery();
		wq.setName(qt.getName());
		
		
		
		HashMap<String, UuidItemType> uuidLookup = new HashMap<String, UuidItemType>();
		for (UuidItemType type : qt.getUuiditem()){
			uuidLookup.put(type.getUuid(), type);
		}
		
		
		fin.close();
		
		
		return null;
	}
	
	
	private Object lookupItem(){
		
	}
}
