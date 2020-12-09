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
package org.wcs.smart.query.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.wcs.smart.query.xml.model.ObjectFactory;
import org.wcs.smart.query.xml.model.Query;


/**
 * Manager for reading and writing patrol xml files.
 * @author Emily
 * @since 1.0.0
 */
public class QueryXmlManager {
	private static final String METADATA_CLASSES_PACKAGE = "org.wcs.smart.query.xml.model"; //$NON-NLS-1$
		
	/**
	 * Reads query data from an xml file.
	 * <p>
	 * User is required to close input stream.
	 * </p>
	 * 
	 * @param file input stream to read patrol data from
	 * @return
	 * @throws JAXBException
	 */
	@SuppressWarnings("unchecked")
	public static Query readQueryFile(InputStream file) throws JAXBException{
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Unmarshaller un = context.createUnmarshaller();	
		JAXBElement<Query> o = (JAXBElement<Query>) un.unmarshal(file);
		Query x = o.getValue();
		return x;
	}
	
	/**
	 * Writes a query object to an xml file.
	 * <p>
	 * User is required to close output stream.
	 * </p>
	 * @param query xml patrol to write
	 * @param file output stream 
	 * @throws JAXBException
	 * @throws IOException
	 */
	public static void writeQuery(Query query, OutputStream file) throws JAXBException, IOException{
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		
		ObjectFactory objFactor = new ObjectFactory();
		
		JAXBElement<Query> element = objFactor.createSmartQuery(query);
		marshaller.marshal(element, file);
	}
}
