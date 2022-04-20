/*
 * Copyright (C) 2022 Wildlife Conservation Society
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

import java.io.InputStream;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.Collection;
import java.util.Locale;

import javax.xml.bind.JAXBException;

import org.wcs.smart.ca.datamodel.SimpleDataModel;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconSet;

/**
 * Converts an xml file into a data model representation 
 * 
 * @author Emily
 *
 */
public interface IXmlToDataModelConverter {

	public static enum I18NMessages{
		ATTRIBUTE_TYPE_NOT_SUPPORTED,
		ATTRIBUTE_NOT_FOUND_ERROR
	}
	
	public SimpleDataModel convert(InputStream is, Collection<Icon> icons, Collection<IconSet> iconSets, Path otherFiles, Locale l) throws JAXBException, ParseException ;

}
