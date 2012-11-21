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
package org.wcs.smart.ui.internal.ca.properties;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ui.internal.ca.CaInfoComposite;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.util.SmartUtils;

/**
 * The conservation area property dialog for managing 
 * conservation areas properties
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CaPropertyPage extends AbstractPropertyJHeaderDialog{

	public static final String ID = "org.wcs.smart.ca.properties";
	
	private CaInfoComposite caComposite = null;
	
	/**
	 * Creates a new dialog
	 */
	public CaPropertyPage() {
		super(Display.getCurrent().getActiveShell(), "Conservation Area Properties");
	}


	
	@Override
	protected Composite createContent(Composite parent) {
		caComposite = new CaInfoComposite(parent,  SWT.NONE, ca);
		
		Label lbl2 = new Label(caComposite, SWT.HORIZONTAL | SWT.SEPARATOR);
		lbl2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,2,1));
		
		Label lbl = new Label(caComposite, SWT.NONE);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false,1,1));
		lbl.setText("Default Language:");
		
		Text lblLang = new Text(caComposite, SWT.NONE);
		lblLang.setEditable(false);
		lblLang.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false,1,1));
		String name = ca.getDefaultLanguage().getName();
		if (name == null){
			name = ca.getDefaultLanguage().getCode();
		}else{
			name = name + " (" + ca.getDefaultLanguage().getCode() + ")";
		}
		lblLang.setText(name);
		
		
		lbl = new Label(caComposite, SWT.NONE);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false,1,1));
		lbl.setText("Unique ID:");
		
		Text txt = new Text(caComposite, SWT.NONE);
		txt.setEditable(false);
		txt.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false,1,1));
		txt.setText(SmartUtils.getDirectoryPath(ca.getUuid()));
		
		caComposite.addChangeListener(new CaInfoComposite.IChangeListener() {
			
			@Override
			public void chageMade() {
				CaPropertyPage.this.setChangesMade(true);
			}
		});
		
		setMessage("Properties related to the conservation area.");
		
		return caComposite;
	}

	/**
	 * Saves the conservation area properties
	 * to the database.
	 */
	@Override
	protected boolean performSave(){		
		
		Session session = getSession();
		Transaction tx = session.beginTransaction();
		try{
//			session.saveOrUpdate(ca);
			caComposite.updateConservationArea(ca);
			tx.commit();
			setChangesMade(false);
			return true;
		}catch (RuntimeException ex){
			tx.rollback();
			session.close();
			SmartPlugIn.displayLog(getShell(),"Error saving changes to conservation areas.  " + ex.getLocalizedMessage(), ex);
		}
		return false;
	}
	
	
	
//	@Override
//	public void performDefaults(){
//		super.performDefaults();
//		caComposite.updateValues(ca);
//	}
}
