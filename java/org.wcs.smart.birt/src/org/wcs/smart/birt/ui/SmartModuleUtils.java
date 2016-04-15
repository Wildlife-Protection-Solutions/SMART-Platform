package org.wcs.smart.birt.ui;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.birt.report.model.api.DesignFileException;
import org.eclipse.birt.report.model.api.IVersionInfo;
import org.eclipse.birt.report.model.api.util.UnicodeUtil;
import org.eclipse.birt.report.model.core.DesignSession;
import org.eclipse.birt.report.model.core.Module;
import org.eclipse.birt.report.model.elements.Library;
import org.eclipse.birt.report.model.i18n.ThreadResources;
import org.eclipse.birt.report.model.parser.DesignSchemaConstants;
import org.eclipse.birt.report.model.parser.ModuleParserErrorHandler;
import org.eclipse.birt.report.model.util.AbstractParseState;
import org.eclipse.birt.report.model.util.ModelUtil;
import org.eclipse.birt.report.model.util.VersionControlMgr;
import org.eclipse.birt.report.model.util.VersionInfo;
import org.eclipse.birt.report.model.util.XMLParserException;
import org.eclipse.birt.report.model.util.XMLParserHandler;
import org.wcs.smart.birt.BirtResourceLocator;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * The functions below are copied from 
 * Copied from BIRT - ModuleUtil.checkVersion( fileName );
 * and modified slightly to ensure the correct
 * ResourceLocator is used.
 */
public class SmartModuleUtils {
	
	private static List<IVersionInfo> checkVersion( InputStream streamData,
			String filename ) throws DesignFileException
	{
		DesignSession session = new DesignSession( ThreadResources.getLocale( ) );
		/* set the resource locator here */
		session.setResourceLocator(BirtResourceLocator.INSTANCE);
		
		byte[] buf = new byte[512];
		int len;
		boolean isSupportedUnknownVersion = false;

		ByteArrayOutputStream bySteam = new ByteArrayOutputStream();
		byte[] data = null;
		try {
			while ((len = streamData.read(buf)) > 0) {
				bySteam.write(buf, 0, len);
				bySteam.flush();
			}

			data = bySteam.toByteArray();
			bySteam.close();
		} catch (IOException e1) {
			// do nothing
		}

		try {
			InputStream inputStreamToParse = new ByteArrayInputStream(data);
			Module module = session.openModule(filename, inputStreamToParse);

			String version = module.getVersionManager().getVersion();
			if (module.getOptions() != null) {
				isSupportedUnknownVersion = module.getOptions()
						.isSupportedUnknownVersion();
			}
			List<IVersionInfo> retList = ModelUtil.checkVersion(version,
					isSupportedUnknownVersion);
			if (hasCompatibilities(module))
				retList.add(new VersionInfo(version,
						VersionInfo.EXTENSION_COMPATIBILITY));
			return retList;
		} catch (DesignFileException e) {
			if (data != null) {
				VersionParserHandler handler = new VersionParserHandler();

				InputStream inputStreamToParse = new ByteArrayInputStream(data);
				if (!inputStreamToParse.markSupported())
					inputStreamToParse = new BufferedInputStream(streamData);

				parse(handler, inputStreamToParse, filename);

				return ModelUtil.checkVersion(handler.version,
						isSupportedUnknownVersion);
			}
			return Collections.emptyList();
		}
	}

	private static boolean hasCompatibilities(Module module) {
		VersionControlMgr versionMgr = module.getVersionManager();
		if (versionMgr.hasExtensionCompatibilities())
			return true;

		// check included libraries
		List<Library> libs = module.getAllLibraries();
		if (libs != null && !libs.isEmpty()) {
			for (int i = 0; i < libs.size(); i++) {
				Library lib = libs.get(i);
				if (lib.getVersionManager().hasExtensionCompatibilities())
					return true;
			}
		}
		return false;
	}

	/**
	 * Parser handler used to parse only the version attribute of the module.
	 * The existing report and library state is reused.
	 */

	private static class VersionParserHandler extends XMLParserHandler {

		private String version = null;

		/**
		 * Default constructor.
		 */
		public VersionParserHandler() {
			super(new ModuleParserErrorHandler());
		}

		public AbstractParseState createStartState() {
			return new StartState();
		}

		/**
		 * Recognizes the top-level tags: Report or Library
		 */

		class StartState extends InnerParseState {

			public AbstractParseState startElement(String tagName) {
				if (DesignSchemaConstants.REPORT_TAG.equalsIgnoreCase(tagName)
						|| DesignSchemaConstants.LIBRARY_TAG
								.equalsIgnoreCase(tagName))
					return new VersionState();
				return super.startElement(tagName);
			}
		}

		/**
		 * Recognizes the top-level tags: Report or Library
		 */

		class VersionState extends InnerParseState {

			public void parseAttrs(Attributes attrs) throws XMLParserException {
				String version = attrs
						.getValue(DesignSchemaConstants.VERSION_ATTRIB);
				VersionParserHandler.this.version = version;
			}

			public void end() throws SAXException {
			}
		}
	}

	/**
	 * Auxiliary method to help parse the input stream.
	 * 
	 * @param handler
	 *            the parse handler
	 * @param streamData
	 *            the input stream
	 * @throws DesignFileException
	 *             any exception if error happens
	 */

	private static void parse(XMLParserHandler handler, InputStream streamData,
			String filename) throws DesignFileException {
		try {
			ModelUtil.checkUTFSignature(streamData, filename);
			SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
			SAXParser parser = saxParserFactory.newSAXParser();
			InputSource inputSource = new InputSource(streamData);
			inputSource.setEncoding(UnicodeUtil.SIGNATURE_UTF_8);
			parser.parse(inputSource, handler);
		} catch (SAXException e) {
			List<XMLParserException> errors = handler.getErrorHandler()
					.getErrors();

			// Syntax error is found

			if (e.getException() instanceof DesignFileException) {
				throw (DesignFileException) e.getException();
			}

			// Invalid xml error is found

			throw new DesignFileException(null, errors, e);
		} catch (ParserConfigurationException e) {
			throw new DesignFileException(null, handler.getErrorHandler()
					.getErrors(), e);
		} catch (IOException e) {
			throw new DesignFileException(null, handler.getErrorHandler()
					.getErrors(), e);
		}
	}

	public static List checkVersion(String fileName) {
		List rtnList = new ArrayList();
		InputStream inputStream = null;

		URL url;
		try {
			url = ModelUtil.getURLPresentation(fileName);
			inputStream = url.openStream();
		} catch (MalformedURLException e2) {
			// do nothing
		} catch (IOException e) {
			rtnList.add(new VersionInfo(null, VersionInfo.INVALID_DESIGN_FILE));
			return rtnList;
		}

		if (inputStream == null) {
			try {
				inputStream = new FileInputStream(fileName);
			} catch (FileNotFoundException e2) {
				rtnList.add(new VersionInfo(null,
						VersionInfo.INVALID_DESIGN_FILE));
				return rtnList;
			}
		}

		try {
			inputStream = new BufferedInputStream(inputStream);
			rtnList.addAll(checkVersion(inputStream, fileName));
		} catch (DesignFileException e1) {
			rtnList.add(new VersionInfo(null, VersionInfo.INVALID_DESIGN_FILE));
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
			}
		}

		return rtnList;
	}
}
