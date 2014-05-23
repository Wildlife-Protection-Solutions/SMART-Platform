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
package org.wcs.smart.ct2smart.xml.parser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author elitvin
 * @since 3.0.0
 */
public class CTPureJdbcParserHandler extends DefaultHandler {

	private static final String E	= "E"; //$NON-NLS-1$
	private static final String A	= "A"; //$NON-NLS-1$
	private static final String S	= "S"; //$NON-NLS-1$
	private static final String T	= "T"; //$NON-NLS-1$
	
	
	private Connection connection;
	private PreparedStatement stElement;
	private PreparedStatement stSighting;
	private PreparedStatement stTimertrack;
	
	private int e_id;
	private int s_id;
	private int t_id;
	private int a_id;
	private boolean isS = true;

	
	public CTPureJdbcParserHandler(Connection connection) throws SQLException {
		this.connection = connection;
		stElement = connection.prepareStatement("insert into ct_to_smart.element (i, n, uuid) values (?, ?, ?)"); //$NON-NLS-1$
		stSighting = connection.prepareStatement("insert into ct_to_smart.sighting (i, n, v, s_uuid, uuid) values (?, ?, ?, ?, ?)"); //$NON-NLS-1$
		stTimertrack = connection.prepareStatement("insert into ct_to_smart.timertrack (i, n, v, t_uuid, uuid) values (?, ?, ?, ?, ?)"); //$NON-NLS-1$
		e_id = a_id = 1;
		s_id = t_id = 0;
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);
		try {
			if (E.equals(qName)) {
				stElement.setString(1, attributes.getValue(TagE.I));
				stElement.setString(2, attributes.getValue(TagE.N));
				stElement.setInt(3, e_id++);
				stElement.executeUpdate();

			} else if (S.equals(qName)) {
				s_id++;
				isS = true;
				if (s_id % 1000 == 0)
					connection.commit();

			} else if (T.equals(qName)) {
				t_id++;
				isS = false;
				if (t_id % 1000 == 0)
					connection.commit();

			} else if (A.equals(qName)) {
				if (isS) {
					stSighting.setString(1, attributes.getValue(TagA.I));
					stSighting.setString(2, attributes.getValue(TagA.N));
					stSighting.setString(3, attributes.getValue(TagA.V));
					stSighting.setInt(4, s_id);
					stSighting.setInt(5, a_id++);
					stSighting.executeUpdate();
				} else {
					stTimertrack.setString(1, attributes.getValue(TagA.I));
					stTimertrack.setString(2, attributes.getValue(TagA.N));
					stTimertrack.setString(3, attributes.getValue(TagA.V));
					stTimertrack.setInt(4, t_id);
					stTimertrack.setInt(5, a_id++);
					stTimertrack.executeUpdate();
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
}
