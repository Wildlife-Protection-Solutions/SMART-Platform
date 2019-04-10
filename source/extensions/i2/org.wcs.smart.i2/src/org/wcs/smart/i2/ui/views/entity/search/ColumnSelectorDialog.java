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
package org.wcs.smart.i2.ui.views.entity.search;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.ui.AttributeLabelProvider;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for selecting columns to display in all entity table.
 *  
 * @author Emily
 *
 */
public class ColumnSelectorDialog extends SmartStyledTitleDialog{

	private CheckboxTableViewer tblAttributes;
	
	private Set<String> visibleColumns;
	
	public ColumnSelectorDialog(Shell parentShell, Set<String> visibleColumns) {
		super(parentShell);
		this.visibleColumns = visibleColumns;
	}
	
	public void okPressed() {
		visibleColumns = new HashSet<>();
		for (Object x : tblAttributes.getCheckedElements()) {
			if (x instanceof IntelAttribute) {
				visibleColumns.add(((IntelAttribute) x).getKeyId());
			}
		}
		super.okPressed();
	}
	
	public Set<String> getVisibleColumns(){
		return this.visibleColumns;
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		parent = (Composite)super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.ColumnSelectorDialog_TableColumns);
		
		tblAttributes = CheckboxTableViewer.newCheckList(main, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		tblAttributes.setContentProvider(ArrayContentProvider.getInstance());
		tblAttributes.setLabelProvider(new AttributeLabelProvider());
		
		tblAttributes.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tblAttributes.getControl().getLayoutData()).heightHint = 400;
		tblAttributes.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		tblAttributes.getTable().addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == SWT.SPACE){
					Object selection = ((IStructuredSelection)tblAttributes.getSelection()).getFirstElement();
					boolean newValue = !tblAttributes.getChecked(selection);
					
					for (Iterator<?>iterator = ((IStructuredSelection)tblAttributes.getSelection()).iterator(); iterator.hasNext();) {
						Object type = iterator.next();
						tblAttributes.setChecked(type, newValue);
					}
					tblAttributes.refresh();
					e.doit = false;
				}
			}
		});
		
		Composite bottomPanel = new Composite(main, SWT.NONE);
		bottomPanel.setLayout(new GridLayout(3, false));
		bottomPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)bottomPanel.getLayout()).marginWidth = 0;
		((GridLayout)bottomPanel.getLayout()).marginHeight = 0;
		
		Link hlink = new Link(bottomPanel, SWT.NONE);
		hlink.setText("<a>" + Messages.ColumnSelectorDialog_SelectAllLabel + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		hlink.addListener(SWT.Selection, e->tblAttributes.setAllChecked(true));
		
		l = new Label(bottomPanel, SWT.SEPARATOR | SWT.VERTICAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)l.getLayoutData()).heightHint = 10;
		
		hlink = new Link(bottomPanel, SWT.NONE);
		hlink.setText("<a>" + Messages.ColumnSelectorDialog_DeSelectAllLabel + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		hlink.addListener(SWT.Selection, e->tblAttributes.setAllChecked(false));
		
		loadAttributesJob.schedule();
		
		setTitle(Messages.ColumnSelectorDialog_Title);
		getShell().setText(Messages.ColumnSelectorDialog_Title);
		setMessage(Messages.ColumnSelectorDialog_Message);
		
		return parent;
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}

	private Job loadAttributesJob = new Job(Messages.ColumnSelectorDialog_loadingJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<IntelAttribute> attributes = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				List<IntelEntityType> types = QueryFactory.buildQuery(session, IntelEntityType.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
				for (IntelEntityType type : types) {
					for (IntelEntityTypeAttribute a : type.getAttributes()) {
						if (!attributes.contains(a.getAttribute())) attributes.add(a.getAttribute());
					}
				}				
			}
			attributes.sort((a,b)->Collator.getInstance().compare(a.getName(), b.getName()));
			
			List<IntelAttribute> checked = new ArrayList<>();
			for (IntelAttribute c : attributes) {
				if (visibleColumns == null || visibleColumns.contains(c.getKeyId())) checked.add(c);
			}
			
			Display.getDefault().syncExec(()->{
				tblAttributes.setInput(attributes);
				//check
				tblAttributes.setCheckedElements(checked.toArray());
			});

			return Status.OK_STATUS;
		}
		
	};
}
