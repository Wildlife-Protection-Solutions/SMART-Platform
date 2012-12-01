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

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Language;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.internal.ca.CaInfoComposite;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;

/**
 * The conservation area property dialog for managing 
 * conservation areas properties
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CaPropertyPage extends AbstractPropertyJHeaderDialog{

	private CaInfoComposite caComposite = null;
	
	private WritableList languages = new WritableList();
	private ListViewer lstLang;
	
	/**
	 * Creates a new dialog
	 */
	public CaPropertyPage() {
		super(Display.getCurrent().getActiveShell(), Messages.CaPropertyPage_Dialog_Title);
	}


	
	@Override
	protected Composite createContent(Composite parent) {
		caComposite = new CaInfoComposite(parent,  SWT.NONE, ca);
		
		Label lbl;

		
		lbl = new Label(caComposite, SWT.NONE);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		lbl.setText("Supported Languages:");
		
		Composite langComp = new Composite(caComposite, SWT.NONE);
		langComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		langComp.setLayout(new GridLayout(2, false));
		
		lstLang = new ListViewer(langComp, SWT.BORDER);
		lstLang.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstLang.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof Language){
					return ((Language) element).getName() + " [" + ((Language)element).getCode() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				return ""; //$NON-NLS-1$
			}
		});
		languages = new WritableList(ca.getLanguages(), Language.class);
		lstLang.setContentProvider(new ObservableListContentProvider());
		lstLang.setInput(languages);
		
		Composite btnComp = new Composite(langComp, SWT.NONE);
		btnComp.setLayout(new GridLayout(1,false));
		Button btnAdd = new Button(btnComp, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		final Button btnRemove = new Button(btnComp, SWT.PUSH);
		btnRemove.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnRemove.setEnabled(false);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		lstLang.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnRemove.setEnabled(!lstLang.getSelection().isEmpty());
			}
		});
		
		
		Label lbl2 = new Label(caComposite, SWT.HORIZONTAL | SWT.SEPARATOR);
		lbl2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false,2,1));
		
		
		lbl = new Label(caComposite, SWT.NONE);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false,1,1));
		lbl.setText(Messages.CaPropertyPage_UniqueID_Label);
		
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
		
		setMessage(Messages.CaPropertyPage_DialogMessage);
		
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
			SmartPlugIn.displayLog(getShell(),Messages.CaPropertyPage_Error_SavingChanages + ex.getLocalizedMessage(), ex);
		}
		return false;
	}
	
	
	
//	@Override
//	public void performDefaults(){
//		super.performDefaults();
//		caComposite.updateValues(ca);
//	}
}
