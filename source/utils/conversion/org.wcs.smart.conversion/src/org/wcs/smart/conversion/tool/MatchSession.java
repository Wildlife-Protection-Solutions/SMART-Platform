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
package org.wcs.smart.conversion.tool;

import java.sql.Connection;

import org.wcs.smart.conversion.model.SmartMapping;
import org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class MatchSession {
	
	private SmartMapping ct2Smart;
	private Connection connection;
	private DataModel dataModel;
	
	public SmartMapping getSmartMapping() {
		return ct2Smart;
	}
	public void setSmartMapping(SmartMapping ct2Smart) {
		this.ct2Smart = ct2Smart;
	}
	public Connection getConnection() {
		return connection;
	}
	public void setConnection(Connection connection) {
		this.connection = connection;
	}
	public DataModel getDataModel() {
		return dataModel;
	}
	public void setDataModel(DataModel dataModel) {
		this.dataModel = dataModel;
	}
	
}
