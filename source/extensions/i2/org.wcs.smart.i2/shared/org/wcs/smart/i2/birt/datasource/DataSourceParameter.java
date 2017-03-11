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
package org.wcs.smart.i2.birt.datasource;

import org.eclipse.datatools.connectivity.oda.IParameterMetaData;

/**
 * Shared data source parameters for entity data sets.
 * 
 * @author Emily
 *
 */
public class DataSourceParameter {

	public static DataSourceParameter RECORD_UUID = new DataSourceParameter(
			"Record UUID", IParameterMetaData.parameterModeIn, //$NON-NLS-1$
			java.sql.Types.VARCHAR);
	
	public static DataSourceParameter ENTITY_UUID = new DataSourceParameter(
			"Entity UUID", IParameterMetaData.parameterModeIn, //$NON-NLS-1$
			java.sql.Types.VARCHAR);
	
	public static DataSourceParameter START_DATE = new DataSourceParameter("Start Date", //$NON-NLS-1$
			IParameterMetaData.parameterModeIn, java.sql.Types.DATE);
	
	public static DataSourceParameter END_DATE = new DataSourceParameter("End Date", //$NON-NLS-1$
			IParameterMetaData.parameterModeIn, java.sql.Types.DATE);

	private String name;
	private int parameterMode;
	private int type;

	DataSourceParameter(String name, int parameterMode, int type) {
		this.name = name;
		this.parameterMode = parameterMode;
		this.type = type;
	}

	public String getName() {
		return this.name;
	}

	public int getParameterMode() {
		return this.parameterMode;
	}

	public int getType() {
		return type;
	}

}
