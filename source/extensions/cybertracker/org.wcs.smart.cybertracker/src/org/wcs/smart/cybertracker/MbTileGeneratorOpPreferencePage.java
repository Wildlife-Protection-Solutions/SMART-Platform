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
package org.wcs.smart.cybertracker;

import java.text.MessageFormat;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
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
import org.wcs.smart.ca.ConservationAreaProperty;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.user.UserLevelManager;

/**
 * Customized Smart preference page for modifying the gps babel install
 * location.
 * 
 * @author egouge
 * 
 */
public class MbTileGeneratorOpPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	public static final String ID = "org.wcs.smart.cybertracker.packages"; //$NON-NLS-1$

	
	private Text txtRenderSize;
	private Label lblRenderSize;

	public MbTileGeneratorOpPreferencePage() {
		super();
	}

	public MbTileGeneratorOpPreferencePage(String title) {
		super(title);
	}

	public MbTileGeneratorOpPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public boolean performOk() {
		if (!isEditable()) return true;
		
		Integer value = -1;
		try {
			value = validateTileSize();
		}catch (Exception ex) {
			CyberTrackerPlugIn.displayError(DialogConstants.ERROR_STRING, ex.getMessage(), ex);
			return false;
		}
		
		try(Session session = HibernateManager.openSession()){
			try {
				session.getTransaction().begin();
				
				ConservationAreaProperty prop = session.createQuery("FROM ConservationAreaProperty WHERE conservationArea = :ca and key = :key", ConservationAreaProperty.class) //$NON-NLS-1$
						.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
						.setParameter("key", CyberTrackerPlugIn.MB_TILE_SIZE_PROP) //$NON-NLS-1$
						.uniqueResult();
				
				if (prop == null) {
					prop = new ConservationAreaProperty();
					prop.setKey(CyberTrackerPlugIn.MB_TILE_SIZE_PROP);
					prop.setConservationArea(SmartDB.getCurrentConservationArea());
					session.persist(prop);
				}
				if (value == 25) {
					session.remove(prop);
				}else {
					prop.setValue(String.valueOf(value));
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				CyberTrackerPlugIn.displayError(DialogConstants.ERROR_STRING, ex.getMessage(), ex);
				return false;
			}
		}
		return true;
	}

	private int validateTileSize() throws Exception{		
		int x = Integer.parseInt(txtRenderSize.getText());
		if (x < 1 || x > 100) {
			throw new Exception(Messages.MbTileGeneratorOpPreferencePage_RenderSizeRequirements);
		}
		return x;
	}
	@Override
	protected void performDefaults() {
		super.performDefaults();
		
		txtRenderSize.setText(String.valueOf(CyberTrackerPlugIn.MB_TILE_SIZE_DEFAULT));
	}

	private boolean isEditable(){
		return UserLevelManager.INSTANCE.supportsUser(SmartDB.getCurrentEmployee(), UserLevelManager.ADMIN);
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		
		if (!isEditable()) {
			Label msg1 = new Label(main, SWT.WRAP);
			msg1.setText(Messages.MbTileGeneratorOpPreferencePage_InsufficientPermissions);
			msg1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)msg1.getLayoutData()).widthHint = 100;
			return main;
		}
		
		lblRenderSize = new Label(main, SWT.WRAP);
		lblRenderSize.setText(Messages.MbTileGeneratorOpPreferencePage_Message);
		lblRenderSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
		((GridData)lblRenderSize.getLayoutData()).widthHint = 100;
		
		txtRenderSize = new Text(main, SWT.BORDER);
		txtRenderSize.setText(DialogConstants.LOADING_TEXT);
		txtRenderSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false ));
		txtRenderSize.setEditable(false);
	
		Job load= new Job(Messages.MbTileGeneratorOpPreferencePage_LoadingPref) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				ConservationAreaProperty prop = new ConservationAreaProperty();
				prop.setValue(String.valueOf(CyberTrackerPlugIn.MB_TILE_SIZE_DEFAULT));
				
				try(Session session = HibernateManager.openSession()){
					prop = session.createQuery("FROM ConservationAreaProperty WHERE conservationArea = :ca and key = :key", ConservationAreaProperty.class) //$NON-NLS-1$
							.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
							.setParameter("key", CyberTrackerPlugIn.MB_TILE_SIZE_PROP) //$NON-NLS-1$
							.uniqueResult();					
				}

				Integer value = 25;
				try {
					value = Integer.valueOf(prop.getValue());
				}catch (Exception ex) {
					CyberTrackerPlugIn.log(ex.getMessage(), ex);
				}
				
				final int fvalue = value;
				Display.getDefault().asyncExec(()->{
					if (txtRenderSize.isDisposed()) return;
					txtRenderSize.setText(String.valueOf(fvalue));
					txtRenderSize.setEditable(true);
					txtRenderSize.addListener(SWT.Modify, e->{
						try {
							validateTileSize();					
							MbTileGeneratorOpPreferencePage.this.setErrorMessage(null);
						}catch (Exception ex) {
							MbTileGeneratorOpPreferencePage.this.setErrorMessage(MessageFormat.format(Messages.MbTileGeneratorOpPreferencePage_invalidSize,ex.getMessage()));
						}
						
					});
				});
				
				return Status.OK_STATUS;
			}};
		load.schedule();
		
		return main;
	}

	
	
}
