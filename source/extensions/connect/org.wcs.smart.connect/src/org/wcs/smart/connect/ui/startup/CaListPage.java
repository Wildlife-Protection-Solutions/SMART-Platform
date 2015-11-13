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
import org.wcs.smart.connect.model.ConnectServer;

public class CaListPage extends WizardPage implements ISelectionChangedListener{

	private ListViewer cmbList;
	
	public CaListPage(){
		super("CALIST");
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		outer.setLayout(new GridLayout());
		
		
		Label l = new Label(outer, SWT.NONE);
		l.setText("SMART Connect Conservation Areas:");
	
		cmbList = new ListViewer(outer);
		cmbList.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		cmbList.setContentProvider(ArrayContentProvider.getInstance());
		cmbList.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof ConservationAreaProxy){
					return ((ConservationAreaProxy) element).getLabel();
				}
				return super.getText(element);
			}
		});
		cmbList.setInput(new String[]{"Loading..."});
		cmbList.addSelectionChangedListener(this);
		
		
		setTitle("Conservation Area");
		setMessage("Select the Conservation Area to download and import.");
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
					cmbList.setInput(dataca);		
					cmbList.refresh();
				}
			});
		}catch(Exception ex){
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					cmbList.setInput(new String[]{"Error"});
					setErrorMessage("Could not connect to server.  Ensure the url, certificate file, username and password are valid.");
				}
			});
			
			ConnectPlugIn.log("Could not connect to server.  Ensure the url, username and password are valid." + "\n\n" + ex.getMessage(), ex);
			
		}
		
	}
	
	public void clearList(){
		cmbList.setInput(new String[]{"Loading..."});
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
