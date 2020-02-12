/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.ui.config;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.AbstractPawsClass;
import org.wcs.smart.paws.model.PawsQueryClass;
import org.wcs.smart.paws.model.PawsSimpleClass;

/**
 * PAWS Classification wrapper
 * 
 * @author Emily
 *
 */
public class ClassificationData {
	
	private AbstractPawsClass op1;
	private String label;

	public ClassificationData(AbstractPawsClass op1, String label) {
		this.op1 = op1;
		this.label = label;
	}
	
	public AbstractPawsClass getPawsClass(){
		return this.op1;
	}
	public String getClassification() {
		if (op1 instanceof PawsSimpleClass) return op1.getClassification();
		if (op1 instanceof PawsQueryClass) return op1.getClassification();
		return ""; //$NON-NLS-1$
	}
	
	public String getDataSource() {
		if (op1 instanceof PawsSimpleClass) return Messages.ClassificationData_DataModel;
		if (op1 instanceof PawsQueryClass) return Messages.ClassificationData_Query;
		return Messages.ClassificationData_unknown;
	}
	
	public String getDetails() {
		return label;
	}

	public static String createLabel(Category c, Attribute a, DmObject listortree){
		if (listortree == null) return c.getFullCategoryName();
		
		StringBuilder sb = new StringBuilder();
		sb.append(listortree.getName());
		sb.append( " (" + a.getName() + ") "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(c.getFullCategoryName());
		return sb.toString();
	}
}