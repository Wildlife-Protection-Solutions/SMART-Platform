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
package org.wcs.smart.reporttable;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.data.oda.smart.impl.table.ITableImageProvider;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.reporttable.ca.CaTable;
import org.wcs.smart.reporttable.ca.EmployeeTable;
import org.wcs.smart.reporttable.ca.StationTable;
import org.wcs.smart.reporttable.patrol.PatrolMandateTable;
import org.wcs.smart.reporttable.patrol.TeamTable;

/**
 * Provides images for various SMART Birt Tables.
 * 
 * @author Emily
 *
 */
public class TableImageProvider implements ITableImageProvider {

	public TableImageProvider() {
	}

	@Override
	public Image getImage(SmartBirtTable table) {
		if (table instanceof CaTable){
			return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DATA_MODEL_ICON);
		}else if (table instanceof EmployeeTable){
			return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EMPLOYEE_ICON);
		}else if (table instanceof StationTable){
			return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.STATION_ICON);
		}else if (table instanceof PatrolMandateTable){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_MANDATE_ICON);
		}else if (table instanceof TeamTable){
			return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_TEAM_ICON);
		}
		return null;
	}

}
