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

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.ui.SurveyDesignLabelProvider;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Page to select designs for given Conservation Area.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class ImportSurveyDesignDesignsPage extends WizardPage{

	public static final String PAGENAME = "CaDesignsPage"; //$NON-NLS-1$
	private CheckboxTableViewer tblEntities;
	
	public ImportSurveyDesignDesignsPage(){
		super(PAGENAME);
	}

	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.SurveyDesignExportHandler_SurveyDesignsLabels);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		tblEntities = CheckboxTableViewer.newCheckList(main, SWT.BORDER | SWT.MULTI);
		tblEntities.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tblEntities.getTable().getLayoutData()).heightHint = 300;
		tblEntities.setContentProvider(ArrayContentProvider.getInstance());
		tblEntities.setLabelProvider(new SurveyDesignLabelProvider());
		tblEntities.setInput(new Object[]{Messages.SurveyDesignExportHandler_LoadingLabel});
		tblEntities.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				setPageComplete(tblEntities.getCheckedElements().length > 0);
			}
		});
		tblEntities.getTable().addKeyListener(new KeyAdapter() {
			
			@Override
			public void keyPressed(KeyEvent e) {
				if (tblEntities.getSelection().isEmpty()){
					return;
				}
				if (e.keyCode == SWT.SPACE){
					IStructuredSelection selection = ((IStructuredSelection)tblEntities.getSelection());
					selection.getFirstElement();
					boolean value = tblEntities.getChecked(   selection.getFirstElement() );
					for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
						Object tp = (Object) iterator.next();
						tblEntities.setChecked(tp, !value);
					}
					e.doit = false;
					setPageComplete(tblEntities.getCheckedElements().length > 0);
				}
				
			}
		});
					
		setControl(main);
		setTitle(Messages.ImportSurveyDesignWizard_Title);
		setMessage(Messages.ImportSurveyDesignDesignsPage_Message);
		
	}

	public void setInput(final ConservationArea ca) {
		if (ca == null)
			return;
		
		Job j = new Job("loadsurveydesigns"){ //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				final List<SurveyDesign> items = new ArrayList<SurveyDesign>();
				Session s = HibernateManager.openSession();
				s.beginTransaction();
				try{
					@SuppressWarnings("unchecked")
					List<SurveyDesign> ds = s.createCriteria(SurveyDesign.class)
							.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
							.list(); 

					items.addAll(ds); 
					Collections.sort(items, new Comparator<SurveyDesign>(){
						@Override
						public int compare(SurveyDesign sd1, SurveyDesign sd2) {
							return Collator.getInstance().compare(sd1.getName(), sd2.getName());
						}});
				}finally{
					s.getTransaction().rollback();
					s.close();
				}
				Display.getDefault().asyncExec(new Runnable(){
					@Override
					public void run() {
						if ( tblEntities.getTable().isDisposed() ) return;
						tblEntities.setInput(items);
					}});
				
				return Status.OK_STATUS;
			}};
		j.setSystem(true);
		j.schedule();
		setPageComplete(false);
	}

	public List<SurveyDesign> getDesigns() {
		Object[] checkedElements = tblEntities.getCheckedElements();
		List<SurveyDesign> designs = new ArrayList<SurveyDesign>(checkedElements.length);
		for (Object obj : checkedElements) {
			if (obj instanceof SurveyDesign) {
				designs.add((SurveyDesign)obj);
			}
		}
		return designs;
	}
	
}
