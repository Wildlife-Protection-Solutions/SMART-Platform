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
package org.wcs.smart.er.query.report;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.data.oda.smart.impl.table.ITableImageProvider;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.query.report.table.SurveyDesignPropertyTable;
import org.wcs.smart.er.query.report.table.SurveySamplingUnitTable;
import org.wcs.smart.er.ui.ErLabelProvider;

/**
 * Image provider for Survey BIRT Tables
 * @author Emily
 *
 */
public class TableImageProvider implements ITableImageProvider {

	public TableImageProvider() {
	}

	@Override
	public Image getImage(SmartBirtTable table) {
		if (table instanceof SurveyDesignPropertyTable){
			return EcologicalRecordsPlugIn.getDefault().getImageRegistry().get(EcologicalRecordsPlugIn.SURVEY_DESIGN_ICON);
		}else if (table instanceof SurveySamplingUnitTable){
			return ErLabelProvider.getImage(((SurveySamplingUnitTable)table).getType());
		}
		return null;
	}

}
