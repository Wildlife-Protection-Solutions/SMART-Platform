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
package org.wcs.smart.ui.map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.ReprojectUtils;

/**
 * Dialog for select the map projection.
 * @author egouge
 *
 */
public class ProjectionDialog extends TitleAreaDialog {

	private ListViewer lst ;
	private Projection selection;
	private CoordinateReferenceSystem defaultCrs;
	
	/**
	 * 
	 * @param parentShell
	 * @param current current projection or null if does not exist
	 */
	public ProjectionDialog(Shell parentShell, CoordinateReferenceSystem defaultCrs) {
		super(parentShell);
		this.defaultCrs = defaultCrs;
	}
	
	/**
	 * 
	 * @return the projection selected by the user
	 */
	public Projection getSelection(){
		return this.selection;
	}
	 
	@Override
	protected void okPressed(){
		selection = (Projection) ((IStructuredSelection)lst.getSelection()).getFirstElement();
		super.okPressed();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		 Composite composite = (Composite)super.createDialogArea(parent);
	
		 lst = new ListViewer(composite, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		 lst.setContentProvider(ArrayContentProvider.getInstance());
		 lst.setLabelProvider(new LabelProvider(){
			 @Override
			 public String getText(Object element){
				 if (element instanceof Projection){
					 return ((Projection)element).getName();
				 }
				 return super.getText(element);
			 }
		 });
		 GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		 gd.heightHint = 150;
		 lst.getList().setLayoutData(gd);
		 Job j = new Job(Messages.ProjectionDialog_LoadProjection_JobName) {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				s.beginTransaction();
				try{
					final Object[] ps = HibernateManager.getCaProjectionList(s).toArray();
					getShell().getDisplay().asyncExec(new Runnable(){

						@Override
						public void run() {
							lst.setInput(ps);
							try{
								Projection selection = null;
								for (Object x : ps){
									if (x instanceof Projection && ReprojectUtils.stringToCrs(((Projection) x).getDefinition()).equals(defaultCrs)){
										selection = (Projection) x;
									}
								}
								if (selection == null && ps.length > 0){
									selection = (Projection) ps[0];
								}	
								if (selection != null){
									lst.setSelection(new StructuredSelection(selection));
								}
							}catch (Exception ex){
								//eatme
							}
						}});
						
				}catch (final Exception ex){
					getShell().getDisplay().syncExec(new Runnable(){

						@Override
						public void run() {
							SmartPlugIn.displayLog(Messages.ProjectionDialog_Error_LoadProjectionMessage + ex.getLocalizedMessage(), ex);							
						}});
					
				}finally{
					s.getTransaction().rollback();
					s.close();
				}
				return Status.OK_STATUS;
			}
		};
		j.schedule();
		 
		lst.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean enabled = false;
				if (!lst.getSelection().isEmpty()){
					Object x = ((IStructuredSelection)lst.getSelection()).getFirstElement();
					if (x instanceof Projection){
						enabled = true;
					}
				}
				getButton(IDialogConstants.OK_ID).setEnabled(enabled);
				
			}
		});
		 
		 getShell().setText(Messages.ProjectionDialog_DialogTitle);
		 setTitle(Messages.ProjectionDialog_DialogTitle);
		 setMessage(Messages.ProjectionDialog_DialogMessage);
		 return composite;
	 }
	
	@Override
	protected void createButtonsForButtonBar(Composite parent){
		super.createButtonsForButtonBar(parent);
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
