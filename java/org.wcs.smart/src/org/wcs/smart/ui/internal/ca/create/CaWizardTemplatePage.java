package org.wcs.smart.ui.internal.ca.create;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.application.DisplayAccess;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.startup.SmartStartUp;
import org.wcs.smart.ui.ConservationAreaLabelProvider;

public class CaWizardTemplatePage  extends CaWizardPage  {
	
	public static final String PAGE_NAME = "CA_TEMPLATE"; //$NON-NLS-1$
	
	private ComboViewer lstCa;
	
	private ConservationArea templateCa;
	private Button btnTemplate;
	private Button btnNew;
	
	/**
	 * Create the wizard.
	 */
	public CaWizardTemplatePage() {
		super(PAGE_NAME);
		setImageDescriptor(SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.SMART_48_ICON));
		setTitle(Messages.CaWizardTemplatePage_DialogTitle);
		setDescription(Messages.CaWizardTemplatePage_DialogMessage);
	}

	/**
	 * Create contents of the wizard.
	 * 
	 * @param parent
	 */
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent,  SWT.NONE);
		composite.setLayout(new GridLayout());
		
		Composite center = new Composite(composite,  SWT.NONE);
		center.setLayout(new GridLayout());
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		btnNew = new Button(center, SWT.RADIO);
		btnNew.setText(Messages.CaWizardTemplatePage_OpBlank);
		
		btnTemplate = new Button(center, SWT.RADIO);
		btnTemplate.setText(Messages.CaWizardTemplatePage_OpTemplate);
		
		Composite templateComp = new Composite(center, SWT.NONE);
		GridLayout gl = new GridLayout(2, false);
		gl.marginLeft = 20;
		templateComp.setLayout(gl);
		templateComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		final Label l = new Label(templateComp, SWT.NONE);
		l.setText(Messages.CaWizardTemplatePage_LblTemplate);
		
		lstCa = new ComboViewer(templateComp);
		lstCa.setContentProvider(ArrayContentProvider.getInstance());
		lstCa.setLabelProvider(new ConservationAreaLabelProvider());
		lstCa.setInput(new String[]{Messages.CaWizardTemplatePage_LoadingLabel});
		lstCa.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnNew.setSelection(true);
		
		lstCa.getControl().setEnabled(false);
		l.setEnabled(false);
		
		Listener listener = new Listener(){

			@Override
			public void handleEvent(Event event) {
				lstCa.getControl().setEnabled(!btnNew.getSelection());
				l.setEnabled(!btnNew.getSelection());
				validate();
			}};
			
		btnNew.addListener(SWT.Selection, listener);
		btnTemplate.addListener(SWT.Selection, listener);
		
		loadConservationAreas();
		validate();
		
		super.setControl(composite);
	}

	/**
	 * Validate the input fields
	 */
	private void validate() {
		String error = null;
		if (btnTemplate.getSelection()){
			if (!(((StructuredSelection)lstCa.getSelection()).getFirstElement() instanceof ConservationArea)){
				error = Messages.CaWizardTemplatePage_TemplateRequired;
			}
		}
		super.setErrorMessage(error);
	}

	/**
	 * Updates the conservation area with the information from this wizard page.
	 * 
	 * @param ca Conservation Area object to update
	 */
	public void updateConservationArea(ConservationArea ca) {
		if (btnNew.getSelection()){
			templateCa = null;
			ca.setDescription(null);
			ca.setId(null);
			ca.setDesignation(null);
			ca.getLanguages().clear();
			ca.setName(null);
			return;
		}
		
		templateCa = (ConservationArea) ((StructuredSelection)lstCa.getSelection()).getFirstElement();
		ca.setId(templateCa.getId());
		ca.setName(templateCa.getName());
		ca.setDescription(templateCa.getDescription());
		ca.setDesignation(templateCa.getDesignation());
		
		ca.getLanguages().clear();
		for (Language l : templateCa.getLanguages()){
			Language clone = new Language();
			clone.setCa(ca);
			clone.setCode(l.getCode());
			clone.setDefault(l.isDefault());
			ca.getLanguages().add(clone);
		}
	}
	
	public ConservationArea getTemplateCa(){
		return this.templateCa;
	}
	public void initControls(ConservationArea ca){
		validate();
	}
	
	private void loadConservationAreas() {
		final Shell currentShell = getWizard().getContainer().getShell();

		Runnable r = new Runnable() {
			@Override
			public void run() {
				DisplayAccess.accessDisplayDuringStartup();

				final List<Object> cas = new ArrayList<Object>();
				Exception error = null;
				try {
					cas.addAll(SmartStartUp.getConservationAreas(false));
				} catch (final Exception ex) {
					error = ex;
				}
				final Exception lerror = error;

				currentShell.getDisplay().syncExec(new Runnable() {
					@Override
					public void run() {
						if (lerror == null) {
							lstCa.setInput(cas);
							lstCa.getCombo().select(0);
						} else {
							SmartPlugIn.displayLog(currentShell,
									lerror.getMessage(), lerror);
						}

					}
				});

			}
		};
		Thread t = new Thread(r);
		t.setContextClassLoader(Thread.currentThread().getContextClassLoader());
		t.start();

	}
}
