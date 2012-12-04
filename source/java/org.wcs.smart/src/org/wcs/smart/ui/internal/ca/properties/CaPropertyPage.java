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

import java.text.Collator;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Language;
import org.wcs.smart.hibernate.SmartDB;
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

	private static final String ERROR_DIALOGTITLE = Messages.CaPropertyPage_ErrorDialogTitle;

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
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		lbl.setText(Messages.CaPropertyPage_DefaultLanguageLabel);
		
		lbl = new Label(caComposite, SWT.NONE);
		lbl.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		((GridData)lbl.getLayoutData()).horizontalIndent = 8;
		lbl.setText(ca.getDefaultLanguage().getLabel());
		
		
		lbl = new Label(caComposite, SWT.NONE);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
		lbl.setText(Messages.CaPropertyPage_SupportedLanguages_Label);
		
		Composite langComp = new Composite(caComposite, SWT.NONE);
		langComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		langComp.setLayout(new GridLayout(2, false));
		
		lstLang = new ListViewer(langComp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
		lstLang.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstLang.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof Language){
					return ((Language)element).getLabel();
				}
				return ""; //$NON-NLS-1$
			}
		});
		languages = new WritableList(ca.getLanguages(), Language.class);
		languages.remove(ca.getDefaultLanguage());
		
		lstLang.setContentProvider(new ObservableListContentProvider());
		lstLang.setInput(languages);
		
		Composite btnComp = new Composite(langComp, SWT.NONE);
		btnComp.setLayout(new GridLayout(1,false));
		Button btnAdd = new Button(btnComp, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		btnAdd.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				Locale[] ls = Locale.getAvailableLocales();
				Arrays.sort(ls, new Comparator<Locale>() {
					@Override
					public int compare(Locale o1, Locale o2) {
						if (o1.getDisplayLanguage().equals(o2.getDisplayLanguage())){
							return Collator.getInstance().compare(o1.getDisplayCountry(), o2.getDisplayCountry());
						}else{
							return Collator.getInstance().compare(o1.getDisplayLanguage(), o2.getDisplayLanguage());
						}
					}
				});
				ListSelectionDialog lstSelection = new ListSelectionDialog(
					getShell(), ls, ArrayContentProvider.getInstance(), 
					new LabelProvider(){
						public String getText(Object x){
							if (x instanceof Locale){
								Locale l = (Locale)x;
								String name = l.getDisplayName();
								name += " [" + l.getLanguage() ; //$NON-NLS-1$
								if (!l.getCountry().isEmpty()){
									name += "_" + l.getCountry(); //$NON-NLS-1$
								}
								name += "]"; //$NON-NLS-1$
								return name;
							}
							return super.getText(x);
						}
					},
					Messages.CaPropertyPage_LocaleToAddLabel);
				if (lstSelection.open() == IDialogConstants.CANCEL_ID){
					return ;
				}
				Object[] rs = lstSelection.getResult();
				for (Object r : rs){
					Language l = new Language();
					l.setCa(SmartDB.getCurrentConservationArea());
					l.setDefault(false);
					l.setCode(SmartUtils.localeToString((Locale)r));
					l.setName(((Locale)r).getDisplayName());
					
					boolean exists = false;
					for (Object o : languages){
						if (l.isSame((Language)o)){
							exists = true;
							break;
						}
					}
					if (exists || ca.getDefaultLanguage().isSame(l)){
						//already added
						continue;
					}
					languages.add(l);
					setChangesMade(true);
				}
				lstLang.refresh();
			
			}
			
		});
		
		final Button btnRemove = new Button(btnComp, SWT.PUSH);
		btnRemove.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnRemove.setEnabled(false);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		btnRemove.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (lstLang.getSelection().isEmpty()) return;
				
				boolean isChanged = false;
				for (Iterator<?> iterator = ((IStructuredSelection)lstLang.getSelection()).iterator(); iterator.hasNext();) {
					Language type = (Language) iterator.next();
					if (type.isDefault()){
						MessageDialog.openError(getShell(), ERROR_DIALOGTITLE, Messages.CaPropertyPage_Error_CannotRemoveDefault);
					}else{
						
						if (MessageDialog.openQuestion(getShell(), Messages.CaPropertyPage_ConfirmDialogTitle, 
								MessageFormat.format(Messages.CaPropertyPage_ConfirmDialogMessage, new Object[]{type.getName()}) )){
							languages.remove(type);
							isChanged = true;
						}
					}	
				}
				lstLang.refresh();
				if (isChanged){
					setChangesMade(true);
				}
			}
			
		});
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
			caComposite.updateConservationArea(ca);
			Language def= ca.getDefaultLanguage();
			ca.getLanguages().clear();
			ca.getLanguages().add(def);
			ca.getLanguages().addAll(languages);
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

}
