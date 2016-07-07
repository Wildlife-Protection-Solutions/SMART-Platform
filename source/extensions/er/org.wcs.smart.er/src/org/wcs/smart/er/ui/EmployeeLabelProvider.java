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
package org.wcs.smart.er.ui;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.er.model.MissionMember;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Label provided for employees and mission members.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class EmployeeLabelProvider extends LabelProvider {

	private static volatile EmployeeLabelProvider instance = null;
	
	public static EmployeeLabelProvider getInstance(){
		synchronized (EmployeeLabelProvider.class) {
			if (instance == null){
				instance = new EmployeeLabelProvider();
			}
			return instance;	
		}
	}
	
	protected EmployeeLabelProvider() {
	}


	@Override
	public Image getImage(Object element) {
		return null;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof Employee) {
			return getLabel((Employee)element);
		} else if (element instanceof MissionMember) {
			return getLabel(((MissionMember)element).getMember());
		}
		return super.getText(element);
	}
	
	private String getLabel(Employee e){
		return SmartLabelProvider.getFullLabel(e);
	}

}
