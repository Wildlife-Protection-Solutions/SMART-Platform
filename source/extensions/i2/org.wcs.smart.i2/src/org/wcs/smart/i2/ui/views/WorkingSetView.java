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
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.i2.model.IntelWorkingSetCategory;
import org.wcs.smart.i2.model.IntelWorkingSetEntity;
import org.wcs.smart.i2.model.IntelWorkingSetItem;
import org.wcs.smart.i2.model.IntelWorkingSetRecord;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.i2.ui.WorkingSetLabelProvider;

public class WorkingSetView {
	
	public static final String ID = "org.wcs.smart.i2.ui.view.workingset";
	
	@Inject
	private IEclipseContext context;

	private Label lblWorkingSet;
	private ToolItem deleteItem;
	private ToolItem newItem;
	private TreeViewer workingsetTree;
	
	public WorkingSetView() {
		super();
	}

	@PostConstruct
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginWidth = 0;
		((GridLayout)parent.getLayout()).marginHeight = 0;
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		Composite core = toolkit.createComposite(parent);
		core.setLayout(new GridLayout());
		((GridLayout)core.getLayout()).marginWidth = 0;
		((GridLayout)core.getLayout()).marginHeight = 0;
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite header = toolkit.createComposite(core);
		header.setLayout(new GridLayout(3, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		toolkit.createLabel(header, "Active Working Set:");
		lblWorkingSet = toolkit.createLabel(header, "");
		lblWorkingSet.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		ToolBar tools = new ToolBar(header, SWT.FLAT);
		deleteItem = new ToolItem(tools, SWT.PUSH);
		deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteItem.setToolTipText("Delete active working set");
		deleteItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteActiveWorkingSet();	
			}
		});
		newItem = new ToolItem(tools, SWT.PUSH);
		newItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		newItem.setToolTipText("Create new working set");
		newItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				newWorkingSet();	
			}
		});
		
		Hyperlink select = toolkit.createHyperlink(core, "Select Working Set...", SWT.NONE);
		select.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				selectWorkingSet();
			}
		});
	
		workingsetTree = new TreeViewer(core, SWT.FULL_SELECTION | SWT.BORDER);
		workingsetTree.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(workingsetTree.getTree());
		workingsetTree.setLabelProvider(WorkingSetLabelProvider.INSTANCE);
		workingsetTree.setContentProvider(new WorkingSetTreeContentProvider());
		
	}
	
	private void newWorkingSet(){
		IntelWorkingSet workingSet = new IntelWorkingSet();
		workingSet.setConservationArea(SmartDB.getCurrentConservationArea());
		workingSet.updateName(SmartDB.getCurrentLanguage(), "New Working Set");
		workingSet.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), "New Working Set");
		workingSet.setName("New Working Set");
		
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			s.save(workingSet);
			s.getTransaction().commit();
		}catch (Exception ex){
			//TODO:
		}finally{
			s.close();
		}
		setWorkingSet(workingSet);
	}

	private void deleteActiveWorkingSet(){
		//TODO:
	}
	
	private void selectWorkingSet(){
		//TODO:
	}
	
	private void setWorkingSet(IntelWorkingSet set){
		LoadWorkingSetJob job = new LoadWorkingSetJob();
		job.setSystem(true);
		job.setWorkingSetUuid(set.getUuid());
		job.schedule();
	}
	@Focus
	public void setFocus() {
	}

	@PreDestroy
	public void dispose() {
	}
	
	public static class WorkingSetViewWrapper extends DIViewPart<WorkingSetView>{
		public WorkingSetViewWrapper() {
			super(WorkingSetView.class);
		}
	}

	private class LoadWorkingSetJob extends Job{
		public LoadWorkingSetJob(){
			super("load working set job");
		}

		private UUID workingSetUuid;
		
		public void setWorkingSetUuid(UUID uuid){
			this.workingSetUuid = uuid;
		}
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			IntelWorkingSet ws = null;
			List<IntelWorkingSetItem> items = new ArrayList<IntelWorkingSetItem>();
			Session s = HibernateManager.openSession();
			try{
				ws = (IntelWorkingSet) s.get(IntelWorkingSet.class, workingSetUuid);
				
				ws.getName();
				for (IntelWorkingSetEntity entity : ws.getEntities()){
					IntelWorkingSetItem i = new IntelWorkingSetItem(IntelWorkingSetCategory.ENTITY, entity.getEntity().getIdAttributeAsText(), entity.getEntity().getUuid(), EntityTypeLabelProvider.INSTANCE.getImage(entity.getEntity().getEntityType()));
					items.add(i);
				}
				
				for (IntelWorkingSetRecord record : ws.getRecords()){
					IntelWorkingSetItem i = new IntelWorkingSetItem(IntelWorkingSetCategory.RECORD, record.getRecord().getTitle(), record.getRecord().getUuid(), Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RECORD));
					items.add(i);
				}
			}finally{
				s.close();
			}
			final String wsname = ws.getName();
			Display.getDefault().syncExec(()->{
				if (lblWorkingSet.isDisposed()) return;
				lblWorkingSet.setText(wsname);
				workingsetTree.setInput(items);
			});
			
			return Status.OK_STATUS;
		}
		
	}
}