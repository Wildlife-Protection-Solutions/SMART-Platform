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
package org.wcs.smart.internal.ca.datamodel.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Locale;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel;

/**
 * Class for reading and writing data model
 * xml files.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class XmlSmartDataModelManager {

	private static final String METADATA_CLASSES_PACKAGE = "org.wcs.smart.internal.ca.datamodel.xml.generate"; //$NON-NLS-1$
	
	/**
	 * Reads a data model xml file and performs validation.
	 * <p>
	 * User is required to close input sream.
	 * </p>
	 * 
	 * @param file data model xml file
	 * @return
	 * @throws JAXBException
	 */
	public static DataModel readDataModel(InputStream file, Locale locale) throws JAXBException, ParseException{
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Unmarshaller un = context.createUnmarshaller();	
		Object o = un.unmarshal(file);
		DataModel x = (DataModel) o;
		
		//validate data model
		XmlDataModelValidator validation = new XmlDataModelValidator(x, locale);
		validation.validate();
		
		return x;
	}
	
	/**
	 * Writes a data model to an xml file.
	 * <p>
	 * User is required to close output stream.
	 * </p>
	 * @param model
	 * @param file
	 * @throws JAXBException
	 * @throws IOException
	 */
	public static void writeDataModel(DataModel model, OutputStream file) throws JAXBException, IOException{
		JAXBContext context = JAXBContext.newInstance(METADATA_CLASSES_PACKAGE);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, new Boolean(true));
		marshaller.marshal(model, file);
	}
	
	
	
	
	
}

