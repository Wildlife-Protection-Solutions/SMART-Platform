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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.wcs.smart.ca.IconManager;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.internal.ca.datamodel.xml.DataModelXmlToSmartConverter;
import org.wcs.smart.ui.ConservationAreaLabelProvider;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.internal.ca.LanguageSelectionDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartUtils;


/**
 * Dialog page for initializing the data model.
 * 
 * @since 1.0.0
 */
public class InitCaDataModelDialog extends SmartStyledTitleDialog {

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

	private Path workingDirectory = null;
	
	/**
	 * @param parentShell
	 */
	public InitCaDataModelDialog(Shell parent, Session session) {
		super(parent);
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


	@Override
	public Control createDialogArea(Composite parent) {
		
		//ensure working directory is dispoed of properly when dialog is closed
		parent.addListener(SWT.Dispose, e->{
			try {
				if (workingDirectory != null) SmartUtils.deleteDirectory(workingDirectory);
			} catch (IOException ex) {
				SmartPlugIn.log(ex.getMessage(), ex);
			}
		});
		
		parent = (Composite) super.createDialogArea(parent);
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, true);
		layout.marginLeft = 15;
		layout.marginTop = 15;
		layout.verticalSpacing = 10;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		setTitle(Messages.InitCaDataModelDialog_PageTitle);
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
		btnUseIucn.setSelection(true);

		btnBlank = new Button(comp, SWT.RADIO);
		btnBlank.setText(Messages.InitCaDataModelDialog_Op_UseBlank);
		btnBlank.addSelectionListener(validateListener);
		btnBlank.setSelection(false);
		
		createImportFromCa(comp);
		createImportXml(comp, validateListener);

		return comp;
	}

	private List<Icon> getIcons(){
		List<Icon> icons = IconManager.INSTANCE.getIcons(session, ca);
		icons.addAll(IconManager.INSTANCE.getSystemIcons(session, ca));
		return icons;
	}
	
