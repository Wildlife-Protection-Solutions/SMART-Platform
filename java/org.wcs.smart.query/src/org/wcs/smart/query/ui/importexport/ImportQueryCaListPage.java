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
package org.wcs.smart.query.ui.importexport;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.internal.hibernate.CaQueryHibernateManagerImpl;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.query.ui.querylist.QueryListContentProvider;
import org.wcs.smart.query.ui.querylist.QueryListLabelProvider;
import org.wcs.smart.util.SmartUtils;

/**
 * Wizard page to select queries from a different Conservation Area
 * to import.
 * 
 * @author Emily
 *
 */
public class ImportQueryCaListPage extends WizardPage {
	
	public static final String PAGENAME = "CaQueries"; //$NON-NLS-1$
	
	private CheckboxTreeViewer chQueries;
	private ConservationArea currentCa;
	
	public ImportQueryCaListPage(){
		super(PAGENAME);
	}
	
	private Job loadQueriesJob = new Job(Messages.QueryListView_LoadQueryJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			HashMap<Integer, List<QueryEditorInput>> queries = new HashMap<Integer, List<QueryEditorInput>>();
			List<QueryFolder> folders = new ArrayList<QueryFolder>();
			
			Language match = SmartUtils.findLanguageMatch(currentCa.getLanguages());
			if (match == null){
				match = currentCa.getDefaultLanguage();
			}
			
			Session session = HibernateManager.openSession();
			try{
				QueryFolder caRootFolder = new QueryFolder();
			
				caRootFolder.setName(CaQueryHibernateManagerImpl.CONSERVATION_AREA_QUERIES_NAME);
				caRootFolder.setUuid(CaQueryHibernateManagerImpl.CA_QUERY_KEY);
				caRootFolder.setConservationArea(SmartDB.getCurrentConservationArea());
				caRootFolder.setRootFolder(true);
				folders.add(caRootFolder);
				
				
				List<QueryFolder> rootFolders = session.createCriteria(QueryFolder.class)
						.add(Restrictions.eq("conservationArea", currentCa)) //$NON-NLS-1$
						.add(Restrictions.isNull("employee")) //$NON-NLS-1$
						.add(Restrictions.isNull("parentFolder")).list(); //$NON-NLS-1$
				List<QueryFolder> toName = new ArrayList<QueryFolder>();
				toName.addAll(rootFolders);
				while(toName.size() > 0){
					QueryFolder qf = toName.remove(0);
					toName.addAll(qf.getChildren());
					qf.setName(findLabel(match, qf.getUuid(), session));
				}
				
				caRootFolder.setChildren(rootFolders);
			

				for (IQueryType type : QueryTypeManager.getInstance().getSupportedQueryTypes()){
					Query hquery = session
						.createQuery("SELECT a.uuid, a.folder.uuid, a.isShared, a.id " //$NON-NLS-1$
							+ "FROM " //$NON-NLS-1$
							+ type.getHibernateClass().getSimpleName()
							+ " a " //$NON-NLS-1$
							+ "WHERE a.conservationArea = :ca " //$NON-NLS-1$
							+ "and a.isShared ='true'"); //$NON-NLS-1$
					hquery.setParameter("ca", currentCa); //$NON-NLS-1$

					List<?> results = hquery.list();
					for (Iterator<?> iterator = results.iterator(); iterator.hasNext();) {
						Object[] object = (Object[]) iterator.next();
						byte[] uuid = (byte[]) object[0];
						byte[] folderuuid = (byte[])object[1];
						String id = (String) object[3];
						String name = findLabel(match, uuid, session);
					
						QueryEditorInput proxy = new QueryEditorInput(uuid, name, id, true, type);
						byte[] key = folderuuid;
						if (folderuuid == null) {
							key = CaQueryHibernateManagerImpl.CA_QUERY_KEY;
						}
						List<QueryEditorInput> proxies = queries.get(Arrays.hashCode(key));
						if (proxies == null) {
							proxies = new ArrayList<QueryEditorInput>();
							queries.put(Arrays.hashCode(key), proxies);
						}
						proxies.add(proxy);
					}
				}
			}finally{
				session.close();
			}
			
			final HashMap<Integer, Object> data = new HashMap<Integer, Object>();
			data.put(QueryListContentProvider.QUERY_KEY, queries);
			data.put(QueryListContentProvider.FOLDER_KEY, folders);
			
			Display.getDefault().syncExec(new Runnable(){

				@Override
				public void run() {
					if (chQueries.getControl().isDisposed()) return;
					chQueries.setAutoExpandLevel(2);
					chQueries.setInput(data);
					chQueries.refresh();
					
				}});
						
			return Status.OK_STATUS;
		}
		
		private String findLabel(Language match, byte[] item, Session session){
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
	};
	
	public List<QueryEditorInput> getQueries(){
		Object[] checked = chQueries.getCheckedElements();
		List<QueryEditorInput> queries = new ArrayList<QueryEditorInput>();
		for (Object selection : checked){
			if (selection instanceof QueryEditorInput){
				queries.add((QueryEditorInput)selection);
			}
		}
		return queries;
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.ImportQueryCaListPage_QueriesLabel);
		
		chQueries = new CheckboxTreeViewer(main, SWT.MULTI | SWT.BORDER);
		chQueries.setLabelProvider(new QueryListLabelProvider());
		chQueries.setContentProvider(new QueryListContentProvider(true));
		chQueries.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		
		chQueries.setCheckStateProvider(new ICheckStateProvider() {
			
			@Override
			public boolean isGrayed(Object element) {
				return element instanceof QueryFolder;
			}
			
			@Override
			public boolean isChecked(Object element) {
				return element instanceof QueryFolder;
			}
		});
		chQueries.getTree().addKeyListener(new KeyAdapter() {
			
			@Override
			public void keyPressed(KeyEvent e) {
				if (chQueries.getSelection().isEmpty()){
					return;
				}
				if (e.keyCode == SWT.SPACE){
					IStructuredSelection selection = ((IStructuredSelection)chQueries.getSelection());
					selection.getFirstElement();
					boolean value = chQueries.getChecked(   selection.getFirstElement() );
					for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
						Object tp = (Object) iterator.next();
						chQueries.setChecked(tp, !value);
					}
					e.doit = false;
							
				}
				
			}
		});
		chQueries.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				setPageComplete(false);
				for (Object x : chQueries.getCheckedElements()){
					if (x instanceof QueryEditorInput){
						setPageComplete(true);
					}
				}
				getContainer().updateButtons();
			}
		});
		
		setControl(main);
		setTitle(Messages.ImportQueryCaListPage_Title);
		setMessage(Messages.ImportQueryCaListPage_Message);

	}
	
	public void initValues(){
		ConservationArea caa = currentCa;
		currentCa = ((ImportQueryCaPage)getWizard().getPage(ImportQueryCaPage.PAGENAME)).getConservationArea();
		if (caa == currentCa){
			return;
		}
		chQueries.setInput(new String[]{Messages.ImportQueryCaListPage_LoadingLabel});
		chQueries.refresh();
		loadQueriesJob.schedule();
		setPageComplete(false);
	}
	
	@Override
	public IWizardPage getNextPage() {
		return getWizard().getPage(ImportQueryFolderPage.PAGENAME);
		
	}

}
