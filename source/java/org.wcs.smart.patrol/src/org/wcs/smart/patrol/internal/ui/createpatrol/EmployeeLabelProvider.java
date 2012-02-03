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
package org.wcs.smart.patrol.internal.ui.createpatrol;

import java.util.Set;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.PatrolLegMember;

/**
 * TODO Purpose of
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class EmployeeLabelProvider extends LabelProvider {
	
	private Set<Employee> leaders ;
	private Set<Employee> pilots;
	
	public EmployeeLabelProvider(){
	}
	
	public void setLeaders(Set<Employee> leaders){
		this.leaders = leaders;
	}
	public void setPilots(Set<Employee> pilots){
		this.pilots = pilots;
	}
	
	@Override
	public Image getImage(Object element) {
		if (element instanceof Employee) {
			
			if (leaders == null){
				if (((Employee) element).getSmartUserId() == null) {
					return JFaceResources.getImageRegistry().get(
							SmartPlugIn.EMPLOYEE_ICON);
				} else {
					return JFaceResources.getImageRegistry().get(
							SmartPlugIn.SMART_EMPLOYEE_ICON);
				}
			}else{
				if (leaders.contains(element)){
					return JFaceResources.getImageRegistry().get(
							SmartPatrolPlugIn.PATROL_LEADER_ICON);
				}else if (pilots != null && pilots.contains(element)){
					return JFaceResources.getImageRegistry().get(
							SmartPatrolPlugIn.PATROL_PILOT_ICON);
				}else{
					return JFaceResources.getImageRegistry().get(
							SmartPatrolPlugIn.PATROL_MEMBER_ICON);
				}
			}
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof Employee) {
			Employee e = (Employee) element;
			return e.getLabel();
		}else if (element instanceof PatrolLegMember){
			return ((PatrolLegMember) element).getMember().getLabel();
		}
		return super.getText(element);
	}

}
