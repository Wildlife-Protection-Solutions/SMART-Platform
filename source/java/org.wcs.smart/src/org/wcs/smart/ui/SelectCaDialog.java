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
package org.wcs.smart.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.ConservationAreaConfiguration;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

/**
 * Dialog for changing the current conservation areas used
 * in the cross ca analysis
 * @author Emily
 *
 */
public class SelectCaDialog extends TitleAreaDialog {

	private static final String ERROR_DIALOG_TITLE = Messages.SelectCaDialog_ErrorDialogtitle;

	private static final String CA_SELECT_MULTI_ERROR = Messages.SelectCaDialog_CaError;

	private CheckboxTableViewer caList; 
	
	private ConservationAreaConfiguration newConfiguration;
	
	public SelectCaDialog(Shell parentShell) {
		super(parentShell);
	}

	public ConservationAreaConfiguration getNewConfiguration(){
		return this.newConfiguration;
	}
	
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId){
			List<ConservationArea> cas = new ArrayList<ConservationArea>();
			Object[] selections = caList.getCheckedElements();
			if (selections.length <= 1){
				MessageDialog.openInformation(getShell(), ERROR_DIALOG_TITLE, CA_SELECT_MULTI_ERROR);
				return;
			}
			for (Object x : selections){
				ConservationArea ca = (ConservationArea)x;
				cas.add(ca);
			}
			//employee list remains the same as you want to 
			//access all queries/reports saved by any user
			Session s = HibernateManager.openSession();
			try{
				newConfiguration = new ConservationAreaConfiguration(SmartDB.getCurrentConservationArea(), 
						cas, 
						SmartDB.getCurrentEmployee(),
					SmartDB.getConservationAreaConfiguration().getEmployees(),
					s);
			}finally{
				if (s.isOpen()) s.close();
			}
		}
		super.buttonPressed(buttonId);
		
	}
	
	protected Control createDialogArea(Composite parent) {
		Composite main = (Composite) super.createDialogArea(parent);
		
		Composite comp = new Composite(main, SWT.NONE);
		comp.setLayout(new GridLayout());
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(comp, SWT.NONE);
		l.setText(Messages.SelectCaDialog_CaLabel);
		
		caList = CheckboxTableViewer.newCheckList(comp, SWT.CHECK | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.MULTI);
		caList.setLabelProvider(new ConservationAreaLabelProvider());
		caList.getTable().addKeyListener(new KeyListener(){
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == ' '){
					boolean value = caList.getChecked(   ((IStructuredSelection)caList.getSelection()).getFirstElement() );
					for (Iterator<?> iterator = ((IStructuredSelection)caList.getSelection()).iterator(); iterator.hasNext();) {
						Object tp = (Object) iterator.next();
						caList.setChecked(tp, !value);
					}
					e.doit = false;
					validate();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});
		caList.setContentProvider(ArrayContentProvider.getInstance());
		caList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)caList.getControl().getLayoutData()).heightHint = 250;
		((GridData)caList.getControl().getLayoutData()).widthHint = 400;
		caList.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});
		
		Composite links = new Composite(comp, SWT.NONE);
		links.setLayout(new GridLayout(3, false));
		
		Link selectAll = new Link(links, SWT.NONE);
		selectAll.setText("<a>" + Messages.SelectCaDialog_SelectAllOption + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		selectAll.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				select(true);
			}
		});
		l = new Label(links, SWT.SEPARATOR | SWT.VERTICAL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false);
		gd.heightHint = selectAll.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		l.setLayoutData(gd);
		
		Link deselectAll = new Link(links,  SWT.NONE);
		deselectAll.setText("<a>" + Messages.SelectCaDialog_DeselectAllOption + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		deselectAll.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				select(false);
			}
		});
		setMessage(Messages.SelectCaDialog_SelectLabel);
		setTitle(Messages.SelectCaDialog_DialogTitle);
		getShell().setText(Messages.SelectCaDialog_ShellTitle);
		
		caList.setInput(new String[]{Messages.SelectCaDialog_Loading});
		
		Job getCaJob = new Job("get ca list job"){ //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					List<ConservationArea> cas = HibernateManager
							.findConservationAreas(SmartDB.getCurrentEmployee().getSmartUserId(),  SmartDB.getPlainTextPassword());
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							if (caList.getControl().isDisposed()) return;
							caList.setInput(cas);
							caList.setAllChecked(false);
							for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
								caList.setChecked(ca, true);
							}
						}
					});	
				} catch (Exception e) {
					SmartPlugIn.log(e.getMessage(), e);
				}
				return Status.OK_STATUS;
			}
			
		};
		getCaJob.setSystem(true);
		getCaJob.schedule();
		return comp;
	}
	
	private void select(boolean all){
		caList.setAllChecked(all);
		validate();
	}
	
	private void validate(){
		String error = null;
		if (caList.getCheckedElements().length < 2){
			error = CA_SELECT_MULTI_ERROR;
		}
		setErrorMessage(error);
		getButton(IDialogConstants.OK_ID).setEnabled(error == null);
	}
	
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
