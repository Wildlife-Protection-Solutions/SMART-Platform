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
package org.wcs.smart.dataentry.dialog;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmDmAttributeSettings;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.dataentry.model.xml.CmSmartToXmlConverter;
import org.wcs.smart.dataentry.model.xml.CmXmlManager;
import org.wcs.smart.dataentry.model.xml.CmXmlToSmartImporter;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.SmartFileUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Dialog for viewing Configurable Models.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ConfigurableModelPropertyDialog extends AbstractPropertyJHeaderDialog {

	private static final int DIALOG_WIDTH = 500;
	private static final int DIALOG_HEIGHT = 550;
	
	private TableViewer modelListViewer;
	private TreeViewer modelTreeViewer;
	
	private Button btnNew;
	private Button btnEdit;
	private Button btnDelete;
	private Button btnExport;
	private Button btnImport;

	private LoadCmModelJob loadCmModelJob = new LoadCmModelJob();
	
	public ConfigurableModelPropertyDialog(Shell parent) {
		super(parent, Messages.ConfigurableModelPropertyDialog_Title);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(DIALOG_WIDTH, DIALOG_HEIGHT);
	}
	
	@Override
	protected Composite createContent(Composite parent) {
		
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout());
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SashForm form = new SashForm(container, SWT.HORIZONTAL);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		modelListViewer = new TableViewer(form, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		modelListViewer.setLabelProvider(new ConfigurableModelLabelProvider());
		modelListViewer.setContentProvider(ArrayContentProvider.getInstance());
		modelListViewer.setInput(getModelsList().toArray());
		modelListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		modelListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateTreeViewer(false);
				btnEdit.setEnabled(!modelListViewer.getSelection().isEmpty());
				btnDelete.setEnabled(!modelListViewer.getSelection().isEmpty());
				btnExport.setEnabled(!modelListViewer.getSelection().isEmpty());
			}
		});
		
		modelTreeViewer = new TreeViewer(form, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		modelTreeViewer.setLabelProvider(new ConfigurableModelLabelProvider());
		modelTreeViewer.setContentProvider(new ConfigurableModelTreeContentProvider(false));
		modelTreeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		form.setWeights(new int[]{40,60});
		
		Composite buttonComposite = new Composite(container, SWT.NONE);
		buttonComposite.setLayout(new GridLayout(3, false));
		buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false,2,1));
		
		btnNew = new Button(buttonComposite, SWT.PUSH);
		btnNew.setText(Messages.ConfigurableModelPropertyDialog_Button_Create);
		setButtonLayoutData(btnNew);
		btnNew.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				CreateNewOpDialog opDialog = new CreateNewOpDialog(getShell());
				if (opDialog.open() == Window.OK){
					ConfigurableModel initModel  = null;
				
					try{
						initModel = opDialog.getDefaultConfigurableModel();
					}catch (Exception ex){
						SmartPlugIn.displayLog(Messages.ConfigurableModelPropertyDialog_CreateCmModelErrorMessage + ex.getLocalizedMessage(), ex);
						return;
					}
					if (initModel == null){
						//cancelled or invalid model
						return;
					}
					
					Dialog dialog = new ConfigurableModelEditDialog(initModel);
					dialog.open();
					
					//refresh list
					modelListViewer.setInput(getModelsList().toArray());
					updateTreeViewer(true);
				}
			}
		});
		
		btnEdit = new Button(buttonComposite, SWT.PUSH);
		btnEdit.setEnabled(false);
		setButtonLayoutData(btnEdit);
		btnEdit.setText(Messages.ConfigurableModelPropertyDialog_Button_Edit);
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ConfigurableModel cm = (ConfigurableModel) ((IStructuredSelection) modelListViewer.getSelection()).getFirstElement();
				if (cm == null){
					return;
				}
				cm = loadConfigurableModel(cm);
				
				Dialog dialog = new ConfigurableModelEditDialog(cm);
				dialog.open();
				
				modelListViewer.setInput(getModelsList().toArray());
				updateTreeViewer(true);
			}
			
			
		});		
		
		btnDelete = new Button(buttonComposite, SWT.PUSH);
		btnDelete.setEnabled(false);
		setButtonLayoutData(btnDelete);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!(modelTreeViewer.getInput() instanceof ConfigurableModel)){
					return;
				}
				final ConfigurableModel cm = (ConfigurableModel) modelTreeViewer.getInput();
				if (cm == null){
					return;
				}
				if (!MessageDialog.openConfirm(getShell(), Messages.ConfigurableModelPropertyDialog_DeleteDialogTitle, MessageFormat.format(Messages.ConfigurableModelPropertyDialog_ConfirmDelete, new Object[]{cm.getName()}))){
					return;
				}


				ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
				try{
				pmd.run(true, false, new IRunnableWithProgress() {
					
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException,
							InterruptedException {
						monitor.beginTask(Messages.ConfigurableModelPropertyDialog_ProgressDelete, 2);
						Session session = HibernateManager.openSession();
						session.beginTransaction();
						try {
							ConfigurableModel currentCm = (ConfigurableModel) session.get(ConfigurableModel.class, cm.getUuid()); //we need an object that is attached to current session
							monitor.worked(1);
							if (DeleteManager.canDelete(currentCm, session)){
								session.delete(currentCm);
								session.getTransaction().commit();
								//deleting filestore
								DataentryHibernateManager.deleteFilestore(currentCm);
							}
						}catch (Exception ex){
							session.getTransaction().rollback();
							SmartPlugIn.log("Error deleting configurable model", ex); //$NON-NLS-1$
							throw new InvocationTargetException(new Exception(Messages.ConfigurableModelPropertyDialog_ErrorDelete + "\n\n" + ex.getMessage())); //$NON-NLS-1$
						} finally {
							session.close();
							monitor.done();
						}
					}
				});
				}catch (InvocationTargetException ex){
					MessageDialog.openError(getShell(), Messages.ConfigurableModelPropertyDialog_ErrorDialogTitle, ex.getCause().getMessage());
				}catch (Exception ex){
					MessageDialog.openError(getShell(), Messages.ConfigurableModelPropertyDialog_ErrorDialogTitle, ex.getMessage());
				}
				
				modelListViewer.setInput(getModelsList().toArray());
				modelTreeViewer.setInput(null);
				updateTreeViewer(false);
			}
		});		


		Composite exportImportCmp = new Composite(container, SWT.NONE);
		exportImportCmp.setLayout(new GridLayout(2, false));
		exportImportCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false,2,1));
		
		btnExport = new Button(exportImportCmp, SWT.PUSH);
		btnExport.setEnabled(false);
		btnExport.setText(Messages.ConfigurableModelPropertyDialog_Button_Export_File);
		btnExport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				exportXml();
			}
		});

		btnImport = new Button(exportImportCmp, SWT.PUSH);
		btnImport.setText(Messages.ConfigurableModelPropertyDialog_Button_Import_File);
		btnImport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				importXml();
			}
		});
		
		setTitle(Messages.ConfigurableModelPropertyDialog_Title);
		setMessage(Messages.ConfigurableModelPropertyDialog_Message);
		
		return container;
	}
	
	private ConfigurableModel loadConfigurableModel(final ConfigurableModel toLoad){
		final ConfigurableModel[] thiscm = new ConfigurableModel[1];
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask(Messages.ConfigurableModelPropertyDialog_loadingTask, 4);
					Session session = getSession();
					try {
						monitor.subTask(Messages.ConfigurableModelPropertyDialog_modelSubTask);
						thiscm[0] = DataentryHibernateManager.getFullConfigurableModel(toLoad.getUuid(), session);
						monitor.worked(1);
						
						//lazily load remaining data
						monitor.subTask(Messages.ConfigurableModelPropertyDialog_nodesSubTask);
						fetchNodesData(thiscm[0].getNodes());
						monitor.worked(1);
						
						monitor.subTask(Messages.ConfigurableModelPropertyDialog_treesSubTask);
						fetchListData(thiscm[0].getDefaultLists());
						monitor.worked(1);
						
						monitor.subTask(Messages.ConfigurableModelPropertyDialog_ListsSubTask);
						fetchTreeData(thiscm[0].getDefaultTrees());
						monitor.worked(1);

						monitor.subTask(Messages.ConfigurableModelPropertyDialog_AttrConfigSubTask);
						fetchAttributeSettings(thiscm[0].getAttributeSettings());
						monitor.worked(1);
					} finally {
						monitor.done();
						session.close();
					}
				}
			});
		}catch (Exception ex){
			SmartPlugIn.displayLog(Messages.ConfigurableModelPropertyDialog_LoadError + ex.getMessage(), ex);
		}
		return thiscm[0];
	}
	
	
	
	private void fetchNodesData(List<CmNode> nodes) {
		if (nodes == null)
			return;
		
		for (CmNode cmNode : nodes) {
			if (cmNode.getCategory() != null){
				Category c = cmNode.getCategory();
				while (c != null){
					c.getNames().size();
					c = c.getParent();
				}
			}
			for (CmAttribute cmAttribute : cmNode.getCmAttributes()){
				cmAttribute.getNames().size();
				if (cmAttribute.getAttribute() != null){
					cmAttribute.getAttribute().getNames().size();
					if (cmAttribute.getAttribute().getActiveListItems() != null){
						for (AttributeListItem cli : cmAttribute.getAttribute().getActiveListItems()){
							cli.getNames().size();
						}
					}
					fetchAttributeTree(cmAttribute.getAttribute().getActiveTreeNodes());
				}
				fetchListData(cmAttribute.getCurrentList());
				fetchListData(cmAttribute.getDefaultList());
				fetchListData(cmAttribute.getList());
				fetchTreeData(cmAttribute.getCurrentTree());
				fetchTreeData(cmAttribute.getDefaultTree());
				fetchTreeData(cmAttribute.getTree());						
			}
			cmNode.getNames().size();
			cmNode.getCmAttributes().size();
			fetchNodesData(cmNode.getChildren());
			
		}
	}
	private void fetchListData(List<CmAttributeListItem> list){
		if (list == null) return;
		for (CmAttributeListItem c : list){
			c.getNames().size();
			c.getListItem().getNames().size();
		}
	}
	private void fetchTreeData(List<CmAttributeTreeNode> list){
		if (list == null) return;
		for (CmAttributeTreeNode c : list){
			c.getNames().size();
			if (c.getDmTreeNode() != null) c.getDmTreeNode().getNames().size();
			fetchTreeData(c.getChildren());
		}
	}
	private void fetchAttributeTree(List<AttributeTreeNode> tree){
		if (tree == null) return;
		for(AttributeTreeNode n : tree){
			n.getNames().size();
			fetchAttributeTree(n.getActiveChildren());
		}
	}
	private void fetchAttributeSettings(Map<Attribute, CmDmAttributeSettings> settings){
		if (settings == null) return;
		settings.size();
	}

	@Override
	protected boolean performSave() {
		return true;
	}

	private List<ConfigurableModel> getModelsList() {
		List<ConfigurableModel> modelList = new ArrayList<ConfigurableModel>();
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try {
			modelList = DataentryHibernateManager.getConfigurableModels(s);
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.ConfigurableModelPropertyDialog_LoadModelsListError, ex);
		} finally {
			s.getTransaction().rollback();
			s.close();
		}
		return modelList;
	}
	
	private void updateTreeViewer(boolean wasEdited) {
		IStructuredSelection selection = (IStructuredSelection) modelListViewer.getSelection();
		if (!selection.isEmpty()) {
			ConfigurableModel cm = (ConfigurableModel) selection.getFirstElement();
			
			Object currentCm = modelTreeViewer.getInput();
			if (!wasEdited && currentCm instanceof ConfigurableModel && currentCm.equals(cm)){
				//selecting the currently selected cm
				return;
			}
			
			modelTreeViewer.setInput(Messages.ConfigurableModelPropertyDialog_LoadingText);
			loadCmModelJob.cancel();
			loadCmModelJob.modelToLoad = cm;
			loadCmModelJob.schedule();
		}
	}

	private void exportXml(){
		final ConfigurableModel cm = (ConfigurableModel) ((IStructuredSelection) modelListViewer.getSelection()).getFirstElement();
		if (cm == null){
			return;
		}
		
		FileDialog fd = new FileDialog(this.getShell(), SWT.SAVE);
		fd.setFilterNames(new String[]{Messages.ConfigurableModelPropertyDialog_ZipFile});
//		fd.setFilterNames(new String[]{Messages.ConfigurableModelPropertyDialog_XmlFile});
		fd.setFilterExtensions(new String[]{"*.zip"});; //$NON-NLS-1$
		fd.setFileName(URLUtils.cleanFilename(cm.getName()));
		String file = fd.open();
		if (file == null){
			//nothing selected
			return;
		}
		final File f = new File(file);
		if (f.exists()){
			if (!MessageDialog.openQuestion(getShell(), Messages.ConfigurableModelPropertyDialog_Overwrite_Dialog_Title, 
					MessageFormat.format(Messages.ConfigurableModelPropertyDialog_Overwrite_Dialog_Message, new Object[]{ f.getName()}))){
				return;
			}
		}
		
		final ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, true, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					File tmpFolder = null;
					try {
						monitor.beginTask(Messages.ConfigurableModelPropertyDialog_Exporting, 7);
						monitor.subTask(Messages.ConfigurableModelPropertyDialog_Converting);
						org.wcs.smart.dataentry.model.xml.generated.ConfigurableModel xml = CmSmartToXmlConverter.convert(cm, monitor);
						
						monitor.subTask(Messages.ConfigurableModelPropertyDialog_Writing);
						if (xml == null || monitor.isCanceled()) return;
						
						int index = f.getName().lastIndexOf('.');
						String name = f.getName();
						if (index >= 0){
							name= name.substring(0, index);
						}
						tmpFolder = SmartFileUtils.createTempDirectory("smart_cm_export"); //$NON-NLS-1$
						File xmlFile = new File(tmpFolder.getAbsolutePath() + File.separator + name + ".xml"); //$NON-NLS-1$
						
						try(FileOutputStream fout = new FileOutputStream(xmlFile)){
							CmXmlManager.writeDataModel(xml, fout);
						}
						
						monitor.worked(1);
						monitor.subTask(Messages.ConfigurableModelPropertyDialog_Zipping);
						if (monitor.isCanceled()) return;
						
						List<File> toZip = new ArrayList<>();
						toZip.add(xmlFile);
						File dataFolder = new File(cm.getFileDataStoreLocation());
						if (dataFolder != null && dataFolder.exists() && dataFolder.isDirectory()) {
							toZip.addAll(Arrays.asList(dataFolder.listFiles()));
						}
						ZipUtil.createZip(toZip.toArray(new File[toZip.size()]), f, monitor);
						if (monitor.isCanceled()) return;
						
						monitor.done();
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								MessageDialog.openInformation(pmd.getShell(), Messages.ConfigurableModelPropertyDialog_Success_Dialog_Title, Messages.ConfigurableModelPropertyDialog_Success_Dialog_Message);
							}});
						
					}catch (final Exception ex){
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								SmartPlugIn.displayLog(Messages.ConfigurableModelPropertyDialog_ExportError, ex);
							}});
					} finally {
						SmartFileUtils.deleteTempDirectory(tmpFolder);
					}
				}
			});
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.ConfigurableModelPropertyDialog_ExportError, ex);
		}
	}


	private void importXml() {
		FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
		fd.setFilterExtensions(new String[]{"*.zip;*.xml", "*.zip", "*.xml", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		fd.setFilterNames(new String[]{Messages.ConfigurableModelPropertyDialog_SupportedFiles, Messages.ConfigurableModelPropertyDialog_ZipFiles, Messages.ConfigurableModelPropertyDialog_XmlFiles, Messages.ConfigurableModelPropertyDialog_AllFiles});
		
		String file = fd.open();
		if (file != null) {
			final File f = new File(file);
			try {
				ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
				pmd.run(true, true, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						try {
							CmXmlToSmartImporter importer = new CmXmlToSmartImporter();
							final ConfigurableModel cm = f.getName().endsWith(".zip") ? importer.importZip(f, monitor) : importer.importXml(f, monitor); //$NON-NLS-1$
							if (cm != null) {
								Display.getDefault().syncExec(new Runnable() {
									@Override
									public void run() {
										MessageDialog.openInformation(getShell(), Messages.ConfigurableModelPropertyDialog_Success_Title, MessageFormat.format(Messages.ConfigurableModelPropertyDialog_Success_Message, cm.findName(SmartDB.getCurrentLanguage())));
										//refresh list
										modelListViewer.setInput(getModelsList().toArray());
										updateTreeViewer(true);
									}
								});
							}
						} catch (final Exception ex) {
							Display.getDefault().syncExec(new Runnable() {
								@Override
								public void run() {
									SmartPlugIn.displayLog(Messages.ConfigurableModelPropertyDialog_Import_Error + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
								}
							});
						}
					}

				});
			} catch (Exception ex) {
				SmartPlugIn.displayLog(Messages.ConfigurableModelPropertyDialog_Import_Error + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
			}
		}				
	}
	
	class LoadCmModelJob extends Job{
		public ConfigurableModel modelToLoad;
		
		public LoadCmModelJob() {
			super(Messages.ConfigurableModelPropertyDialog_LoadJobName);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			ConfigurableModel cm = modelToLoad;
			cm = DataentryHibernateManager.getFullConfigurableModel(cm.getUuid());
			final ConfigurableModel lcm = cm;
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					if (!modelTreeViewer.getControl().isDisposed()){
						modelTreeViewer.setInput(lcm);
					}
				}});
			return Status.OK_STATUS;
		}
		
	}
}
