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
package org.wcs.smart.cybertracker.importer;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.ICyberTrackerConstants;
import org.wcs.smart.cybertracker.model.ICyberTrackerData;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Main composite of Cyber Tracker import editor
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class CyberTrackerImportComposite extends Composite {

	private static final int HEIGHT_HINT = 300;
	
	/**
	 * Default displayed columns
	 * 
	 * @author elitvin
	 * @since 4.0.0
	 */
	public enum CTTableColumn {
		IMPORT_NOTE ("",	20), //$NON-NLS-1$
		START_DATE	("Start Date and Time",	120),
		END_DATE	("End Date and Time",		120),
		TYPE		("Type", 		60),
		SIGHT_COUNT	("Observations Count", 	50),
		DETAILS		("Details", 	300);
		
		private String guiName;
		private int width;
		CTTableColumn(String guiName, int width) {
			this.guiName = guiName;
			this.width = width;
		}
		public String getGuiName() {
			return this.guiName;
		}
		public int getWidth() {
			return width;
		}
	}

	private TableViewer viewer;
	private Button btnAdd;
	private Button btnRemove;
	
	private Composite detailsInnerPanel;
	
	private CyberTrackerImporter importer = new CyberTrackerImporter();
	
	private List<ICyberTrackerData> tableInputData = new ArrayList<ICyberTrackerData>();
	
	private Map<String, IImportEditorContent> contentMap = new HashMap<String, IImportEditorContent>();
	private Map<String, Composite> compositeMap = new HashMap<String, Composite>();
	private Composite emptyComposite;

	/**
	 * @param parent
	 * @param style
	 */
	public CyberTrackerImportComposite(Composite parent, int style, FormToolkit toolkit) {
		super(parent, style);
		buildContentMap();
		createControls(toolkit);
	}
	
	private void buildContentMap() {
		if (Platform.getExtensionRegistry() != null) {
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(CyberTrackerPlugIn.EXTENSION_ID);
			try {
				for (IConfigurationElement e : config) {
					String name = e.getAttribute("name"); //$NON-NLS-1$
					IImportEditorContent content = (IImportEditorContent) e.createExecutableExtension("importEditorContent"); //$NON-NLS-1$
					contentMap.put(name, content);
				}
			}catch (Exception ex){
				SmartPlugIn.displayLog("Error getting CyberTracker extensions", ex);
			}
		}
	}

	private void createControls(FormToolkit toolkit) {
		toolkit.adapt(this);
		
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = 0;
		this.setLayout(layout);
		
		Section data = toolkit.createSection(this, Section.TITLE_BAR | Section.EXPANDED);
		layout = new GridLayout();
		data.setLayout(layout);
		data.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		data.setText(Messages.CTPatrolTableContainer_DataSectionHeader);
		
		Composite dataComp = toolkit.createComposite(data);
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = 0;
		dataComp.setLayout(layout);
		dataComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		data.setClient(dataComp);
		
		/* Import Data */		
		Composite buttons = toolkit.createComposite(dataComp);
		buttons.setLayout(new GridLayout(2, true));
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		
		Button btnImport = toolkit.createButton(buttons, Messages.CyberTrackerImportDialog_Button_Import, SWT.PUSH);
		btnImport.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnImport.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performImport(false);
			}
		});

		Button btnImportPda = toolkit.createButton(buttons, Messages.CyberTrackerImportDialog_Button_ImportFromDevice, SWT.PUSH);
		btnImportPda.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnImportPda.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performImport(true);
			}
		});
		
		viewer = new TableViewer(dataComp, SWT.BORDER | SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.MULTI);
		toolkit.paintBordersFor(viewer.getTable());
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(true);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = HEIGHT_HINT;
		viewer.getTable().setLayoutData(gd);
		ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.NO_RECREATE);
		
		viewer.setItemCount(0);
		addColumns(viewer);
		viewer.setInput(tableInputData);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean selected = !viewer.getSelection().isEmpty();
				btnAdd.setEnabled(selected);
				btnRemove.setEnabled(selected);
				if (selected){
					updatePatrolDetails(((IStructuredSelection)viewer.getSelection()).getFirstElement());
				}else{
					updatePatrolDetails(null);
				}
			}
		});
		final MenuManager mgr = new MenuManager();
		Action addAction = new Action("Add"){
			@Override
			public void run() {
				handleAdd();
			}
		};
		Action deleteAction = new Action(DialogConstants.DELETE_BUTTON_TEXT){
			@Override
			public void run() {
				handleRemove();
			}
		};
		mgr.add(addAction);
		mgr.add(deleteAction);
		mgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				updateContextMenu(viewer.getControl().getMenu(), !viewer.getSelection().isEmpty());
			}
		});
		viewer.getControl().setMenu(mgr.createContextMenu(viewer.getControl()));
		mgr.updateAll(true);
		
		
		/* Import Button Panel */
		buttons = new Composite(dataComp, SWT.NONE);
		buttons.setLayout(new GridLayout(3, true));
		buttons.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnAdd = toolkit.createButton(buttons,"Add", SWT.PUSH);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnAdd.setEnabled(false);

		btnRemove = toolkit.createButton(buttons,DialogConstants.DELETE_BUTTON_TEXT,SWT.PUSH);
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnRemove.setEnabled(false);
		
		/* Patrol Details Section */
		final Section details = toolkit.createSection(this, Section.TITLE_BAR | Section.COMPACT | Section.TWISTIE);
		details.setLayout(new GridLayout());
		details.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		details.setText(Messages.CTPatrolTableContainer_DetailsSectionHeader);
		details.addExpansionListener(new ExpansionAdapter() {
			@Override
			public void expansionStateChanged(ExpansionEvent e) {
				if (details.isExpanded()){
					details.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));			
				}else{
					details.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				}
				details.getParent().layout(true, true);
			}
		});
		details.setClient(createDetailsComposite(details, toolkit));
	}

	private void updateContextMenu(Menu menu, boolean enabled) {
		if (menu != null) {
			for (MenuItem item : menu.getItems()) {
				item.setEnabled(enabled);
			}
		}
	}
	
	private void performImport(final boolean fromPda) {
		File[] dialogFiles = null;
		if (!fromPda) {
			dialogFiles = selectFile();
			if (dialogFiles == null) return;
		}
		
		final File[] files = dialogFiles;
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask(Messages.CyberTrackerImportDialog_Task_RawImport, 1);
					List<ICyberTrackerData> data;
					int code = 0;
					try {
						if (fromPda) {
							CyberTrackerImportResult result = importer.importPdaData(monitor);
							data = result.getData();
							code = result.getReturnCode();
						}else{
							monitor.beginTask(Messages.CyberTrackerImportDialog_Task_RawImport, files.length);
							data = new ArrayList<ICyberTrackerData>();
							for (int i = 0; i < files.length; i ++){
								File currentFile = files[i];
								if (!currentFile.exists()){
									final File fcurrentFile = currentFile;
									Display.getDefault().syncExec(new Runnable(){
										@Override
										public void run() {
											MessageDialog.openError(getShell(), Messages.CyberTrackerImportDialog_Error_Title,MessageFormat.format( Messages.CyberTrackerImportDialog_Error_Message, new Object[]{fcurrentFile.toString()}));
										}});
														
								}else{
									data.addAll(importer.importFileData(files[i], new SubProgressMonitor(monitor,1)));
								}
								monitor.worked(1);
							}
						}
						addTableData(data);
					} catch (Exception e) {
						CyberTrackerPlugIn.displayError(Messages.CTPatrolTableContainer_Error_Title, MessageFormat.format(Messages.CTPatrolTableContainer_ImportError_Message, e.getMessage()), e);
						return;
					}
					int count = data != null ? data.size() : 0;
					if (count > 0) {
						CyberTrackerPlugIn.displayInfo(Messages.CTPatrolTableContainer_InfoDialog_Title, MessageFormat.format(Messages.CTPatrolTableContainer_ImportCompleted, count));
					} else {
						CyberTrackerPlugIn.displayInfo(Messages.CTPatrolTableContainer_InfoDialog_Title, getEmptyDownloadMessage(code));
					}
				}

			});
		} catch (Exception e) {
			CyberTrackerPlugIn.displayError(Messages.CTPatrolTableContainer_Error_Title, Messages.CTPatrolTableContainer_Error_ImportAborted, e);
		}

	}

	private String getEmptyDownloadMessage(int code) {
		switch (code) {
		case ICyberTrackerConstants.DOWNLOAD_CODE_NO_CONNECTION:
			return Messages.CTPatrolTableContainer_DownloadCode_300;
		case ICyberTrackerConstants.DOWNLOAD_CODE_NO_DATA:
			return Messages.CTPatrolTableContainer_DownloadCode_301;
		case ICyberTrackerConstants.DOWNLOAD_CODE_SUCCESS:
			return Messages.CTPatrolTableContainer_DownloadCode_302;
		default:
			return Messages.CTPatrolTableContainer_NoDataImported;
		}
	}
	

	private void handleRemove() {
		boolean isOk = MessageDialog.openQuestion(getShell(), Messages.CTPatrolTableContainer_DeleteWarn_Title, Messages.CTPatrolTableContainer_DeleteWarn_Message);
		if (isOk) {
			IStructuredSelection sel = (IStructuredSelection)viewer.getSelection();
			if (!sel.isEmpty()) {
				for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
					tableInputData.remove(iterator.next());
				}
			}
			viewer.refresh();
		}
	}
	
	private void addColumns(TableViewer viewer) {
		for (CTTableColumn column : CTTableColumn.values()) {
			TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
			viewerColumn.getColumn().setText(column.getGuiName());
			viewerColumn.getColumn().setWidth(column.getWidth());
			viewerColumn.setLabelProvider(new CyberTrackerImportTableCellLabelProvider(column));
		}
	}

	private File[] selectFile() {
		CyberTrackerFileImportDialog fileDialog = new CyberTrackerFileImportDialog(getShell());
		if (fileDialog.open() == IDialogConstants.OK_ID)
			return fileDialog.getSelectedFiles();
		return null;
	}
	
	public TableViewer getViewer() {
		return viewer;
	}

	public void addTableData(List<ICyberTrackerData> data) {
		tableInputData.addAll(data);
		refreshViewer();
	}
	
	private void refreshViewer() {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				viewer.refresh();
			}
		});
	}
	
	private IImportEditorContent getEditorContent(String datatype) {
		return contentMap.get(datatype);
	}
	
	private Composite createDetailsComposite(Composite parent, FormToolkit toolkit){
		ScrolledComposite scrolled = new ScrolledComposite(parent,  SWT.V_SCROLL | SWT.H_SCROLL);
		scrolled.setLayout(new GridLayout(1, false));
		scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		toolkit.adapt(scrolled);
		scrolled.setExpandHorizontal(true);
		
		detailsInnerPanel = toolkit.createComposite(scrolled);
		detailsInnerPanel.setLayout(new StackLayout());
		detailsInnerPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		emptyComposite = toolkit.createComposite(detailsInnerPanel);
		emptyComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		for (String datatype : contentMap.keySet()) {
			IImportEditorContent content = contentMap.get(datatype);
			Composite cmp = content.createDetailsComposite(detailsInnerPanel, toolkit);
			compositeMap.put(datatype, cmp);
		}
		
		scrolled.setContent(detailsInnerPanel);
		detailsInnerPanel.setSize(detailsInnerPanel.computeSize(SWT.DEFAULT, SWT.DEFAULT).x, 300);
		return scrolled;
	}

	private void updatePatrolDetails(Object selection) {
		
		StackLayout stackLayout = ((StackLayout)detailsInnerPanel.getLayout());
		if (selection != null) {
			ICyberTrackerData data = (ICyberTrackerData) selection;
			String datatype = data.getType();
			getEditorContent(datatype).inputChanged(selection);
			stackLayout.topControl = compositeMap.get(datatype);
		} else {
			stackLayout.topControl = emptyComposite;
		}
		detailsInnerPanel.layout();
	}
	
	protected void handleAdd() {
		IStructuredSelection selection = (IStructuredSelection)viewer.getSelection();
		if (!selection.isEmpty()) {
			String datatype = ((ICyberTrackerData)selection.getFirstElement()).getType();
			boolean isSame = true;
			for (Iterator<?> i = selection.iterator(); i.hasNext();) {
				ICyberTrackerData data = (ICyberTrackerData) i.next();
				if (!datatype.equals(data.getType())) { //datatype should never be null even for old CTX file, importer/dataBuilder should handle this
					isSame = false;
					break;
				}
			}
			if (isSame) {
				getEditorContent(datatype).handleAdd(getShell(), selection);
			} else {
				MessageDialog.openError(getShell(), "Error", "Unable to import selected data. Please select data of the same type for importing.");
			}
		}
		refreshViewer();
	}

}
