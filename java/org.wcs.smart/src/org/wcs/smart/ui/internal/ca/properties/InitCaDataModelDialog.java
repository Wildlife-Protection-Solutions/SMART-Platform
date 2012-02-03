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
package org.wcs.smart.ui.internal.ca.properties;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelXmlToSmartConverter;
import org.wcs.smart.ui.internal.ca.LanguageSelectionDialog;

/**
 * Dialog page for initializing the data model.
 * 
 * @since 1.0.0
 */
public class InitCaDataModelDialog extends TitleAreaDialog {

	private Button btnImport;
	private Button btnUseCustom;
	private Button btnUseIucn;
	private Label lblFileName;
	private ComboViewer caViewer;

	private ConservationArea ca;
	private DataModel dm = null;

	private Session session = null;
	private Button btnClone;

	/**
	 * @param parentShell
	 */
	public InitCaDataModelDialog() {
		super(Display.getCurrent().getActiveShell());
		this.ca = SmartDB.getCurrentConservationArea();

		session = HibernateManager.openSession();
		session.refresh(ca);
	}

	private Session getSession() {
		if (session == null || !session.isOpen()) {
			session = HibernateManager.openSession();
		}
		return session;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Initialize Conservation Area Data Model");
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	public Control createDialogArea(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, true);
		layout.marginLeft = 15;
		layout.marginTop = 15;
		layout.verticalSpacing = 10;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		setMessage("No data model has been defined.  Please select the initial data model (further modifications can be made later):");

		SelectionListener validateListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				btnImport.setEnabled(btnUseCustom.getSelection());
				lblFileName.setEnabled(btnUseCustom.getSelection());
				if(caViewer != null){
					caViewer.getControl().setEnabled(false);
				}
				validate();
			}
		};
		btnUseIucn = new Button(comp, SWT.RADIO);
		btnUseIucn.setText("Use the ICUN data model");
		btnUseIucn.addSelectionListener(validateListener);
		btnUseIucn.setSelection(false);

		createImportFromCa(comp);
		createImportXml(comp, validateListener);

		return comp;
	}

	private void createImportXml(Composite comp,
			SelectionListener validateListener) {
		GridLayout layout;
		btnUseCustom = new Button(comp, SWT.RADIO);
		btnUseCustom.setText("Import a custom data model.");
		btnUseCustom.addSelectionListener(validateListener);
		btnUseCustom.setSelection(false);

		Composite imp = new Composite(comp, SWT.NONE);
		imp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		layout = new GridLayout(1, true);
		layout.marginLeft = 25;
		imp.setLayout(layout);

		btnImport = new Button(imp, SWT.PUSH);
		btnImport.setEnabled(false);
		btnImport.setText("Import XML ...");
		btnImport.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if(caViewer != null){
					caViewer.getControl().setEnabled(false);
				}
				
				FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
				String file = fd.open();
				if (file != null) {
					final File f = new File(file);
					try {
						ProgressMonitorDialog pmd = new ProgressMonitorDialog(
								getShell());
						pmd.run(false, false, new IRunnableWithProgress() {

							@Override
							public void run(IProgressMonitor monitor)
									throws InvocationTargetException,
									InterruptedException {
								try {
									DataModelXmlToSmartConverter converter =  new DataModelXmlToSmartConverter();
									dm = converter.convert(f, ca, true);
								} catch (Exception ex) {
									SmartPlugIn
											.displayLog(getShell(),
													"Could not read data model xml file.",
													ex);
									dm = null;
								}
							}

						});

						lblFileName.setText(f.getAbsolutePath());
					} catch (Exception ex) {
						SmartPlugIn.displayLog(getShell(),
								"Could not read data model xml file.", ex);
						dm = null;
					}
				}
				validate();
			}
		});
		lblFileName = new Label(imp, SWT.NONE);
		lblFileName
				.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
	}

	private void createImportFromCa(Composite comp) {
		GridLayout layout;
		Composite imp;

		List<ConservationArea> areas = HibernateManager.getConservationAreas();
		areas.remove(this.ca);
		if (areas.size() > 0) {
			btnClone = new Button(comp, SWT.RADIO);
			btnClone.setText("Copy DataModel From Existing Conservation Area");
			btnClone.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					caViewer.getControl().setEnabled(true);
					validate();
				}
			});

			imp = new Composite(comp, SWT.NONE);
			imp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			layout = new GridLayout(1, true);
			layout.marginLeft = 25;
			imp.setLayout(layout);

			caViewer = new ComboViewer(imp, SWT.DROP_DOWN | SWT.READ_ONLY);
			caViewer.getControl().setEnabled(false);
			caViewer.setContentProvider(ArrayContentProvider.getInstance());
			caViewer.setLabelProvider(new LabelProvider() {
				public String getText(Object element) {
					if (element instanceof ConservationArea) {
						ConservationArea ca = (ConservationArea) element;
						return ca.getId() + " - " + ca.getName();
					}
					return super.getText(element);
				}
			});

			caViewer.setInput(areas.toArray());
			caViewer.setSelection(new StructuredSelection(areas.get(0)));
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.FINISH_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	private boolean saveDataModel() {

		try {
			if (btnUseIucn.getSelection()) {
				// import conservation area
				try {
					ProgressMonitorDialog pmd = new ProgressMonitorDialog(
							getShell());
					pmd.run(false, false, new IRunnableWithProgress() {

						@Override
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {
							InputStream is = SmartProperties.getIucnDataModelFile();
							try {
								try {
									DataModelXmlToSmartConverter converter = new DataModelXmlToSmartConverter();
									dm = converter.convert(is, ca, true);
								} finally {
									is.close();
								}
							} catch (Exception ex) {
								SmartPlugIn.displayLog(getShell(),ex.getMessage(), ex);
								dm = null;
							}
						}
					});
					
				} catch (Exception ex) {
					SmartPlugIn.displayLog(getShell(),ex.getMessage(), ex);
					return false;
				}
			} else if (btnClone.getSelection()) {
				// clone existing conservation area
				ProgressMonitorDialog pmd = new ProgressMonitorDialog(
						getShell());
				pmd.run(false, false, new IRunnableWithProgress() {

					@Override
					public void run(IProgressMonitor monitor)
							throws InvocationTargetException,
							InterruptedException {

						ConservationArea caToCloneFrom = (ConservationArea) ((IStructuredSelection) caViewer.getSelection()).getFirstElement();
						DataModel dmToClone = HibernateManager.loadDataModel(caToCloneFrom, getSession());
						
						if (dmToClone.getCategories().size() == 0) {
							MessageDialog
									.openError(
											getShell(),
											"Data Model Not Defined",
											"The conservation area '"
													+ caToCloneFrom.getId()
													+ " - "
													+ caToCloneFrom.getName()
													+ "' does not have a defined data model.  You cannot copy from this conservation area.");
							dm = null;
							return;
						}
						//TODO: this needs to be tested when we support mulitple languages
						boolean hasLang = false;
						getSession().refresh(caToCloneFrom);
						for (Language lang: caToCloneFrom.getLanguages()){
							if (lang.getCode().equals(ca.getDefaultLanguage().getCode())){
								hasLang = true;
							}
						}
						String code = null;
						if (!hasLang){
							String[] codes = new String[caToCloneFrom.getLanguages().size()];
							int index = 0;
							for (Language ll : caToCloneFrom.getLanguages()){
								codes[index++] = ll.getCode();
							}
							LanguageSelectionDialog lsd = new LanguageSelectionDialog(getShell(), ca, codes);
							if (lsd.open() != IDialogConstants.OK_ID){
								dm = null;
								return;
							}
							code = (String)((IStructuredSelection)lsd.getSelection()).getFirstElement();
						}
						dm = dmToClone.clone(ca, code);
						
					}

				});

			}
			if (dm == null) {
				return false;
			}

		

			Session s = getSession();
			s.beginTransaction();
			dm.save(s);
			s.getTransaction().commit();
			return true;
		} catch (Exception ex) {
			SmartPlugIn.displayLog(getShell(),"Could not save data model.", ex);
		}
		return false;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		if (IDialogConstants.OK_ID == buttonId) {
			setReturnCode(OK);
			if (!saveDataModel()) {
				return;
			}
		} else if (IDialogConstants.CANCEL_ID == buttonId) {
			setReturnCode(CANCEL);
		}
		getSession().close();

		close();
	}

	private void validate() {
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		if (btnUseCustom.getSelection()) {
			if (dm != null) {
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
		} else if (btnUseIucn.getSelection() | (btnClone != null && btnClone.getSelection())) {
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		}
	}
	
	
}
