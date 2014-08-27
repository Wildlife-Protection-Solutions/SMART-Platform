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

public class TypePage extends WizardPage implements SelectionListener{
	
	private Button opPlots;
	private Button opTransects;
	
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
		opTransects.setText(Messages.TypePage_TransectsOption);
		opTransects.addSelectionListener(this);
		
		opPlots = new Button(c, SWT.CHECK);
		opPlots.setSelection(true);
		opPlots.setText(Messages.TypePage_PlotsOption);
		opPlots.addSelectionListener(this);
		
		setControl(main);
		
		setTitle(Messages.TypePage_Title);
		setMessage(Messages.TypePage_Message);
	}
	
	@Override
	public IWizardPage getNextPage(){
		if (!exportTransect() && !exportPlots()){
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

	@Override
	public void widgetSelected(SelectionEvent e) {
		getWizard().getContainer().updateButtons();
		
	}

	@Override
	public void widgetDefaultSelected(SelectionEvent e) {
		
	}
	
}
