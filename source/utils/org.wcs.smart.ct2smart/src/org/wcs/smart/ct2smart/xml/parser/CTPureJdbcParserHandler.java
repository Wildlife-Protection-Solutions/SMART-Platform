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
	
	private TagT t = null;

	
	public CTPureJdbcParserHandler(Connection connection) throws SQLException {
		this.connection = connection;
		stElement = connection.prepareStatement("insert into ct_to_smart.element (i, n, uuid) values (?, ?, ?)"); //$NON-NLS-1$
		stSighting = connection.prepareStatement("insert into ct_to_smart.sighting (i, n, v, s_uuid, uuid) values (?, ?, ?, ?, ?)"); //$NON-NLS-1$
//		stTimertrack = connection.prepareStatement("insert into ct_to_smart.timertrack (i, n, v, t_uuid, uuid) values (?, ?, ?, ?, ?)"); //$NON-NLS-1$
		stTimertrack = connection.prepareStatement("insert into ct_to_smart.timertrack (device_id, date, time, latitude, longitude, uuid) values (?, ?, ?, ?, ?, ?)"); //$NON-NLS-1$		
		e_id = a_id = 1;
		s_id = t_id = 0;
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);
		try {
			if (A.equals(qName)) {
				if (isS) {
					stSighting.setString(1, attributes.getValue(TagA.I));
					stSighting.setString(2, attributes.getValue(TagA.N));
					stSighting.setString(3, attributes.getValue(TagA.V));
					stSighting.setInt(4, s_id);
					stSighting.setInt(5, a_id++);
					stSighting.executeUpdate();
				} else {
					String nv = attributes.getValue(TagA.N);
					if (TagT.DEVICE_ID.equals(nv)) {
						t.setDeviceId(attributes.getValue(TagA.V));
					} else if (TagT.DATE.equals(nv)) {
						t.setDate(attributes.getValue(TagA.V));
					} else if (TagT.TIME.equals(nv)) {
						t.setTime(attributes.getValue(TagA.V));
					} else if (TagT.LATITUDE.equals(nv)) {
						t.setLatitude(attributes.getValue(TagA.V));
					} else if (TagT.LONGITUDE.equals(nv)) {
						t.setLongitude(attributes.getValue(TagA.V));
					}
				}

			} else if (S.equals(qName)) {
				s_id++;
				isS = true;
				if (s_id % 1000 == 0)
					connection.commit();

			} else if (T.equals(qName)) {
				t_id++;
				isS = false;
				t = new TagT();

			} else if (E.equals(qName)) {
				stElement.setString(1, attributes.getValue(TagE.I));
				stElement.setString(2, attributes.getValue(TagE.N));
				stElement.setInt(3, e_id++);
				stElement.executeUpdate();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(uri, localName, qName);
		if (T.equals(qName)) {
			try {
				if (t != null) {
					stTimertrack.setString(1, t.getDeviceId());
					stTimertrack.setString(2, t.getDate());
					stTimertrack.setString(3, t.getTime());
					stTimertrack.setString(4, t.getLatitude());
					stTimertrack.setString(5, t.getLongitude());
					stTimertrack.setInt(6, t_id);
					stTimertrack.executeUpdate();
				}
				if (t_id % 1000 == 0)
					connection.commit();
			} catch (SQLException e) {
				e.printStackTrace();
			}

		}		
		
	}
}
