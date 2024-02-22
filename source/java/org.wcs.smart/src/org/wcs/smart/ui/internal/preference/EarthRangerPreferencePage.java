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
package org.wcs.smart.ui.internal.preference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationAreaProperty;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Customized Smart preference page for modifying the gps babel install
 * location.
 * 
 * @author egouge
 * 
 */
public class EarthRangerPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	public static final String ID = "org.wcs.smart.preference.earthranger"; //$NON-NLS-1$

	private Text txtUrl;
	
	public EarthRangerPreferencePage() {
		this(null);
	}
	
	public EarthRangerPreferencePage(String title) {
		super(title);
		super.setImageDescriptor(SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.ICON_INTEGRATE));

	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public boolean performOk() {
		save(txtUrl.getText());
		return true;
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		loadDefault.schedule();	
	}

	private boolean save(String url) {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try{
				ConservationAreaProperty prop = 
			
						QueryFactory.buildQuery(session, ConservationAreaProperty.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}, //$NON-NLS-1$
						new Object[] {"key", ConservationAreaProperty.EARTH_RANGER_URL}).uniqueResult(); //$NON-NLS-1$
			
				if (prop == null) {
					prop = new ConservationAreaProperty();
					prop.setKey(ConservationAreaProperty.EARTH_RANGER_URL);
					prop.setConservationArea(SmartDB.getCurrentConservationArea());
					session.persist(prop);
				}
				
				if (url.strip().equalsIgnoreCase("https://<server>.pamdas.org/")) { //$NON-NLS-1$
					session.remove(prop);
				}else {
					prop.setValue(url.strip());
				}
				session.getTransaction().commit();
				return true;
			}catch (Exception ex) {
				if (session.getTransaction().isActive()) session.getTransaction().rollback();
				throw ex;
			}
		}catch (Exception ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
			return false;
		}
	}
	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		
		Label l = new Label(main, SWT.NONE);
		l.setText("Earth Ranger URL:");
		
		txtUrl = new Text(main, SWT.BORDER );
		txtUrl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtUrl.setText(DialogConstants.LOADING_TEXT);
		
		l = new Label(main, SWT.WRAP);
		l.setText("This will be of the form https://<server>.pamdas.org");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		((GridData)l.getLayoutData()).widthHint = 100;

		loadDefault.schedule();
		return main;
	}
	
	private Job loadDefault = new Job("load preference") {  //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			String text = "";
			try (Session session = HibernateManager.openSession()){
				
				ConservationAreaProperty prop = 
						QueryFactory.buildQuery(session, ConservationAreaProperty.class, 
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()},  //$NON-NLS-1$
						new Object[] {"key", ConservationAreaProperty.EARTH_RANGER_URL}).uniqueResult();  //$NON-NLS-1$
				if (prop != null) {
					text = prop.getValue();
				}
				final String ftext = text;
				Display.getDefault().asyncExec(()->{
					txtUrl.setText(ftext);
				});
				
				return Status.OK_STATUS;
			}
		}
	};

}
