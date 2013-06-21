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
package org.wcs.smart.cybertracker.importer;

import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.SimpleListItem;
import org.wcs.smart.cybertracker.importer.CTPatrolTableContainer.CTPatrolTableColumn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol;

/**
 * Table containing data for imported patrols
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CTPatrolTableCellLabelProvider extends ColumnLabelProvider {

	private CTPatrolTableColumn column;
	
	public CTPatrolTableCellLabelProvider(CTPatrolTableColumn column) {
		this.column = column;
	}
	
	@Override
	public String getText(Object element) {
		if (element instanceof CyberTrackerPatrol) {
			CyberTrackerPatrol ctPatrol = (CyberTrackerPatrol) element;
			switch (column) {
			case START_DATE:return dateAsString(ctPatrol.getStartDate());
			case END_DATE: 	return dateAsString(ctPatrol.getEndDate());
			case TYPE: 		return ctPatrol.getPatrolType() != null ? ctPatrol.getPatrolType().getGuiName() : ""; //$NON-NLS-1$
			case TRANSPORT:	return ctPatrol.getPatrolTransportType() != null ? ctPatrol.getPatrolTransportType().getName() : ""; //$NON-NLS-1$
			case ARMED: 	return ctPatrol.isArmed() ? Messages.CTPatrolTableCellLabelProvider_Armed_Yes : Messages.CTPatrolTableCellLabelProvider_Armed_No;
			case MANDATE:	return asString(ctPatrol.getMandate());
			case TEAM: 		return asString(ctPatrol.getTeam());
			case STATION:	return asString(ctPatrol.getStation());
			case OBJECTIVE: return ctPatrol.getObjective();
			case COMMENT:	return ctPatrol.getComment();
			case LEADER:	return asString(ctPatrol.getLeader());
			case PILOT:		return asString(ctPatrol.getPilot());
			case MEMBERS:	return asString(ctPatrol.getMembers());

			}
		}
		return super.getText(element);
	}

	private String asString(Employee employee) {
		return employee != null ? employee.getFullLabel() : ""; //$NON-NLS-1$
	}

	private String asString(List<Employee> employee) {
		StringBuilder result = new StringBuilder();
		for (Iterator<Employee> i = employee.iterator(); i.hasNext();) {
			Employee e = i.next();
			result.append(asString(e));
			if (i.hasNext())
				result.append("; "); //$NON-NLS-1$
		}
		return result.toString();
	}
	
	private String asString(SimpleListItem item) {
		return item != null ? item.getName() : ""; //$NON-NLS-1$
	}

	private String dateAsString(Date date) {
		if (date == null){
			return ""; //$NON-NLS-1$
		}
		return DateFormat.getDateInstance().format(date);
	}
	
}
