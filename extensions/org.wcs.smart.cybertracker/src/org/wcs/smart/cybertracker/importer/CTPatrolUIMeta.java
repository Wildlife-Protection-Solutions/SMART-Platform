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

import org.wcs.smart.cybertracker.internal.Messages;
	/**
	 * Metadata for patrols that is displayed in details window.
	 * 
	 * @author elitvin
	 * @since 4.0.0
	 */
	public enum CTPatrolUIMeta {
		IMPORT_NOTE (""), //$NON-NLS-1$
		START_DATE	(Messages.CTPatrolTableColumn_StartDate),
		END_DATE	(Messages.CTPatrolTableColumn_EndDate),
		TYPE		(Messages.CTPatrolTableColumn_Type),
		TRANSPORT	(Messages.CTPatrolTableColumn_Transport),
		ARMED		(Messages.CTPatrolTableColumn_Armed),
//		MANDATE		(Messages.CTPatrolTableColumn_Mandate),
		TEAM		(Messages.CTPatrolTableColumn_Team),
		STATION		(Messages.CTPatrolTableColumn_Station),
//		OBJECTIVE	(Messages.CTPatrolTableColumn_Objective),
		COMMENT		(Messages.CTPatrolTableColumn_Comment),
		LEADER		(Messages.CTPatrolTableColumn_Leader),
		PILOT		(Messages.CTPatrolTableColumn_Pilot),
		MEMBERS		(Messages.CTPatrolTableColumn_Members),
		SIGHT_COUNT	(Messages.CTPatrolTableColumn_SightCount);
		
		private String guiName;
		CTPatrolUIMeta(String guiName) {
			this.guiName = guiName;
		}
		public String getGuiName() {
			return this.guiName;
		}
	}
