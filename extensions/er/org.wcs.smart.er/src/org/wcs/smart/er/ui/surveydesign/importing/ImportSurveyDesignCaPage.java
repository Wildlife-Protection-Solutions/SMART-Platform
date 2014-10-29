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
package org.wcs.smart.er.ui.surveydesign.importing;

import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.ConservationAreaLabelProvider;

/**
 * Wizard page to select a Conservation Area which is not
 * the current Conservation Area.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class ImportSurveyDesignCaPage extends WizardPage {
	
	public static final String PAGENAME = "CaPage"; //$NON-NLS-1$
	private ComboViewer cmbViewer;
	private ConservationArea currentCa;
	
	public ImportSurveyDesignCaPage(){
		super(PAGENAME);
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite top = new Composite(main, SWT.NONE);
		top.setLayout(new GridLayout(2, false));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label l = new Label(top, SWT.NONE);
		l.setText("Conservation Area:");
		
		cmbViewer = new ComboViewer(top, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
		cmbViewer.setLabelProvider(new ConservationAreaLabelProvider());
		cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Session session = HibernateManager.openSession();
		try{
			List<ConservationArea> areas = HibernateManager.getConservationAreas(session);
			areas.remove(SmartDB.getCurrentConservationArea());
			cmbViewer.setInput(areas);
		}finally{
			session.close();
		}
		cmbViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = (IStructuredSelection) event.getSelection();
				if (sel.getFirstElement() instanceof ConservationArea){
					currentCa = (ConservationArea)sel.getFirstElement();
				}
				setPageComplete(currentCa != null);
				getWizard().getContainer().updateButtons();
			}
		});
		
		
		setControl(main);
		setTitle("Import Survey Design(s)");
		setMessage("Select the Conservation Area to import survey design(s) from.");
	}
	
	public ConservationArea getConservationArea(){
		return currentCa;
	}
	
	@Override
	public IWizardPage getNextPage() {
		if (currentCa != null) {
			return getWizard().getPage(ImportSurveyDesignDesignsPage.PAGENAME);
		}
		return null;
	}

}
