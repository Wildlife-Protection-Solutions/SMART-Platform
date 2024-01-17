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
package org.wcs.smart.birt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.elements.structures.ColumnHint;
import org.eclipse.birt.report.model.api.elements.structures.OdaResultSetColumn;


public class BirtSmartUtils {

	@SuppressWarnings("unchecked")
	public static void updateDatasetConfiguration(OdaDataSetHandle dataset) {
		try{
			HashMap<String, String> name2key = new HashMap<String, String>();
			List<OdaResultSetColumn> column = (List<OdaResultSetColumn>) dataset.getProperty("resultSet"); //$NON-NLS-1$
			for (OdaResultSetColumn c : column) {
				name2key.put(c.getColumnName(), c.getNativeName());
				c.setColumnName(c.getNativeName());
			}
		
			ArrayList<?> columns = (ArrayList<?>) dataset.getProperty("columnHints");  //$NON-NLS-1$
			for (Object col : columns){
				Object displayNameObj = ((ColumnHint)col).getProperty(dataset.getModule(), ColumnHint.DISPLAY_NAME_MEMBER);
				if (displayNameObj != null) {
					String displayName = displayNameObj.toString();
					String colName = name2key.get(displayName); 
					if (colName != null) ((ColumnHint)col).setProperty(ColumnHint.COLUMN_NAME_MEMBER, colName);
					((ColumnHint)col).setProperty(ColumnHint.ALIAS_MEMBER, displayName);
				}
			}
		}catch (Exception ex){
			Logger.getLogger(BirtSmartUtils.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
		}
	}
}
