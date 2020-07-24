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
package org.wcs.smart.ui;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.osgi.framework.Bundle;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SharedUtils;

/**
 * Handler for creating a email with part of the log
 * file included in the message body
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class EmailLogHandler {

	@Execute
	public void execute(Shell activeShell) {

		String filename = Platform.getLogFileLocation().toOSString();
		Path logfile = Paths.get(filename);
		
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.EmailLogHandler_fileAttachmentWarning);
		sb.append(SharedUtils.LINE_SEPARATOR);
		sb.append(logfile.toString());
		sb.append(SharedUtils.LINE_SEPARATOR);
		sb.append(SharedUtils.LINE_SEPARATOR);
		sb.append("SMART Version: " + System.getProperty("org.wcs.smart.version.simple")); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append(SharedUtils.LINE_SEPARATOR);
		sb.append("Locale:" + Locale.getDefault().getDisplayName()); //$NON-NLS-1$
		sb.append(SharedUtils.LINE_SEPARATOR);
		sb.append("-----"); //$NON-NLS-1$
		sb.append(SharedUtils.LINE_SEPARATOR);
		sb.append("Database PlugIn Versions"); //$NON-NLS-1$
		sb.append(SharedUtils.LINE_SEPARATOR);
		try (Session s = HibernateManager.openSession()) {
			List<?> data = s.createNativeQuery("SELECT plugin_id, version FROM " + SmartDB.PLUGIN_VERSION_TBL).list(); //$NON-NLS-1$
			for (Object x : data) {
				Object[] z = (Object[]) x;
				sb.append("  " + ((String) z[0]).substring("org.wcs.".length()) + ": " + (String) z[1]); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				sb.append(SharedUtils.LINE_SEPARATOR);
			}
		} catch (Exception ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
			sb.append(ex.getLocalizedMessage());
		}
		sb.append("-----"); //$NON-NLS-1$
		sb.append(SharedUtils.LINE_SEPARATOR);
		sb.append("PlugIn Versions"); //$NON-NLS-1$
		sb.append(SharedUtils.LINE_SEPARATOR);

		for (Bundle b : SmartPlugIn.getDefault().getBundle().getBundleContext().getBundles()) {
			if (b.getSymbolicName().startsWith("org.wcs.smart")) { //$NON-NLS-1$
				sb.append("  " + b.getSymbolicName().substring("org.wcs.".length()) + ": " + b.getVersion().toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				sb.append(SharedUtils.LINE_SEPARATOR);
			}
		}
		sb.append("-----"); //$NON-NLS-1$
		sb.append(SharedUtils.LINE_SEPARATOR);
		sb.append("Log File:"); //$NON-NLS-1$
		sb.append(SharedUtils.LINE_SEPARATOR);
		
		
		//System.out.println(logfile.toString());
		long length = 0;
		
		//only include the last 300 lines
		String line = null;
		try(BufferedReader fr = Files.newBufferedReader(logfile)){
			while((line = fr.readLine()) != null) {
				length++;
			}
		}catch (Exception ex) {
			
		}
		try(BufferedReader fr = Files.newBufferedReader(logfile)){
			int cnt = 0;
			while((line = fr.readLine()) != null) {
				if (cnt > length - 300) {
					sb.append(line);
					sb.append(SharedUtils.LINE_SEPARATOR);
				}
				cnt++;
			}
		}catch (Exception ex) {
			
		}
		
		String mailto = "mailto:?subject=" + enc("SMART Error Log") + "&body=" + enc(sb.toString());	 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		try {
			//TODO: test this on linux and mac
			Desktop.getDesktop().mail(new URI(mailto));
		} catch (Exception e) {
			SmartPlugIn.log(e.getMessage(), e);
		}		
	}

	private String enc(String p) {
		if (p == null) return ""; //$NON-NLS-1$
		return URLEncoder.encode(p, StandardCharsets.UTF_8).replace("+", "%20"); //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	// E3
	public static class EmailLogHandlerWrapper extends DIHandler<EmailLogHandler> {
		public EmailLogHandlerWrapper() {
			super(EmailLogHandler.class);
		}
	}
}
