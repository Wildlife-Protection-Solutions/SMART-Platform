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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
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
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
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
	private Button btnBlank;
	
	private Label lblFileName;
	private ComboViewer caViewer;

	private ConservationArea ca;
	private DataModel dm = null;

	private Session session = null;
	private Button btnClone;

	/**
	 * @param parentShell
	 */
	public InitCaDataModelDialog(Session session) {
		super(Display.getCurrent().getActiveShell());
		this.ca = SmartDB.getCurrentConservationArea();
		this.session = session;
	}

	private Session getSession() {
		
		return session;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(Messages.InitCaDataModelDialog_Dialog_Title);
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	public DataModel getDataModel(){
		return this.dm;
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

		setMessage(Messages.InitCaDataModelDialog_NoDataModel_Message);

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
		btnUseIucn.setText(Messages.InitCaDataModelDialog_Op_UseIUCN);
		btnUseIucn.addSelectionListener(validateListener);
		btnUseIucn.setSelection(false);

		btnBlank = new Button(comp, SWT.RADIO);
		btnBlank.setText(Messages.InitCaDataModelDialog_Op_UseBlank);
		btnBlank.addSelectionListener(validateListener);
		btnBlank.setSelection(false);
		
		createImportFromCa(comp);
		createImportXml(comp, validateListener);

		return comp;
	}

	private void createImportXml(Composite comp,
			SelectionListener validateListener) {
		GridLayout layout;
		btnUseCustom = new Button(comp, SWT.RADIO);
		btnUseCustom.setText(Messages.InitCaDataModelDialog_Op_ImportCustom);
		btnUseCustom.addSelectionListener(validateListener);
		btnUseCustom.setSelection(false);

		Composite imp = new Composite(comp, SWT.NONE);
		imp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		layout = new GridLayout(1, true);
		layout.marginLeft = 25;
		imp.setLayout(layout);

		btnImport = new Button(imp, SWT.PUSH);
		btnImport.setEnabled(false);
		btnImport.setText(Messages.InitCaDataModelDialog_ImportXML_Button);
		btnImport.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if(caViewer != null){
					caViewer.getControl().setEnabled(false);
				}
				
				FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
				fd.setFilterExtensions(new String[]{"*.xml", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
				fd.setFilterNames(new String[]{Messages.InitCaDataModelDialog_XmlFileFilter_Name, Messages.InitCaDataModelDialog_AllFiles_FilterName});
				
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
													Messages.InitCaDataModelDialog_Error_CouldNotReadXml,
													ex);
									dm = null;
								}
							}

						});

						lblFileName.setText(f.getAbsolutePath());
					} catch (Exception ex) {
						SmartPlugIn.displayLog(getShell(),
								Messages.InitCaDataModelDialog_Error_CouldNotReadXml, ex);
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

		List<ConservationArea> areas = HibernateManager.getConservationAreas(session);
		areas.remove(this.ca);
		if (areas.size() > 0) {
			btnClone = new Button(comp, SWT.RADIO);
			btnClone.setText(Messages.InitCaDataModelDialog_Op_CopyExistingCa);
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
						return ca.getId() + " - " + ca.getName(); //$NON-NLS-1$
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
			
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(
					getShell());
			
			
			pmd.run(false, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException,
						InterruptedException {
					
					if (btnUseIucn.getSelection()) {
						monitor.setTaskName(Messages.InitCaDataModelDialog_Progress_LoadingDefaultDm);
						InputStream is = SmartProperties.getIucnDataModelFile();
						try {
							try {
								DataModelXmlToSmartConverter converter = new DataModelXmlToSmartConverter();
								dm = converter.convert(is, ca, true);
							} finally {
								is.close();
							}
						} catch (Exception ex) {
							dm = null;
							throw new InvocationTargetException(ex);
						}
					}else if (btnClone != null && btnClone.getSelection()) {
						//clone from another data model
						monitor.setTaskName(Messages.InitCaDataModelDialog_Progress_ClosingDm);
						ConservationArea caToCloneFrom = (ConservationArea) ((IStructuredSelection) caViewer.getSelection()).getFirstElement();
						DataModel dmToClone = null;
						getSession().beginTransaction();
						try{
							dmToClone = HibernateManager.loadDataModel(caToCloneFrom, getSession());
						}finally{
							getSession().getTransaction().commit();
						}
						
						if (dmToClone.getCategories().size() == 0) {
							dm = null;
							String error = MessageFormat.format(Messages.InitCaDataModelDialog_Error_Cloning, new Object[]{ caToCloneFrom.getId() + " - " + caToCloneFrom.getName()});  //$NON-NLS-1$
							throw new InvocationTargetException(new IllegalStateException(error),error);
						}
						//TODO: this needs to be tested when we support multiple languages
						boolean hasLang = false;

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
							}
							code = (String)((IStructuredSelection)lsd.getSelection()).getFirstElement();
						}
//						for (Iterator iterator = dmToClone.getAttributes().iterator(); iterator.hasNext();) {
//							Attribute type = (Attribute) iterator.next();
//							if (type.getTree() != null){
//								for (AttributeTreeNode node : type.getTree()){
//									System.out.println(node.getName());
//								}
//							}
//							
//						}
						dm = dmToClone.clone(ca, code);
					}else if (btnBlank.getSelection()){
						dm = new DataModel(ca, new ArrayList<Category>(), new ArrayList<Attribute>());
					}
					
					if (dm == null){
						String error = Messages.InitCaDataModelDialog_Error_NoDataModel;
						throw new InvocationTargetException(new IllegalStateException(error), error);
					}
					monitor.setTaskName(Messages.InitCaDataModelDialog_Progress_SavingDm);
					dm.save(getSession(), monitor);
					return ;
				}
			});
			return true;
		} catch (Exception ex) {
			SmartPlugIn.displayLog(getShell(),Messages.InitCaDataModelDialog_Error_CouldNotSaveDm + ex.getLocalizedMessage(), ex);
	
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

		close();
	}

	private void validate() {
		getButton(IDialogConstants.OK_ID).setEnabled(false);
		if (btnUseCustom.getSelection()) {
			if (dm != null) {
				getButton(IDialogConstants.OK_ID).setEnabled(true);
			}
		} else if (btnUseIucn.getSelection() || (btnClone != null && btnClone.getSelection())) {
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		} else if (btnBlank.getSelection()){
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		}
	}
	
	
}
