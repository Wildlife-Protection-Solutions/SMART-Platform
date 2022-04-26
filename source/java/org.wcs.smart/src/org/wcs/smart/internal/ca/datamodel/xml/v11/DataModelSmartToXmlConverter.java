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
package org.wcs.smart.internal.ca.datamodel.xml.v11;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.SimpleDataModel;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelToXmlConverter;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v11.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v11.CategoryType;

/**
 * Converts a database data-model to the xml representation 
 * of the data model.
 * 
 * @author egouge
 *
 */
public class DataModelSmartToXmlConverter extends DataModelToXmlConverter{

	private IProgressMonitor monitor;
	

	public DataModelSmartToXmlConverter(IconOption includeIconFiles, IProgressMonitor monitor) {
		super(includeIconFiles);
		this.monitor = monitor;
	}
	
	/**
	 * Converts an smart model to xml model 
	 * @param dm smart data model
	 * @param monitor progress monitor
	 * @return xml data model
	 */
	@Override
	public org.wcs.smart.internal.ca.datamodel.xml.generate.v11.DataModel convert(SimpleDataModel dm) {
		
		this.dm = dm;
		
		monitor.beginTask(Messages.DataModelSmartToXmlConverter_Progress_convertingDm, 3);
		xml = new org.wcs.smart.internal.ca.datamodel.xml.generate.v11.DataModel();
		
		monitor.subTask(Messages.DataModelSmartToXmlConverter_Progress_Languages);
		this.llookup = processLanguages();
		monitor.worked(1);
		
		monitor.subTask(Messages.DataModelSmartToXmlConverter_Progress_Attributes);
		processAttributes();
		monitor.worked(1);
		
		monitor.subTask(Messages.DataModelSmartToXmlConverter_ProgressCategories);
		processCategories();
		monitor.worked(1);
		
		return xml;
	}

	@Override
	protected void processCategory(Category child, List<CategoryType> parentList){
		monitor.subTask(MessageFormat.format(Messages.DataModelSmartToXmlConverter_ProgressCategory, new Object[]{child.getName()}));
		super.processCategory(child, parentList);
	}
	
	protected AttributeType processAttribute(Attribute att) {
		monitor.subTask(MessageFormat.format(Messages.DataModelSmartToXmlConverter_ProgressAttribute, new Object[]{att.getName()}));
		return super.processAttribute(att);
	}
	
}
