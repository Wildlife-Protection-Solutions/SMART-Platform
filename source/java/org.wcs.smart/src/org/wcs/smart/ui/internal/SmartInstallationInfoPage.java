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
package org.wcs.smart.ui.internal;

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.about.InstallationPage;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.Language;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SharedUtils;

/**
 * SMART installation info page.
 * 
 * @author Emily
 *
 */
public class SmartInstallationInfoPage extends InstallationPage {

	public SmartInstallationInfoPage() {
	}

	@Override
	public void createControl(Composite parent) {
		
		
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Text txt = new Text(comp, SWT.READ_ONLY | SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txt.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		StringBuilder sb = new StringBuilder();
		sb.append(Messages.SmartInstallationInfoPage_DBLocation_Label);
		
		String embeddedDb = SmartProperties.getInstance().getProperty(SmartProperties.PROP_SMART_DB);
		File db = new File(embeddedDb);
		try{
			sb.append(db.getCanonicalPath());
		}catch (Exception ex){
			sb.append(db.getAbsolutePath());
		}
		sb.append(SharedUtils.LINE_SEPARATOR);
		sb.append(SharedUtils.LINE_SEPARATOR);
		
		sb.append(Messages.SmartInstallationInfoPage_FilestoreLocation_Label);
		embeddedDb = SmartProperties.getInstance().getProperty(SmartProperties.PROP_FILESTORE);
		db = new File(embeddedDb);
		try{
			sb.append(db.getCanonicalPath());
		}catch (Exception ex){
			sb.append(db.getAbsolutePath());
		}
		sb.append(SharedUtils.LINE_SEPARATOR);
		sb.append(SharedUtils.LINE_SEPARATOR);
		
		sb.append(Messages.SmartInstallationInfoPage_GPSBabel_LocationLabel);
		embeddedDb = SmartProperties.getInstance().getProperty(SmartProperties.PROP_GPS_BABEL);
		db = new File(embeddedDb);
		try{
			sb.append(db.getCanonicalPath());
		}catch (Exception ex){
			sb.append(db.getAbsolutePath());
		}
		sb.append(SharedUtils.LINE_SEPARATOR);
		sb.append(SharedUtils.LINE_SEPARATOR);
		
		
		sb.append(Messages.SmartInstallationInfoPage_SystemLang_Label);
		sb.append((new Locale(Platform.getNL())).getDisplayName());
		sb.append(SharedUtils.LINE_SEPARATOR);
		sb.append(Messages.SmartInstallationInfoPage_DefaultLang_Label);
		Language defaultl = null;
		for (Language l : SmartDB.getCurrentConservationArea().getLanguages()){
			if (l.isDefault()){
				defaultl = l;
				break;
			}
		}
		if (defaultl == null){
			sb.append("null"); //$NON-NLS-1$
		}else{
			sb.append(defaultl.getLabel());
		}
		sb.append(SharedUtils.LINE_SEPARATOR);
		sb.append(SharedUtils.LINE_SEPARATOR);
		sb.append(Messages.SmartInstallationInfoPage_DbPluginVersions);
		sb.append(SharedUtils.LINE_SEPARATOR);
		Session s = HibernateManager.openSession();
		try{
			List<?> data = s.createSQLQuery("SELECT plugin_id, version FROM " +SmartDB.PLUGIN_VERSION_TBL).list(); //$NON-NLS-1$
			for (Object x : data){
				Object[] z = (Object[])x;
				sb.append("  " + (String)z[0] + ": " + (String)z[1]);  //$NON-NLS-1$//$NON-NLS-2$
				sb.append(SharedUtils.LINE_SEPARATOR);
			}
				
		}catch (Exception ex){
			SmartPlugIn.log(ex.getMessage(), ex);
			sb.append(ex.getLocalizedMessage());
		}finally{
			s.close();
		}

		txt.setText(sb.toString());
	}

}
