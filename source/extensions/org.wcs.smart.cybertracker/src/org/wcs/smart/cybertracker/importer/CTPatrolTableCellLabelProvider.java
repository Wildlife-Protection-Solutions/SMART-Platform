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
import java.text.MessageFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.cybertracker.importer.CTPatrolTableContainer.CTPatrolTableColumn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol.ErrorType;
import org.wcs.smart.cybertracker.model.CyberTrackerPatrol.PatrolMeta;

/**
 * Table containing data for imported patrols
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CTPatrolTableCellLabelProvider extends ColumnLabelProvider {
	
	private static final int IMAGE_SIZE = 12;
	private static final Image ERROR_IMAGE = new Image(Display.getDefault(), Display.getDefault().getSystemImage(SWT.ICON_ERROR).getImageData().scaledTo(IMAGE_SIZE, IMAGE_SIZE));
	private static final Image WARN_IMAGE  = new Image(Display.getDefault(), Display.getDefault().getSystemImage(SWT.ICON_WARNING).getImageData().scaledTo(IMAGE_SIZE, IMAGE_SIZE));

	private CTPatrolTableColumn column;
	
	public CTPatrolTableCellLabelProvider(CTPatrolTableColumn column) {
		this.column = column;
	}
	
	@Override
	public String getText(Object element) {
		if (element instanceof CyberTrackerPatrol) {
			CyberTrackerPatrol ctPatrol = (CyberTrackerPatrol) element;
			switch (column) {
			case IMPORT_NOTE: return ""; //$NON-NLS-1$
			case START_DATE:return dateAsString(ctPatrol.getStartDate());
			case END_DATE: 	return dateAsString(ctPatrol.getEndDate());
			case TYPE: 		return ctPatrol.getPatrolType() != null ? ctPatrol.getPatrolType().getGuiName(Locale.getDefault()) : ""; //$NON-NLS-1$
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
		if (CTPatrolTableColumn.IMPORT_NOTE.equals(column)) {
			if (element instanceof CyberTrackerPatrol) {
				CyberTrackerPatrol ctPatrol = (CyberTrackerPatrol) element;
				return ctPatrol.getMissingKeys().isEmpty() ? null : WARN_IMAGE;
			}
			return null;
		}
		PatrolMeta meta = toMeta(column);
		if (meta == null)
			return null;
		if (element instanceof CyberTrackerPatrol) {
			CyberTrackerPatrol ctPatrol = (CyberTrackerPatrol) element;
			List<CyberTrackerPatrol.ImportError> in = ctPatrol.getProblems().get(meta);
			if (in == null || in.size() == 0){
				return null;
			}
			for (CyberTrackerPatrol.ImportError err : in){
				if (err.getType() == ErrorType.ERROR){
					return ERROR_IMAGE;
				}
			}
			return WARN_IMAGE;
		}
		return null;
	}

	@Override
	public String getToolTipText(Object element) {
		if (element instanceof CyberTrackerPatrol) {
			String result = ""; //$NON-NLS-1$
			CyberTrackerPatrol ctPatrol = (CyberTrackerPatrol) element;
			if (CTPatrolTableColumn.IMPORT_NOTE.equals(column)) {
				if (!ctPatrol.getMissingKeys().isEmpty()) {
					String keys = ""; //$NON-NLS-1$
					for (String key : ctPatrol.getMissingKeys()) {
						keys += "\n" + key; //$NON-NLS-1$
					}
					return MessageFormat.format(Messages.CTPatrolTableCellLabelProvider_IngoreKeysWarning, keys);
				}
				return result;
			}
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
			String prefix = Messages.CTPatrolTableCellLabelProvider_Tooltip_Warning;

			String msg = ""; //$NON-NLS-1$
			for (CyberTrackerPatrol.ImportError problem : ctPatrol.getProblems().get(meta)) {
				msg += "\n" + problem.getMessage(); //$NON-NLS-1$
				if (problem.getType() == ErrorType.ERROR){
					prefix = Messages.CTPatrolTableCellLabelProvider_Tooltip_Error;
				}
			}
			result += prefix + msg;
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
