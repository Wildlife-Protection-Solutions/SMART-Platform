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

import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.cybertracker.importer.CTPatrolTableContainer.CTPatrolTableColumn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol.PatrolMeta;

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
			case TRANSPORT:	return ctPatrol.getCtTransport();
			case ARMED: 	return ctPatrol.isArmed() ? Messages.CTPatrolTableCellLabelProvider_Armed_Yes : Messages.CTPatrolTableCellLabelProvider_Armed_No;
//			case MANDATE:	return asString(ctPatrol.getMandate());
			case TEAM: 		return ctPatrol.getCtTeam();
			case STATION:	return ctPatrol.getCtStation();
//			case OBJECTIVE: return ctPatrol.getObjective();
			case COMMENT:	return ctPatrol.getComment();
			case LEADER:	return ctPatrol.getCtLeader();
			case PILOT:		return ctPatrol.getCtPilot();
			case MEMBERS:	return asString(ctPatrol.getCtMembers(), "; "); //$NON-NLS-1$
			case SIGHT_COUNT:return String.valueOf(ctPatrol.getPatrolData().size());
			}
		}
		return super.getText(element);
	}

	@Override
	public Image getImage(Object element) {
		PatrolMeta meta = toMeta(column);
		if (meta == null)
			return null;
		if (element instanceof CyberTrackerPatrol) {
			CyberTrackerPatrol ctPatrol = (CyberTrackerPatrol) element;
			if (!ctPatrol.getProblems().containsKey(meta))
				return null;
			switch (column) {
			case TRANSPORT:	return FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage();
			default: return FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_WARNING).getImage();
			}
		}
		return null;
	}

	@Override
	public String getToolTipText(Object element) {
		if (element instanceof CyberTrackerPatrol) {
			String result = ""; //$NON-NLS-1$
			CyberTrackerPatrol ctPatrol = (CyberTrackerPatrol) element;
			if (column == CTPatrolTableColumn.MEMBERS) {
				result = asString(ctPatrol.getCtMembers(), "\n"); //$NON-NLS-1$
			} else {
				result = getText(element);
			}
			PatrolMeta meta = toMeta(column);
			if (meta == null)
				return result;
			if (!ctPatrol.getProblems().containsKey(meta))
				return result;
			result += "\n"; //$NON-NLS-1$
			result += column == CTPatrolTableColumn.TRANSPORT ? Messages.CTPatrolTableCellLabelProvider_Tooltip_Error : Messages.CTPatrolTableCellLabelProvider_Tooltip_Warning;
			for (String problem : ctPatrol.getProblems().get(meta)) {
				result += "\n" + problem; //$NON-NLS-1$
			}
			return result;
		}		
		return super.getToolTipText(element);
	}

	private PatrolMeta toMeta(CTPatrolTableColumn column) {
		switch (column) {
		case TRANSPORT:	return PatrolMeta.TRANSPORT;
		case TEAM: 		return PatrolMeta.TEAM;
		case STATION:	return PatrolMeta.STATION;
		case LEADER:	return PatrolMeta.LEADER;
		case PILOT:		return PatrolMeta.PILOT;
		case MEMBERS:	return PatrolMeta.MEMBERS;
		default: break;
		}
		return null;
	}
	
	private String asString(List<String> members, String separator) {
		StringBuilder result = new StringBuilder();
		for (Iterator<String> i = members.iterator(); i.hasNext();) {
			String e = i.next();
			result.append(e);
			if (i.hasNext())
				result.append(separator);
		}
		return result.toString();
	}

	private String dateAsString(Date date) {
		if (date == null) {
			return ""; //$NON-NLS-1$
		}
		return DateFormat.getDateTimeInstance().format(date);
	}
	
}
