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
package org.wcs.smart.report.internal.ui.export;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.RootReportFolder;
import org.wcs.smart.report.ui.ReportContentProvider;
import org.wcs.smart.report.ui.ReportLabelProvider;
import org.wcs.smart.util.SmartUtils;

/**
 * Wizard page to select queries from a different Conservation Area
 * to import.
 * 
 * @author Emily
 *
 */
public class ImportReportCaListPage extends WizardPage {
	
	public static final String PAGENAME = "CaReports"; //$NON-NLS-1$
	
	private CheckboxTreeViewer chReports;
	private ConservationArea currentCa;
	
	public ImportReportCaListPage(){
		super(PAGENAME);
	}
	
	
	public List<Report> getReports(){
		Object[] checked = chReports.getCheckedElements();
		List<Report> reports = new ArrayList<Report>();
		for (Object selection : checked){
			if (selection instanceof Report){
				reports.add((Report)selection);
			}
		}
		return reports;
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.ImportReportCaListPage_ReportLabel);
		
		chReports = new CheckboxTreeViewer(main, SWT.MULTI | SWT.BORDER);
		chReports.setLabelProvider(new ReportLabelProvider());
		chReports.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		chReports.setContentProvider(new ReportContentProvider());
		
		chReports.setCheckStateProvider(new ICheckStateProvider() {
			public boolean isGrayed(Object element) {
				return false;
			}
			
			@Override
			public boolean isChecked(Object element) {
				Object parent = ((ReportContentProvider)chReports.getContentProvider()).getParent(element);
				if (parent == null){
					return false;
				}else{
					return chReports.getChecked(parent);
				}
			}
		});
		
