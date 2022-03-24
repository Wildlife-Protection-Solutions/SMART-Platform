/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.i2.patrol.ui;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.patrol.internal.Messages;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.Resources;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Record selector dialog
 * @author Emily
 *
 */
public class RecordSelectorDialog extends SmartStyledTitleDialog {

	private DateFilterDropDownComposite dComp;
	
	private TableViewer tblRecords;
	private List<IntelRecord> selection;
	
	public RecordSelectorDialog(Shell parent) {
		super(parent);
	}

	public List<IntelRecord> getSelection(){
		return this.selection;
	}
	
	@Override
	public void okPressed() {
		selection = new ArrayList<>();
		for (Iterator<?> iterator = tblRecords.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object item = (Object) iterator.next();
			if (item instanceof IntelRecord) {
				selection.add((IntelRecord) item);
			}
			
		}
		super.okPressed();
	}
	/**
	 * Create contents of the dialog.
	 */
	@Override
	public Control createDialogArea(Composite parent) {

		Composite myparent = (Composite) super.createDialogArea(parent);
		myparent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite main = new Composite(myparent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		dComp = new DateFilterDropDownComposite(main, 
				new DateFilter[] {DateFilter.LAST_30_DAYS, 
						DateFilter.LAST_60_DAYS, 
						DateFilter.ALL, DateFilter.CUSTOM},
				DateFilter.LAST_30_DAYS);
		
		Composite tableComp = new Composite(main, SWT.NONE);
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tableComp.getLayoutData()).heightHint = 200;
		
		TableColumnLayout tlayout = new TableColumnLayout();
		tableComp.setLayout(tlayout);
		
		tblRecords = new TableViewer(tableComp, SWT.BORDER | SWT.MULTI);	
		tblRecords.setContentProvider(ArrayContentProvider.getInstance());
		tblRecords.addDoubleClickListener(e->okPressed());
		TableViewerColumn col = new TableViewerColumn(tblRecords, SWT.NONE);
		tlayout.setColumnData(col.getColumn(), new ColumnWeightData(1));
		col.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof IntelRecord) {
					return ((IntelRecord) element).getTitle();
				}
				return super.getText(element);
			}
			
			public Image getImage(Object element) {
				if (element instanceof IntelRecord) {
					return Resources.INSTANCE.getImage(((IntelRecord) element).getRecordSource());
				}
				return super.getImage(element);
			}
		});
		
		refresh();
		dComp.addChangeListener(e->refresh());
		
		super.setTitle(Messages.RecordSelectorDialog_Title);
		super.setMessage(Messages.RecordSelectorDialog_Message);
		return myparent;
	}
	
	private LocalDate startDate;
	private LocalDate endDate;
	
	private void refresh() {
		tblRecords.setInput(new String[] {DialogConstants.LOADING_TEXT});
		DateFilter dFilter = dComp.getDateFilter();
		startDate = dFilter.getStartDate();
		endDate = dFilter.getEndDate();
		if (dFilter == DateFilter.CUSTOM) {
			startDate = dComp.getCustomStartDate();
			endDate = dComp.getCustomEndDate();
		}
		
		loadRecords.schedule();
	}
	
	
	private Job loadRecords = new Job("load records") { //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<IntelRecord> records;
			
			try(Session session = HibernateManager.openSession()){
				
				List<IntelProfile> profiles = ProfilesManager.INSTANCE.getProfiles(session, true);
				List<IntelProfile> viewable = new ArrayList<>();
				for (IntelProfile p : profiles) {
					if (IntelSecurityManager.INSTANCE.canViewRecords(p)) viewable.add(p);
					
				}
				
				if (viewable.isEmpty()) {
					records = Collections.emptyList();
				}else {
					StringBuilder sb = new StringBuilder();
					sb.append(" FROM "); //$NON-NLS-1$
					sb.append(" IntelRecord "); //$NON-NLS-1$
					sb.append(" WHERE conservationArea = :ca AND "); //$NON-NLS-1$
					sb.append(" profile IN (:profiles)"); //$NON-NLS-1$
					if (startDate != null && endDate != null) {
						sb.append(" AND primaryDate >= :start and primaryDate <= :end"); //$NON-NLS-1$
					}
					
					Query<IntelRecord> query= session.createQuery(sb.toString(), IntelRecord.class)
						.setParameter("ca", SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
						.setParameterList("profiles", viewable); //$NON-NLS-1$
					if (startDate != null && endDate != null) {
						query.setParameter("start",  startDate.atStartOfDay()); //$NON-NLS-1$
						query.setParameter("end",  endDate.atTime(LocalTime.MAX)); //$NON-NLS-1$
					}
					records = query.list();
					records.forEach(r->{
						if (r.getRecordSource() != null) r.getRecordSource().getName();
						r.getTitle();
					});
				}
			}
			
			Display.getDefault().asyncExec(()->{
				tblRecords.setInput(records);
			});
			return Status.OK_STATUS;
		} 
		
	};
}
