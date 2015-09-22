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
package org.wcs.smart.cybertracker.patrol.importer;

import java.util.Locale;

import org.wcs.smart.cybertracker.importer.CtStringUtil;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.patrol.model.CyberTrackerPatrol;

/**
 * Label provider for details panel fields
 * @author elitvin
 * @since 4.0.0
 */
public class PatrolCTLabelProvider {

	/**
	 * Metadata for patrols that is displayed in details window.
	 * 
	 * @author elitvin
	 * @since 4.0.0
	 */
	public enum CTPatrolUIMeta {
		START_DATE,
		END_DATE,
		TYPE,
		TRANSPORT,
		ARMED,
//		MANDATE,
		TEAM,
		STATION	,
//		OBJECTIVE,
		COMMENT,
		LEADER,
		PILOT,
		MEMBERS,
		SIGHT_COUNT;
	}

	private CTPatrolUIMeta column;

	public PatrolCTLabelProvider(CTPatrolUIMeta column) {
		this.column = column;
	}
	public String getText(Object element) {
		if (element instanceof CyberTrackerPatrol) {
			CyberTrackerPatrol ctPatrol = (CyberTrackerPatrol) element;
			switch (column) {
			case START_DATE:return CtStringUtil.dateAsString(ctPatrol.getStartDate());
			case END_DATE: 	return CtStringUtil.dateAsString(ctPatrol.getEndDate());
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
			case MEMBERS:	return CtStringUtil.listAsString(ctPatrol.getCtMembers(), ", "); //$NON-NLS-1$
			case SIGHT_COUNT:return String.valueOf(ctPatrol.getSData().size());
			}
		}
		return "unknown meta: " + column; //$NON-NLS-1$
	}

}
