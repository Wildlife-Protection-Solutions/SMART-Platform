package org.wcs.smart.er.ui.samplingunit.export.wizard;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.er.internal.Messages;
import org.wcs.smart.er.model.SamplingUnit.SamplingUnitType;

public class TypePage extends WizardPage implements SelectionListener{
	
	private Button opPlots;
	private Button opTransects;
	private Button opRecon;
	
	public TypePage(){
		super("TYPE_PAGE"); //$NON-NLS-1$
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		
		Composite c = new Composite(main, SWT.NONE);
		c.setLayout(new GridLayout(1, false));
		c.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		opTransects = new Button(c, SWT.CHECK);
		opTransects.setSelection(true);
		opTransects.setText(SamplingUnitType.TRANSECT.getGuiName());
		opTransects.addSelectionListener(this);
		
		opPlots = new Button(c, SWT.CHECK);
		opPlots.setSelection(true);
		opPlots.setText(SamplingUnitType.PLOT.getGuiName());
		opPlots.addSelectionListener(this);
		
		opRecon = new Button(c, SWT.CHECK);
		opRecon.setSelection(true);
		opRecon.setText(SamplingUnitType.RECON.getGuiName());
		opRecon.addSelectionListener(this);
		
		setControl(main);
		
		setTitle(Messages.TypePage1_Title);
		setMessage(Messages.TypePage1_Message);
	}
	
	@Override
	public IWizardPage getNextPage(){
		if (!exportTransect() && !exportPlots() && !exportRecon()){
			return null;
		}
		return super.getNextPage();
	}
	
	public boolean exportTransect(){
		return opTransects.getSelection();
	}
	
	public boolean exportPlots(){
		return opPlots.getSelection();
	}

	public boolean exportRecon(){
		return opRecon.getSelection();
	}
	
	@Override
	public void widgetSelected(SelectionEvent e) {
		getWizard().getContainer().updateButtons();
		
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		
	}
	
}
