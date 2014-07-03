package org.wcs.smart.ct2smart.xml.parser;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

@Deprecated
public class CTParser {
	
	private static CTParser instance;

	private CTParser() {
	}

	public static CTParser getInstance() {
		if (instance == null)
			instance = new CTParser();
		return instance;
	}
	
	public void parseFile(File file, Connection c) {
		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			SAXParser saxParser = factory.newSAXParser();
//			CTTestParserHandler ctTestParserHandler = new CTTestParserHandler();
			boolean autoCommit = c.getAutoCommit();
			c.setAutoCommit(false);
			DefaultHandler ctParserHandler = new CTPureJdbcParserHandler(c);
//			DefaultHandler ctParserHandler = new CTPureJdbcCsvParserHandler(c);
			saxParser.parse(file, ctParserHandler);
			c.commit();
			c.setAutoCommit(autoCommit);
//			ctTestParserHandler.printResult();
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
