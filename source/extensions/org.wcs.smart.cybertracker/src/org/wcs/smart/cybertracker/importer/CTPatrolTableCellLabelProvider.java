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

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.cybertracker.importer.CTPatrolTableContainer.CTPatrolTableColumn;

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
			case TYPE: 		return ctPatrol.getPatrolType().getGuiName();
			case TRANSPORT:	return ctPatrol.getPatrolTransportType().getName();
			case ARMED: 	return ctPatrol.isArmed() ? "Yes" : "No";
			case MANDATE:	return ctPatrol.getMandate().getName();
			case TEAM: 		return ctPatrol.getTeam().getName();
			case STATION:	return ctPatrol.getStation().getName();
			case OBJECTIVE: return ctPatrol.getObjective();
			case COMMENT:	return ctPatrol.getComment();
			}
			return ""; //$NON-NLS-1$
		}
		return super.getText(element);
	}
	
}
