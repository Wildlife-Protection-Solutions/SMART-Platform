package org.wcs.smart.ui.internal;

import java.io.File;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.about.InstallationPage;

import org.wcs.smart.SmartProperties;
import org.wcs.smart.util.SmartUtils;

public class SmartInstallationInfoPage extends InstallationPage {

	public SmartInstallationInfoPage() {
		// TODO Auto-generated constructor stub
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
		sb.append("Database Location: ");
		
		String embeddedDb = SmartProperties.getInstance().getProperty(SmartProperties.SMART_DB_KEY);
		File db = new File(embeddedDb);
		try{
			sb.append(db.getCanonicalPath());
		}catch (Exception ex){
			sb.append(db.getAbsolutePath());
		}
		sb.append(SmartUtils.LINE_SEPARATOR);
		sb.append(SmartUtils.LINE_SEPARATOR);
		
		sb.append("Filestore Location:");
		embeddedDb = SmartProperties.getInstance().getProperty(SmartProperties.FILESTORE_KEY);
		db = new File(embeddedDb);
		try{
			sb.append(db.getCanonicalPath());
		}catch (Exception ex){
			sb.append(db.getAbsolutePath());
		}
		sb.append(SmartUtils.LINE_SEPARATOR);
		sb.append(SmartUtils.LINE_SEPARATOR);
		
		sb.append("GPS Babel Location:");
		embeddedDb = SmartProperties.getInstance().getProperty(SmartProperties.GPS_BABLE_KEY);
		db = new File(embeddedDb);
		try{
			sb.append(db.getCanonicalPath());
		}catch (Exception ex){
			sb.append(db.getAbsolutePath());
		}
		sb.append(SmartUtils.LINE_SEPARATOR);
		sb.append(SmartUtils.LINE_SEPARATOR);
		
		txt.setText(sb.toString());
	}

}