	private List<IconSet> getIconSets(){
		List<IconSet> sets = new ArrayList<>();
		sets.addAll(QueryFactory.buildQuery(session, IconSet.class,
				new Object[] {"conservationArea", ca}).list()); //$NON-NLS-1$
		
		return sets;
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
		btnImport.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.IMPORT_ICON));
		btnImport.setBackground(imp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnImport.setEnabled(false);
		btnImport.setText(DialogConstants.IMPORT_BUTTON_TEXT);
		btnImport.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if(caViewer != null){
					caViewer.getControl().setEnabled(false);
				}
				
				FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
				fd.setFilterExtensions(new String[]{"*.xml;*.zip", "*.xml", "*.zip", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				fd.setFilterNames(new String[]{DialogConstants.ZIP_XML_FILES,
						DialogConstants.XML_FILES, DialogConstants.ZIP_FILES, DialogConstants.ALL_FILES});
				
				String file = fd.open();
				if (file != null) {
					final Path f = Paths.get(file);
					try {
						ProgressMonitorDialog pmd = new ProgressMonitorDialog(
								getShell());
						pmd.run(false, false, new IRunnableWithProgress() {

							@Override
							public void run(IProgressMonitor monitor)
									throws InvocationTargetException,
									InterruptedException {
								
								try {
									if (workingDirectory != null) {
										SmartUtils.deleteDirectory(workingDirectory);
									}
									workingDirectory = Files.createTempDirectory("smartdm"); //$NON-NLS-1$

									DataModelXmlToSmartConverter converter =  new DataModelXmlToSmartConverter();
									dm = converter.convert(f, ca, getIcons(), getIconSets(), true, workingDirectory);
								} catch (Exception ex) {
									SmartPlugIn
											.displayLog(Messages.InitCaDataModelDialog_Error_CouldNotReadXml + "\n\n" + ex.getMessage(), //$NON-NLS-1$
													ex);
									dm = null;
								}
							}

						});

						lblFileName.setText(f.toAbsolutePath().normalize().toString());
					} catch (Exception ex) {
						SmartPlugIn.displayLog(Messages.InitCaDataModelDialog_Error_CouldNotReadXml +"\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
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
			btnClone.setSelection(false);

			imp = new Composite(comp, SWT.NONE);
			imp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			layout = new GridLayout(1, true);
			layout.marginLeft = 25;
			imp.setLayout(layout);

			caViewer = new ComboViewer(imp, SWT.DROP_DOWN | SWT.READ_ONLY);
			caViewer.getControl().setEnabled(false);
			caViewer.setContentProvider(ArrayContentProvider.getInstance());
			caViewer.setLabelProvider(new ConservationAreaLabelProvider());
			caViewer.setInput(areas.toArray());
			caViewer.setSelection(new StructuredSelection(areas.get(0)));
			GridData ld = new GridData(SWT.FILL, SWT.FILL, true, false);
			ld.widthHint = 120;
			caViewer.getControl().setLayoutData(ld);
			
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
		final boolean isIUCN = btnUseIucn.getSelection();
		ConservationArea caToCloneFromA = null;
		final boolean isCa = btnClone != null && btnClone.getSelection();
		if (isCa){
			caToCloneFromA = (ConservationArea) ((IStructuredSelection) caViewer.getSelection()).getFirstElement();
		}
		final ConservationArea caToCloneFrom = caToCloneFromA;
		
		final boolean isBlank = btnBlank.getSelection();
		try {
			
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(
					getShell());
			
			
			pmd.run(true, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException,
						InterruptedException {
					SubMonitor progress = null;
					if (isIUCN) {
						progress = SubMonitor.convert(monitor,Messages.InitCaDataModelDialog_Progress_LoadingDefaultDm, 1);
						InputStream is = SmartProperties.getIucnDataModelFile();
						try {
							try {
								DataModelXmlToSmartConverter converter = new DataModelXmlToSmartConverter();
								dm = converter.convert(is, ca, getIcons(), getIconSets(), true, workingDirectory);
							} finally {
								is.close();
							}
						} catch (Exception ex) {
							dm = null;
							throw new InvocationTargetException(ex);
						}
					}else if (isCa) {
						progress = SubMonitor.convert(monitor,Messages.InitCaDataModelDialog_Progress_ClosingDm, 3);
						
						DataModel dmToClone = null;
						getSession().beginTransaction();
						try{
							progress.subTask(MessageFormat.format(Messages.InitCaDataModelDialog_Progress_loadDataModel, caToCloneFrom.getName()));
							dmToClone = HibernateManager.loadDataModel(caToCloneFrom, getSession());
							progress.worked(1);
						}finally{
							getSession().getTransaction().commit();
						}
						
						if (dmToClone.getCategories().size() == 0) {
							dm = null;
							String error = MessageFormat.format(Messages.InitCaDataModelDialog_Error_Cloning, new Object[]{ ca.getNameLabel()}); 
							throw new InvocationTargetException(new IllegalStateException(error),error);
						}
						
						boolean hasLang = false;
						progress.subTask(Messages.InitCaDataModelDialog_Progress_CloneLanguages);
						for (Language lang: caToCloneFrom.getLanguages()){
							if (lang.getCode().equals(ca.getDefaultLanguage().getCode())){
								hasLang = true;
							}
						}
						String code = null;
						if (!hasLang){
							final String[] codes = new String[caToCloneFrom.getLanguages().size()];
							int index = 0;
							for (Language ll : caToCloneFrom.getLanguages()){
								codes[index++] = ll.getCode();
							}
							final String[] langCode = {null};
							Display.getDefault().syncExec(new Runnable(){

								@Override
								public void run() {
									LanguageSelectionDialog lsd = new LanguageSelectionDialog(getShell(), ca, codes);
									if (lsd.open() == IDialogConstants.OK_ID){
										langCode[0] = (String)((IStructuredSelection)lsd.getSelection()).getFirstElement();
									}
									
								}});
							if (langCode[0] == null){
								dm = null;
							}else{
								code = langCode[0];
							}
						}
						
						
						//find all the new icons 
						List<Icon> caIcons = getIcons();
						
						List<Icon> dmIcons = new ArrayList<>();
						dmToClone.getCategories().forEach(c->{
							c.accept(e->{
								if (e.getIcon() != null) dmIcons.add(e.getIcon());
								return true;
							});
						});
						dmToClone.getAttributes().forEach(a->{
							if (a.getIcon() != null) dmIcons.add(a.getIcon());
							if (a.getAttributeList() != null) {
								for (AttributeListItem item : a.getAttributeList()) {
									if (item.getIcon() != null) dmIcons.add(item.getIcon());
								}
							}
							if (a.getTree() != null) {
								for (AttributeTreeNode node : a.getTree()) {
									node.accept(e->{
										if (e.getIcon() != null) dmIcons.add(e.getIcon());
										return true;
									});		
								}
							}
							
						});
						//now for every icon in the dm look for the icon in the ca
						HashMap<String,Icon> caIconMap = new HashMap<>();
						caIcons.forEach(e->caIconMap.put(e.getKeyId(), e));
						List<IconSet> sets = getIconSets();
						for (Icon dmIcon : dmIcons) {
							if (!caIconMap.containsKey(dmIcon.getKeyId())) {
								//We want to create a icon for this and add it to our icon list
								Icon newIcon = new Icon();
								newIcon.setConservationArea(ca);
								newIcon.setKeyId(dmIcon.getKeyId());
								newIcon.setFiles(new ArrayList<>());

								//names and icon sets
								newIcon.updateName(ca.getDefaultLanguage(), dmIcon.getDefaultName());
								for (org.wcs.smart.ca.Label l : dmIcon.getNames()) {
									for (Language ll : ca.getLanguages()) {
										if (l.getLanguage().getCode().equalsIgnoreCase(ll.getCode())) {
											newIcon.updateName(ll, l.getValue());
										}
									}
								}
								
								for (IconSet is : sets) {
									IconFile tocopy = null;
									for (IconFile iconf : dmIcon.getFiles()) {
										if (iconf.getIconSet().getKeyId().equalsIgnoreCase(is.getKeyId())) {
											tocopy = iconf;
											break;
										}
									}
									if (tocopy == null) continue;
									
									IconFile newFile = new IconFile();
									newFile.setIcon(newIcon);
									newFile.setIconSet(is);
									tocopy.computeFileLocation(getSession());
									newFile.setCopyFromLocation(tocopy.getAttachmentFile());
									newFile.setFilename(tocopy.getFilename());
									newIcon.getFiles().add(newFile);
								}
								caIcons.add(newIcon);
							}
						}
						
						
						dm = dmToClone.clone(ca, code, caIcons, progress.split(1));
					}else if (isBlank){
						progress = SubMonitor.convert(monitor, 1);
						dm = new DataModel(ca, new ArrayList<Category>(), new ArrayList<Attribute>());
					}else {
						progress = SubMonitor.convert(monitor, 1);
					}
					
					if (dm == null){
						String error = Messages.InitCaDataModelDialog_Error_NoDataModel;
						throw new InvocationTargetException(new IllegalStateException(error), error);
					}
					monitor.subTask(Messages.InitCaDataModelDialog_Progress_SavingDm);
					dm.save(getSession(), progress.split(1));
					
					return ;
				}
			});
			return true;
		} catch (InvocationTargetException ex) {
			SmartPlugIn.displayLog(Messages.InitCaDataModelDialog_Error_CouldNotSaveDm + ex.getCause().getLocalizedMessage(), ex);
		}catch(Exception ex){
			SmartPlugIn.displayLog(Messages.InitCaDataModelDialog_Error_CouldNotSaveDm + ex.getCause(), ex);
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
