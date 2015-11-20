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
package org.wcs.smart.connect.ui.startup;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.api.model.ConservationAreaProxy;
import org.wcs.smart.connect.api.model.ConservationAreaProxy.Status;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;

/**
 * Lists conservation area from a smart connect server.
 * 
 * @author Emily
 *
 */
public class ConnectCaListPage extends WizardPage implements ISelectionChangedListener{

	private ListViewer cmbList;
	
	public ConnectCaListPage(){
		super("CALIST"); //$NON-NLS-1$
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		outer.setLayout(new GridLayout());
		
		
		Label l = new Label(outer, SWT.NONE);
		l.setText(Messages.ConnectCaListPage_CaLabel);
	
		cmbList = new ListViewer(outer);
		cmbList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)cmbList.getList().getLayoutData()).heightHint = 100;
		cmbList.setContentProvider(ArrayContentProvider.getInstance());
		cmbList.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof ConservationAreaProxy){
					return ((ConservationAreaProxy) element).getLabel();
				}
				return super.getText(element);
			}
		});
		cmbList.setInput(new String[]{Messages.ConnectCaListPage_LoadingLabel});
		cmbList.addSelectionChangedListener(this);
		
		
		setTitle(Messages.ConnectCaListPage_Title);
		setMessage(Messages.ConnectCaListPage_Message);
		setControl(outer);
	}

	public void initList(ConnectServer temp, String username, String password){
		try{
			SmartConnect connect = SmartConnect.findInstance(temp, username, password);		
			setErrorMessage(null);
			final List<ConservationAreaProxy> data = connect.getConservationAreas();
			final List<ConservationAreaProxy> dataca = new ArrayList<ConservationAreaProxy>();
			for (ConservationAreaProxy p : data){
				if (p.getStatus() == Status.DATA){
					dataca.add(p);
				}
			}
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					if (dataca.isEmpty()){
						setErrorMessage(Messages.ConnectCaListPage_NoCa);
					}
					cmbList.setInput(dataca);		
					cmbList.refresh();
				}
			});
		}catch(Exception ex){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					cmbList.setInput(new String[]{Messages.ConnectCaListPage_ErrorLabel});
					setErrorMessage(Messages.ConnectCaListPage_CouldNotConnect1);
				}
			});
			
			ConnectPlugIn.log(Messages.ConnectCaListPage_CouldNotConnect2 + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			
		}
		
	}
	
	public void clearList(){
		cmbList.setInput(new String[]{Messages.ConnectCaListPage_LoadingLabel});
	}
	
	public boolean isPageComplete(){
		if (!super.isPageComplete()){
			return false;
		}
		return getSelection() != null;
	}
	
	public ConservationAreaProxy getSelection(){
		if (cmbList.getSelection().isEmpty() ) return null;
		Object x = ((IStructuredSelection)cmbList.getSelection()).getFirstElement();
		if (x instanceof ConservationAreaProxy){
			return (ConservationAreaProxy)x ;
		}
		return null;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		if (getContainer() != null) getContainer().updateButtons();
	}

}
