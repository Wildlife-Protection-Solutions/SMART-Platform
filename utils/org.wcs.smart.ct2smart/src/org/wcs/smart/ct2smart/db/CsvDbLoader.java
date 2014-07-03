package org.wcs.smart.ct2smart.db;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.wcs.smart.ct2smart.xml.parser.CTPureJdbcCsvParserHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CsvDbLoader {

	private static CsvDbLoader instance;

	private CsvDbLoader() {
	}

	public static CsvDbLoader getInstance() {
		if (instance == null)
			instance = new CsvDbLoader();
		return instance;
	}

	public void load(File file, Connection c) throws SQLException {
		cleanDb(c);
		parseFile(file, c);
	}
	
	private void cleanDb(Connection c) throws SQLException {
		c.createStatement().executeUpdate("TRUNCATE TABLE CT_TO_SMART.ELEMENT"); //$NON-NLS-1$
//		c.createStatement().executeUpdate("TRUNCATE TABLE CT_TO_SMART.SIGHTING"); //$NON-NLS-1$
		c.createStatement().executeUpdate("TRUNCATE TABLE CT_TO_SMART.TIMERTRACK"); //$NON-NLS-1$

		c.createStatement().executeUpdate("TRUNCATE TABLE CT_TO_SMART.ATTRIBUTES"); //$NON-NLS-1$
		try {
			c.createStatement().executeUpdate("DROP TABLE CT_TO_SMART.CSV"); //$NON-NLS-1$
		} catch (SQLException e) {
			System.out.print("Drop CSV table is not required"); //$NON-NLS-1$
			//ignore, table doesn't exist
		}
		c.createStatement().executeUpdate("create table ct_to_smart.csv (id integer not null, primary key (id))"); //$NON-NLS-1$
	}

	private void parseFile(File file, Connection c) {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = factory.newSAXParser();
			boolean autoCommit = c.getAutoCommit();
			c.setAutoCommit(false);
			DefaultHandler ctParserHandler = new CTPureJdbcCsvParserHandler(c);
			saxParser.parse(file, ctParserHandler);
			c.commit();
			c.setAutoCommit(autoCommit);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
