package org.wcs.smart.ct2smart.xml.parser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CTPureJdbcCsvParserHandler extends DefaultHandler {

	private static final String E	= "E"; //$NON-NLS-1$
	private static final String A	= "A"; //$NON-NLS-1$
	private static final String S	= "S"; //$NON-NLS-1$
	private static final String T	= "T"; //$NON-NLS-1$


	private Connection connection;
	private PreparedStatement stElement;
	private PreparedStatement stAttribute;
	private PreparedStatement stTimertrack;
	private PreparedStatement stSighting;

	private int e_id;
	private int s_id;
	private int t_id;
	private int col_id;
	private boolean isS = true;

	private Map<String, Integer> i2column;
	private TagS s = new TagS();
	private TagT t = null;

	public CTPureJdbcCsvParserHandler(Connection connection) throws SQLException {
		this.connection = connection;
		i2column = new HashMap<String, Integer>();
		stElement = connection.prepareStatement("insert into ct_to_smart.element (i, n, uuid) values (?, ?, ?)"); //$NON-NLS-1$
		stAttribute = connection.prepareStatement("insert into ct_to_smart.attributes (id, n, i) values (?, ?, ?)"); //$NON-NLS-1$
		stTimertrack = connection.prepareStatement("insert into ct_to_smart.timertrack (device_id, date, time, latitude, longitude, uuid) values (?, ?, ?, ?, ?, ?)"); //$NON-NLS-1$		
		e_id = col_id = 1;
		s_id = t_id = 0;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);
		try {
			if (A.equals(qName)) {
				if (isS) {
					TagA a = new TagA();
					a.setI(attributes.getValue(TagA.I));
					a.setN(attributes.getValue(TagA.N));
					a.setV(attributes.getValue(TagA.V));
					s.add(a);
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
				s.clear();

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
		try {
			if (S.equals(qName)) {
				processS(s);
				if (s_id % 1000 == 0) {
					connection.commit();
					System.out.println("Processed <S> id: " + s_id);
				}
				
			} else if (T.equals(qName)) {
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
			}		
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	private void processS(TagS tagS) throws SQLException {
		for (TagA tagA : tagS) {
			Integer col = i2column.get(tagA.getI());
			if (col == null) {
				//we need extra column in csv table
				stAttribute.setInt(1, col_id);
				stAttribute.setString(2, tagA.getN());
				stAttribute.setString(3, tagA.getI());
				stAttribute.executeUpdate();
				connection.commit();
				
				col = col_id;
				connection.createStatement().executeUpdate("alter table ct_to_smart.csv add column a"+col+" varchar(1024)");  //$NON-NLS-1$//$NON-NLS-2$
				connection.commit();

				col_id++;
				i2column.put(tagA.getI(), col);
				stSighting = null;
			}
		}

		if (stSighting == null) {
			//need new statement as number of columns changed
			StringBuilder c = new StringBuilder();
			StringBuilder v = new StringBuilder();
			for (int col = 1; col < col_id; col++) {
				c.append("a").append(col).append(", "); //$NON-NLS-1$ //$NON-NLS-2$
				v.append("?, "); //$NON-NLS-1$
			}
			stSighting = connection.prepareStatement("insert into ct_to_smart.csv ("+c+"id) values ("+v+"?)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		
		for (int col = 1; col < col_id; col++) {
			stSighting.setString(col, null);
		}
		for (TagA tagA : tagS) {
			Integer col = i2column.get(tagA.getI());
			stSighting.setString(col, tagA.getV());
		}
		stSighting.setInt(col_id, s_id);
		stSighting.executeUpdate();
	}

}
