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
package org.wcs.smart.ui.internal.ca.create;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.internal.ca.CaInfoComposite;
import org.wcs.smart.util.SmartUtils;

/**
 * The first page of the create conservation 
 * area wizard which gathers general information 
 * about a given conservation area.
 * 
 * @author Emily Gouge
 *
 */
public class CaWizard_CaDef extends CaWizardPage  {
	

	/* ui fields */
	private CaInfoComposite composite = null;
	
	private ComboViewer lstViewer ;
	
	/**
	 * Create the wizard.
	 */
	public CaWizard_CaDef() {
		super(Messages.CaWizard_CaDef_CaDef_PageName);
		setImageDescriptor(SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.SMART_48_ICON));
		setTitle(Messages.CaWizard_CaDef_PageTitle);
		setDescription(Messages.CaWizard_CaDef_PageDescription);
	}

	/**
	 * Create contents of the wizard.
	 * 
	 * @param parent
	 */
	public void createControl(Composite parent) {
		composite = new CaInfoComposite(parent,  SWT.NULL, null);
		composite.addValidationListener(new CaInfoComposite.IValidationListener() {
			@Override
			public void validate() {
				CaWizard_CaDef.this.validate();
			}
		});
		
		Label lblLang = new Label(composite, SWT.NONE);
		lblLang.setText(Messages.CaWizard_CaDef_DefaultLangLable);
		
		lstViewer = new ComboViewer(composite,  SWT.DROP_DOWN | SWT.READ_ONLY);
		lstViewer.setContentProvider(ArrayContentProvider.getInstance());
		Locale[] lls = Locale.getAvailableLocales();
		Arrays.sort(lls, new Comparator<Locale>() {
			@Override
			public int compare(Locale o1, Locale o2) {
				if (o1.getCountry().isEmpty() && !o2.getCountry().isEmpty()){
					return -1;
				}else if (!o1.getCountry().isEmpty() && o2.getCountry().isEmpty()){
					return 1;
				}
				if (o1.getDisplayLanguage().equals(o2.getDisplayLanguage())){
					return Collator.getInstance().compare(o1.getDisplayCountry(), o2.getDisplayCountry());
				}else{
					return Collator.getInstance().compare(o1.getDisplayLanguage(), o2.getDisplayLanguage());
				}
			}
		});
		lstViewer.setInput(lls);
		lstViewer.setLabelProvider(new LabelProvider(){
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
					});
		Locale l = SmartUtils.stringToLocale(Locale.getDefault().getLanguage());
		if (l != null){
			lstViewer.setSelection(new StructuredSelection(l));
		}
		lstViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});
		
		GridData data = new GridData(SWT.FILL, SWT.CENTER, true,false, 1, 1);
		data.horizontalIndent = 8;
		lstViewer.getControl().setLayoutData(data);
		
		new Label(composite, SWT.NONE);
		Label lblDescription = new Label(composite, SWT.WRAP);
		lblDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		((GridData)lblDescription.getLayoutData()).widthHint = 100;
		((GridData)lblDescription.getLayoutData()).horizontalIndent = 8;
		lblDescription.setText(Messages.CaWizard_CaDef_DefaultLangInfo);
		
		setControl(composite);
		
		validate();
	}

	/**
	 * Validate the input fields
	 */
	private void validate() {
		super.setErrorMessage(null);
		
		boolean isComplete = true;
		if (!composite.isValid()){
			isComplete = false;
		}	
		if (lstViewer.getSelection().isEmpty()){
			isComplete = false;
		}
		super.setPageComplete(isComplete);
	}

	/**
	 * Updates the conservation area with the information from this wizard page.
	 * 
	 * @param ca Conservation Area object to update
	 */
	public void updateConservationArea(ConservationArea ca) {
		composite.updateConservationArea(ca);
		
		Language lang = new Language();
		lang.setCa(ca);

		Locale e = (Locale) ((IStructuredSelection)lstViewer.getSelection()).getFirstElement(); 
		lang.setCode(SmartUtils.localeToString(e));
		lang.setDefault(true);
		ca.getLanguages().add(lang);
	}
	
}
