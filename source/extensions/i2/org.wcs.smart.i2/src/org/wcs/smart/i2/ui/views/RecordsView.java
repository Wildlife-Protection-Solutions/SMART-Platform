/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.views;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.ui.RecordLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.ui.properties.FilterComposite;

public class RecordsView {

	public static final String ID = "org.wcs.smart.i2.ui.view.records";
	
	@Inject
	private EPartService partService;

	public RecordsView() {
		super();
	}

	private ListViewer lstInProgress;
	private ListViewer lstNewRecords;
	private ListViewer lstAllRecords;
	
	@PostConstruct
	public void createPartControl(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		parent.setLayout(new GridLayout());
		
		Section inProgress = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		inProgress.setText("In Progress");
		inProgress.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		inProgress.setLayout(new GridLayout());
		lstInProgress = new ListViewer(inProgress, SWT.BORDER);
		inProgress.setClient(lstInProgress.getControl());
		lstInProgress.setContentProvider(ArrayContentProvider.getInstance());
		lstInProgress.setLabelProvider(new RecordLabelProvider());
		lstInProgress.setInput(new String[]{DialogConstants.LOADING_TEXT});
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
//		gd.heightHint = 200;
		lstInProgress.getControl().setLayoutData(gd);
		
		
		Section newRecords = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		newRecords.setText("New Records");
		newRecords.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		newRecords.setLayout(new GridLayout());
		lstNewRecords = new ListViewer(newRecords, SWT.BORDER);
		newRecords.setClient(lstNewRecords.getControl());
		lstNewRecords.setContentProvider(ArrayContentProvider.getInstance());
		lstNewRecords.setLabelProvider(new RecordLabelProvider());
		lstNewRecords.setInput(new String[]{DialogConstants.LOADING_TEXT});
		gd = new GridData(SWT.FILL, SWT.FILL, true, true);
//		gd.heightHint = 200;
		lstNewRecords.getControl().setLayoutData(gd);
		
		
		Section allRecords = toolkit.createSection(parent, Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
		allRecords.setText("All Records");
		allRecords.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		allRecords.setLayout(new GridLayout());
		Composite allRecordsSection = toolkit.createComposite(allRecords);
		allRecords.setClient(allRecordsSection);
		allRecordsSection.setLayout(new GridLayout());
		
		FilterComposite typeFilter = new FilterComposite(allRecordsSection, SWT.NONE);
		typeFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		typeFilter.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				//TODO filter
//				filter.setFilterString(typeFilter.getPatternFilter());
			}
		});
		
		lstAllRecords = new ListViewer(allRecordsSection, SWT.BORDER);
		lstAllRecords.setContentProvider(ArrayContentProvider.getInstance());
		lstAllRecords.setLabelProvider(new RecordLabelProvider());
		lstAllRecords.setInput(new String[]{DialogConstants.LOADING_TEXT});
		lstAllRecords.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		loadRecordsJob.schedule(0);
	}

	// @Optional
	// @Inject
	// private void
	// dbModified(@EventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object
	// data){
	// }

	@Focus
	public void setFocus() {
		lstInProgress.getControl().setFocus();
	}

	@PreDestroy
	public void dispose() {
	}
	
	public static class RecordsViewWrapper extends DIViewPart<RecordsView>{
		public RecordsViewWrapper() {
			super(RecordsView.class);
		}
	}

	Job loadRecordsJob = new Job("Loading Intelligence Records"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final List<IntelRecord> inProgress = new ArrayList<IntelRecord>();
			Session s = HibernateManager.openSession();
			try{
				inProgress.addAll(s.createCriteria(IntelRecord.class)
						.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
						.add(Restrictions.eq("status", IntelRecord.Status.PROCESSING))
						.list());
			}finally{
				s.close();
			}
			
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					lstInProgress.setInput(inProgress);
				}
			});
			
			final List<IntelRecord> newRecords = new ArrayList<IntelRecord>();
			s = HibernateManager.openSession();
			try{
				inProgress.addAll(s.createCriteria(IntelRecord.class)
						.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
						.add(Restrictions.eq("status", IntelRecord.Status.NEW))
						.list());
			}finally{
				s.close();
			}
			
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					lstNewRecords.setInput(newRecords);
				}
			});
			return Status.OK_STATUS;
		}
		
	};
}