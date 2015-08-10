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

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.cybertracker.importer.CyberTrackerImportComposite.CTTableColumn;
import org.wcs.smart.cybertracker.model.ICyberTrackerData;

/**
 * Label provider dor cells from table containing data for imported records
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerImportTableCellLabelProvider extends ColumnLabelProvider {
	
	private CTTableColumn column;
	
	public CyberTrackerImportTableCellLabelProvider(CTTableColumn column) {
		this.column = column;
	}
	
	@Override
	public String getText(Object element) {
		if (element instanceof ICyberTrackerData) {
			ICyberTrackerData ctData = (ICyberTrackerData) element;
			switch (column) {
			case IMPORT_NOTE: return ""; //$NON-NLS-1$
			case START_DATE:return dateAsString(ctData.getStartDate());
			case END_DATE: 	return dateAsString(ctData.getEndDate());
			case TYPE: 		return ctData.getType() != null ? ctData.getType() : ""; //$NON-NLS-1$
			case DETAILS:	return ctData.getDetails() != null ? ctData.getDetails() : ""; //$NON-NLS-1$
			case SIGHT_COUNT:return String.valueOf(ctData.getSData().size());
			}
		}
		return super.getText(element);
	}

	private String dateAsString(Date date) {
		if (date == null) {
			return ""; //$NON-NLS-1$
		}
		return DateFormat.getDateTimeInstance().format(date);
	}
	
}
