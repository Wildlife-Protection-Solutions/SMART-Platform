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
package org.wcs.smart.patrol.xml.in;

import java.io.File;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.xml.external.IConvertedExtraData;

/**
 * XML to Patrol converter interface.  This allows up to support old patrol
 * xml formats as we new ones when importing data.
 * 
 * @author Emily
 *
 */
public interface IXmlToPatrolConverter {
	
	/**
	 * @return any warnings generated during the import process
	 */
	public List<String> getWarnings();
	
	/**
	 * @return the imported patrol
	 */
	public Patrol getImportedPatrol();
		
	/**
	 * Imports a patrol from an xml object.
	 * <p>
	 * Use getImportedPatrol() to retrieve the imported
	 * patrol object.
	 * </p>
	 * <p>User getWarings() to retrieve any warnings
	 * that occurred during the import process.
	 * </p> 
	 * @param xml
	 * @param session
	 * @param ca
	 * @param attachmentLocation
	 * @throws Exception
	 */
	public void convertFile(File xmlFile, Session session, ConservationArea ca, File attachmentLocation) throws Exception;
	
	/**
	 * Must be called after convertFile.  Converts the xml plugin additions
	 */
	public List<IConvertedExtraData> convertExtraData();
}


