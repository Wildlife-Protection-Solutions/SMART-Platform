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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Label provided for employee class.
 * <p>
 * Optional leader and pilots lists can be provided. If provided the image
 * returned identified if the employee was a leader or pilot. Otherwise the
 * image returned identifies if the employee is a smart user or not.
 * </p>
 * 
 * @author Emily
 * @since 1.0.0
 */
public class EmployeeLabelProvider extends LabelProvider {

	private static final String LEADER_LABEL = Messages.EmployeeLabelProvider_LeaderIdentifier;
	private static final String PILOT_LABEL = Messages.EmployeeLabelProvider_PilotIdentifier;
	
	private Set<Employee> leaders;
	private Set<Employee> pilots;

	public EmployeeLabelProvider() {
	}

	/**
	 * @param leaders
	 *            set of leaders
	 */
	public void setLeaders(Set<Employee> leaders) {
		this.leaders = leaders;
	}

	/**
	 * 
	 * @param pilots
	 *            set of pilots
	 */
	public void setPilots(Set<Employee> pilots) {
		this.pilots = pilots;
	}

	@Override
	/**
	 * If leader list is provided
	 * the image returned identified if the employee was
	 * a leader or pilot.  Otherwise the image returned identifies
	 * if the employee is a smart user or not.</p> 
	 */
	public Image getImage(Object element) {
		if (element instanceof Employee) {

			if (leaders == null) {
				if (((Employee) element).getSmartUserId() == null) {
					return SmartPatrolPlugIn.getDefault().getImageRegistry().get(
							SmartPlugIn.EMPLOYEE_ICON);
				} else {
					return SmartPatrolPlugIn.getDefault().getImageRegistry().get(
							SmartPlugIn.SMART_EMPLOYEE_ICON);
				}
			} else {
				if (leaders.contains(element)) {
					return SmartPatrolPlugIn.getDefault().getImageRegistry().get(
							SmartPatrolPlugIn.PATROL_LEADER_ICON);
				} else if (pilots != null && pilots.contains(element)) {
					return SmartPatrolPlugIn.getDefault().getImageRegistry().get(
							SmartPatrolPlugIn.PATROL_PILOT_ICON);
				} else {
					return SmartPatrolPlugIn.getDefault().getImageRegistry().get(
							SmartPatrolPlugIn.PATROL_MEMBER_ICON);
				}
			}
		}
		return null;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof Employee) {
			String text = ""; //$NON-NLS-1$
			if (leaders != null && leaders.contains(element)) {
				text = "[" + LEADER_LABEL + "] "; //$NON-NLS-1$ //$NON-NLS-2$
			} 
			if (pilots != null && pilots.contains(element)) {
				text += "[" + PILOT_LABEL + "] "; //$NON-NLS-1$ //$NON-NLS-2$
			}
			return text + SmartLabelProvider.getFullLabel((Employee) element);
		} else if (element instanceof PatrolLegMember) {
			String text = ""; //$NON-NLS-1$
			if (((PatrolLegMember) element).getIsLeader()) {
				text = "[" + LEADER_LABEL + "]";  //$NON-NLS-1$//$NON-NLS-2$
			} 
			if (((PatrolLegMember) element).getIsPilot()) {
				text += "[" + PILOT_LABEL + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			}
			return text + SmartLabelProvider.getFullLabel(((PatrolLegMember) element).getMember());
		}
		return super.getText(element);
	}

}
