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
package org.wcs.smart.er.query.report.table;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.er.model.IErLabelProvider;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesignProperty;

/**
 * Survey design property table 
 * 
 * @author Emily
 *
 */
public class SurveyDesignPropertyTable extends SmartBirtTable {

	public static final String SD_PREFIX = "SD_PROP"; //$NON-NLS-1$
	private SurveyDesign sd;
	private IErLabelProvider lblProvider = SmartContext.INSTANCE.getClass(IErLabelProvider.class);
	
	public SurveyDesignPropertyTable(SurveyDesign sd) {
		super(SD_PREFIX + ":" + sd.getKeyId()); //$NON-NLS-1$
		this.sd = sd;
	}

	@Override
	public String[] getColumnNames(SmartConnection connection) {
		String[] names = new String[sd.getProperties().size() + 6];
		names[0] = "sd:name"; //$NON-NLS-1$
		names[1] = "sd:status"; //$NON-NLS-1$
		names[2] = "sd:startdate"; //$NON-NLS-1$
		names[3] = "sd:enddate"; //$NON-NLS-1$
		names[4] = "sd:key"; //$NON-NLS-1$
		names[5] = "sd:description"; //$NON-NLS-1$
		
		int i = 6;
		for (SurveyDesignProperty p : sd.getProperties()){
			String key = p.getName().toLowerCase().replaceAll("[^a-z0-9]", "") + "_" + (i-5);   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
			names[i++] = "sd:" + key; //$NON-NLS-1$
		}
		return names;
	}

	@Override
	public String[] getColumnLabels(SmartConnection connection) {
		String[] names = new String[sd.getProperties().size() + 6];
		
		
		names[0] = lblProvider.getLabel(IErLabelProvider.SD_NAME_COL_KEY, connection.getCurrentLocale());
		names[1] = lblProvider.getLabel(IErLabelProvider.SD_STATUS_COL_KEY, connection.getCurrentLocale());
		names[2] = lblProvider.getLabel(IErLabelProvider.SD_STARTDATE_COL_KEY, connection.getCurrentLocale());
		names[3] = lblProvider.getLabel(IErLabelProvider.SD_ENDDATE_COL_KEY, connection.getCurrentLocale());
		names[4] = lblProvider.getLabel(IErLabelProvider.SD_KEY_COL_KEY, connection.getCurrentLocale());
		names[5] = lblProvider.getLabel(IErLabelProvider.SD_DESCRIPTION_COL_KEY, connection.getCurrentLocale());
		
		int i = 6;
		for (SurveyDesignProperty p : sd.getProperties()){
			names[i++] = p.getName();
		}
		return names;
	}

	@Override
	public int[] getColumnTypes(SmartConnection connection) {
		int[] types = new int[sd.getProperties().size() + 7];
		for (int i = 0; i < types.length; i ++){
			types[i] = java.sql.Types.VARCHAR;
		}
		types[2] = java.sql.Types.DATE;
		types[3] = java.sql.Types.DATE;
		return types;
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValues(org.wcs.smart.ca.ConservationArea)
	 */
	@Override
	public List<Object> getValues (SmartConnection connection) {
		return Collections.singletonList((Object)sd);
		
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#getValue(java.lang.Object, int)
	 */
	@Override
	public Object getValue(Object object, int index, SmartConnection connection) {
		SurveyDesign e = (SurveyDesign)object;
		
		if (index == 0){
			return e.getName();
		}else if (index == 1){
			return e.getState().getGuiName(Locale.getDefault());
		}else if (index == 2){
			return e.getStartDate();
		}else if (index == 3){
			return e.getEndDate();
		}else if (index == 4){
			return e.getKeyId();
		}else if (index == 5){
			return e.getDescription();
		}else if (index >= 6){
			return sd.getProperties().get(index - 6).getValue();
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#openQuery()
	 */
	@Override
	public void openQuery() {		
	}

	/**
	 * @see org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable#closeQuery()
	 */
	@Override
	public void closeQuery() {
	}


	@Override
	public String getTableFullName(Locale l) {
		return MessageFormat.format(
				SmartContext.INSTANCE.getClass(IErLabelProvider.class).getLabel(IErLabelProvider.SD_TABLE_LONGNAME_KEY, l), 
		new Object[]{sd.getName()});
	}

	@Override
	public String getTableShortName(Locale l) {
		return sd.getName();
	}


}