		chReports.getTree().addKeyListener(new KeyAdapter() {
			
			@Override
			public void keyPressed(KeyEvent e) {
				if (chReports.getSelection().isEmpty()){
					return;
				}
				if (e.keyCode == SWT.SPACE){
					IStructuredSelection selection = ((IStructuredSelection)chReports.getSelection());
					selection.getFirstElement();
					boolean value = chReports.getChecked(   selection.getFirstElement() );
					for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
						Object tp = (Object) iterator.next();
						chReports.setChecked(tp, !value);
					}
					treeSelectionChanged();
					e.doit = false;
							
				}
				
			}
		});
		chReports.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {

				if (event.getElement() instanceof ReportFolder ||
						event.getElement() instanceof RootReportFolder){
					boolean newState = event.getChecked();
					//check or uncheck all sub folder
					List<Object> objects = new ArrayList<Object>();
					objects.add(event.getElement());
					while(objects.size() > 0){
						Object o = objects.remove(0);
						chReports.setChecked(o, newState);
						if (o instanceof ReportFolder ||
								o instanceof RootReportFolder){
							Object[] kids = ((ReportContentProvider)chReports.getContentProvider()).getChildren(o);
							for (Object kid : kids){
								objects.add(kid);
							}
						}
					}	
					chReports.setGrayed(event.getElement(), false);
				}
				//if checked then we want to check all parent elements
				if (event.getChecked()){
					
					Object parent = ((ReportContentProvider)chReports.getContentProvider()).getParent(event.getElement());
					while(parent != null){
						chReports.setGrayChecked(parent, true);
						parent = ((ReportContentProvider)chReports.getContentProvider()).getParent(parent);
					}
				}else{
					//we want de-select parent if appropriate 
					Object parent = ((ReportContentProvider)chReports.getContentProvider()).getParent(event.getElement());
					while(parent != null){
						//if any of the children are checked then
						//we need to unselect 
						boolean checked = false;
						Object[] kids = ((ReportContentProvider)chReports.getContentProvider()).getChildren(parent);
						for (Object k : kids){
							if (chReports.getChecked(k)){
								checked = true;
								break;
							}
						}
						chReports.setGrayChecked(parent, checked);
						
						parent = ((ReportContentProvider)chReports.getContentProvider()).getParent(parent);
					}
				}
				
				treeSelectionChanged();
			}
		});
		
		setControl(main);
		setTitle(Messages.ImportReportCaListPage_Title);
		setMessage(Messages.ImportReportCaListPage_Message);

	}

	
	
	private void treeSelectionChanged(){
		setPageComplete(false);
		for (Object x : chReports.getCheckedElements()){
			if (x instanceof Report){
				setPageComplete(true);
				break;
			}
		}
		getContainer().updateButtons();
	}
	
	public void initValues(){
		ConservationArea caa = currentCa;
		currentCa = ((ImportReportCaPage)getWizard().getPage(ImportReportCaPage.PAGENAME)).getConservationArea();
		if (caa == currentCa){
			return;
		}
		
		chReports.setInput(Messages.ImportReportCaListPage_LoadingLabel);
		chReports.refresh();
		chReports.expandToLevel(2);
		
		loadReports.setSystem(true);
		loadReports.schedule();
		
		setPageComplete(false);
	}
	
	@Override
	public IWizardPage getNextPage() {
		return getWizard().getPage(ImportReportFolderPage.PAGENAME);
		
	}
	
	private Job loadReports = new Job("loadreports"){ //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			ConservationArea ca = currentCa;
			
			final List<Object> allItems = new ArrayList<Object>();
			
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			try{
				//load all shared folders
				List<?> folders = s.createCriteria(ReportFolder.class)
						.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
						.add(Restrictions.isNull("employee")) //$NON-NLS-1$
						.list();
				allItems.addAll(folders);
				
				//load all shared reports
				List<?> reports = s.createCriteria(Report.class)
						.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
						.add(Restrictions.eq("shared", true)) //$NON-NLS-1$
						.list();
				allItems.addAll(reports);
				
				assignNames(allItems, s);
			}finally{
				s.getTransaction().rollback();
				s.close();
			}
			getShell().getDisplay().syncExec(new Runnable(){

				@Override
				public void run() {
					chReports.setInput(new Object[]{allItems, null});
					chReports.expandToLevel(2);
				}});
			
			return Status.OK_STATUS;
		}
		
	};
	
	public static void assignNames(List<?> items, Session s){
		Language match = null;
		
		for (Object x : items){
			ConservationArea ca = null;
			if (x instanceof ReportFolder){
				ReportFolder f = (ReportFolder)x;
				if (f.getConservationArea().equals(SmartDB.getCurrentConservationArea())){
					continue;
				}
				ca = f.getConservationArea();
			}else if (x instanceof Report){
				Report r = (Report)x;
				r.getConservationArea().getName();
				if (r.getConservationArea().equals(SmartDB.getCurrentConservationArea())){
					continue;
				}
				ca = r.getConservationArea();
			}
			
			NamedItem it = (NamedItem)x;
			
			if (match == null){
				match = SmartUtils.findLanguageMatch(ca.getLanguages());
				if (match == null){
					match = ca.getDefaultLanguage();
				}
			}
			it.setName(findLabel(match, it.getUuid(), ca, s));	
		}
	}
	private static String findLabel(Language match, byte[] item, ConservationArea currentCa, Session session){
		org.wcs.smart.ca.Label.LabelItemPK lid = new org.wcs.smart.ca.Label.LabelItemPK();
		lid.setElement(new UuidItem(item));
		lid.setLanguage(match);
		
		org.wcs.smart.ca.Label ll = (org.wcs.smart.ca.Label) session.load(org.wcs.smart.ca.Label.class, lid);
		if (ll == null){
			lid.setLanguage(currentCa.getDefaultLanguage());
			ll = (org.wcs.smart.ca.Label) session.load(org.wcs.smart.ca.Label.class, lid);
			if (ll != null){
				return ll.getValue();
			}
		}else{
			return ll.getValue();
		}
		return ""; //$NON-NLS-1$
	}


}
