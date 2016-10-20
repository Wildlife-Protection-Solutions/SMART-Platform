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

import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.EntityTypeManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.birt.IntelReportManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.ui.EntityTypeLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Entity type list view.  Used in birt editor to make it easier for
 * entity type templates to be modified.
 * 
 * @author Emily
 *
 */
public class EntityTypeListView {
	
	public static final String ID = "org.wcs.smart.i2.view.entitytypelist";
	
	@Inject
	private IEclipseContext context;
	
	private TableViewer lstTypes;
	
	@PostConstruct
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout());
		
		lstTypes = new TableViewer(parent, SWT.NONE);
		lstTypes.setContentProvider(ArrayContentProvider.getInstance());
		lstTypes.setLabelProvider(new EntityTypeLabelProvider());
		lstTypes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lstTypes.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editTemplate();
			}
		});
		
		Menu mnu = new Menu(lstTypes.getControl());
		lstTypes.getControl().setMenu(mnu);
		
		MenuItem edit = new MenuItem(mnu, SWT.PUSH);
		edit.setText("Edit");
		edit.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_EDIT));
		edit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editTemplate();
			}
		});
		
		refreshList();
	}
	
	private void editTemplate(){
		Object x = ((IStructuredSelection)lstTypes.getSelection()).getFirstElement();
		if (x instanceof IntelEntityType){
			IntelReportManager.INSTANCE.editTemplate((IntelEntityType)x);
		}
	}
	
	private void refreshList(){
		refreshJob.setSystem(true);
		refreshJob.schedule();
	}
	
	@Focus
	public void setFocus(){
		lstTypes.getControl().setFocus();
	}
	@Inject
	@Optional
	private void entityTypesModified(@UIEventTopic(IntelEvents.ENTITY_TYPE_ALL) IntelEntityType type){
		refreshList();
	}
	
	public static class EntityTypeListViewWrapper extends DIViewPart<EntityTypeListView>{
		public EntityTypeListViewWrapper() {
			super(EntityTypeListView.class);
		}
	}
	
	private Job refreshJob = new Job("refresh entity list"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Display.getDefault().syncExec(() -> lstTypes.setInput(new String[]{DialogConstants.LOADING_TEXT}));
			List<IntelEntityType> types = null;
		
			Session s = HibernateManager.openSession();
			try{
				types = EntityTypeManager.INSTANCE.getEntityTypes(s, SmartDB.getCurrentConservationArea());
			}finally{
				s.close();
			}
			
			final List<IntelEntityType> ftypes = types;
			Display.getDefault().syncExec(() -> lstTypes.setInput(ftypes));
			return Status.OK_STATUS;
		}
		
	};
}
