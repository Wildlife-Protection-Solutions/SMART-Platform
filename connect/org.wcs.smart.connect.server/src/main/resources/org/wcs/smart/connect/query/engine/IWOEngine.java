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
package org.wcs.smart.connect.query.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;

/**
 * Interface for query engines which query waypoint/observations
 * and return the results
 * 
 * @author Emily
 *
 * @param <T>
 */
public interface IWOEngine<T extends IResultItem> extends IQueryEngine{

	/**
	 * The temporary database table containing in the queyr results
	 * @return
	 */
	public String getQueryDataTable();
	
	public ConservationAreaFilter getCaFilter();
	
	/**
	 * Generates the name for a temporary table
	 * @return
	 */
	public String createTempTableName();
	
	public int getCategoryCnt();

	/**
	 * Drops all temporary created database tables
	 * @param c
	 * @throws SQLException
	 */
	public void cleanUp(Session session) throws SQLException;
	
	/**
	 * Drops an individual database table
	 * @param c
	 * @param table
	 * @throws SQLException
	 */
	public void dropTable(Session session, String table) throws SQLException;
	
	
	/**
	 * 
	 * @return the table which contains labels for data model and other 
	 * elements that are internationalized 
	 */
	public String getObservationLabelTable();

	
	
}
