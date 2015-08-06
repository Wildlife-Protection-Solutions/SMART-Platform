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
package org.wcs.smart.intelligence.informant.editor;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.IntelligenceHibernateManager;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.informant.PersistentManager;
import org.wcs.smart.intelligence.informant.aes.EncryptedData;
import org.wcs.smart.intelligence.informant.aes.InformantAesManager;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Informant;
import org.wcs.smart.intelligence.model.InformantDataKey;
import org.wcs.smart.intelligence.model.IntelligenceSource;

/**
 * Editor for viewing informant application data.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class InformantDataEditor extends EditorPart implements ISaveablePart2 {
	
	public static final String ID = "org.wcs.smart.intelligence.informant.InformantDataEditor"; //$NON-NLS-1$

	private static final String LOGINTOOLTIP = Messages.InformantDataEditor_logintooltip;
	/**
	 * Secured informant columns
	 * 
	 * @author elitvin
	 * @since 3.2.0
	 */
	public enum InfromantColumn {
		COL_FIRST_NAME (Messages.InfromantColumn_FirstName, InformantDataKey.FIRST_NAME),
		COL_LAST_NAME (Messages.InfromantColumn_LastName, InformantDataKey.LAST_NAME),
		COL_START_DATE (Messages.InfromantColumn_StartDate, InformantDataKey.START_DATE),
		COL_PHONE (Messages.InfromantColumn_Phone, InformantDataKey.PHONE),
		COL_DESCRIPTION (Messages.InfromantColumn_Description, InformantDataKey.DESCRIPTION),
		COL_ROLE (Messages.InfromantColumn_Role, InformantDataKey.ROLE),
		COL_ADDRESS (Messages.InfromantColumn_Address, InformantDataKey.ADDRESS),
		COL_CITY (Messages.InfromantColumn_City, InformantDataKey.CITY),
		COL_COUNTRY (Messages.InfromantColumn_Country, InformantDataKey.COUNTRY),
		COL_NOTES (Messages.InfromantColumn_Notes, InformantDataKey.NOTES);
		
		private static final int DEFAULT_COLUMN_WIDTH = 80;
		
		private String guiName;
		private InformantDataKey key;
		private int width;
		InfromantColumn(String guiName, InformantDataKey key) {
			this(guiName, key, DEFAULT_COLUMN_WIDTH);
		}
		InfromantColumn(String guiName, InformantDataKey key, int width) {
			this.guiName = guiName;
			this.key = key;
			this.width = width;
		}
		public String getGuiName() {
			return this.guiName;
		}
		public int getWidth() {
			return width;
		}
		public InformantDataKey getKey() {
			return key;
		}
	}

	private FormToolkit toolkit = new FormToolkit(Display.getCurrent());

	private TableViewer viewer;
	private InformantSorter informantSorter;
	
	private Composite upperCmp;
	private Composite linkComp;
	private Hyperlink btnLogin;
	private Button btnEdit;
	private Button btnAdd;
	private Button btnDelete;
	private Hyperlink btnChangePwd;
	private Button btnShow;
	
	private Map<TableColumn, Integer> securedColumnsMap = new HashMap<TableColumn, Integer>();
	
	private final Comparator<Informant> idComparator = new Comparator<Informant>() {
		@Override
		public int compare(Informant i1, Informant i2) {
			return i1.getId().compareTo(i2.getId());
		}
	};
	
	public InformantDataEditor() {
	}	

	@Override
	public void createPartControl(Composite parent) {
		Form form = toolkit.createForm(parent);
		form.setText(Messages.InformantDataEditor_Title);
		GridLayout layout = new GridLayout();
		form.getBody().setLayout(layout);
		
		Composite main = toolkit.createComposite(form.getBody());
		layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		/* login/logout security */
		linkComp = toolkit.createComposite(main);
		linkComp.setLayout(new GridLayout(3, false));
		((GridLayout)linkComp.getLayout()).marginTop = 0;
		((GridLayout)linkComp.getLayout()).marginBottom = 5;
		((GridLayout)linkComp.getLayout()).marginWidth = 0;
		linkComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnLogin = toolkit.createHyperlink(linkComp, Messages.InformantDataEditor_Button_SetPassword, SWT.PUSH);
		btnLogin.addHyperlinkListener(new HyperlinkAdapter(){
			public void linkActivated(HyperlinkEvent e) {
				performLoginOrLogout();
			}
		});
		btnLogin.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		btnLogin.setToolTipText(LOGINTOOLTIP);

		btnChangePwd = toolkit.createHyperlink(linkComp, Messages.InformantDataEditor_ChangePassword, SWT.PUSH);
		btnChangePwd.setToolTipText(Messages.InformantDataEditor_passtooltip);
		btnChangePwd.addHyperlinkListener(new HyperlinkAdapter(){
			public void linkActivated(HyperlinkEvent e) {
				performChangePassword();
			}
		});
		btnChangePwd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		btnShow = toolkit.createButton(linkComp, Messages.InformantDataEditor_ShowCoulumns, SWT.CHECK);
		btnShow.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnShow.setSelection(true);
		btnShow.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showSecuredColumns(btnShow.getSelection());
			}
		});
		btnShow.setToolTipText(Messages.InformantDataEditor_columnstooltip);
		
		
		/* add edit delete */
		upperCmp = toolkit.createComposite(main);
		layout = new GridLayout(1, false);
		layout.marginWidth = layout.marginHeight = 0;
		upperCmp.setLayout(layout);
		upperCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Composite btnComp = toolkit.createComposite(upperCmp);
		btnComp.setLayout(new GridLayout(3, true));
		((GridLayout)btnComp.getLayout()).marginWidth = 0;
		((GridLayout)btnComp.getLayout()).marginHeight = 0;
		btnComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		int sizeHint = 80;
		btnAdd = toolkit.createButton(btnComp, Messages.InformantDataEditor_Button_Add, SWT.PUSH);
		btnAdd.setToolTipText(Messages.InformantDataEditor_newtooltip);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performAdd();
			}
		});
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnAdd.getLayoutData()).widthHint = sizeHint;
		
		btnEdit = toolkit.createButton(btnComp, Messages.InformantDataEditor_Button_Edit, SWT.PUSH);
		btnEdit.setToolTipText(Messages.InformantDataEditor_edittooltip);
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performEdit();
			}
		});
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnEdit.getLayoutData()).widthHint = sizeHint;
		
		btnDelete = toolkit.createButton(btnComp, Messages.InformantDataEditor_Button_Delete, SWT.PUSH);
		btnDelete.setToolTipText(Messages.InformantDataEditor_deletetooltip);
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performDelete();
			}
		});
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnDelete.getLayoutData()).widthHint = sizeHint;
		
		
		
		viewer = new TableViewer(main, SWT.BORDER | SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.MULTI);
		toolkit.paintBordersFor(viewer.getTable());
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(true);
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 300;
		viewer.getTable().setLayoutData(gd);

		informantSorter = new InformantSorter(viewer);
		viewer.setComparator(informantSorter);
		viewer.setItemCount(0);
		addColumns(viewer);
		
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
		viewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				performEdit();
			}
		});
		
		toolkit.createLabel(main, MessageFormat.format(Messages.InformantDataEditor_Note, IntelligenceSource.INFORMANT_KEY));
		
		loadData();
		informantSorter.setSortColumn(viewer.getTable().getColumn(0));
		updateButtons();
		
		viewer.getTable().pack(true);
		parent.layout();
	}

	protected void showSecuredColumns(boolean isVisible) {
		for (TableColumn column : securedColumnsMap.keySet()) {
			if (isVisible) {
				if (column.getWidth() == 0) {
					column.setWidth(securedColumnsMap.get(column));
					column.setResizable(true);
				}
			} else {
				if (column.getWidth() > 0) {
					securedColumnsMap.put(column, column.getWidth());
					column.setWidth(0);
					column.setResizable(false);
				}				
			}
		}
		viewer.refresh();
	}

	private void updateButtons() {
		ISelection selection = viewer.getSelection();
		boolean hasDecrypted = InformantAesManager.getInstance().containsDecrypted();
		boolean isPasswordSet = InformantAesManager.getInstance().isPasswordSet();
		boolean isEmptyInput = isEmptySecureInput();
//		btnDelete.setEnabled(isDecrypted && selection != null && !selection.isEmpty());
//		btnEdit.setEnabled(isDecrypted && selection != null && !selection.isEmpty());
//		btnAdd.setEnabled(isDecrypted || (!isDecrypted && isPasswordSet && isEmptyInput));
		btnEdit.setEnabled(selection != null && !selection.isEmpty());
		btnDelete.setEnabled(selection != null && !selection.isEmpty());
		btnLogin.setToolTipText(null);
		if (isEmptyInput) {
			btnLogin.setText(isPasswordSet ? Messages.InformantDataEditor_Button_Logout : Messages.InformantDataEditor_Button_SetPassword);
			btnShow.setVisible(isPasswordSet);
			btnChangePwd.setVisible(isPasswordSet);
		} else {
			btnLogin.setText(hasDecrypted ? Messages.InformantDataEditor_Button_Logout : Messages.InformantDataEditor_Button_Login);
			if (!hasDecrypted){
				btnLogin.setToolTipText(LOGINTOOLTIP);
			}
			btnShow.setVisible(hasDecrypted);
			btnChangePwd.setVisible(hasDecrypted);
		}
		showSecuredColumns(btnShow.getVisible() && btnShow.getSelection());
		upperCmp.layout();
		linkComp.layout();
	}

	private boolean isEmptySecureInput() {
		Object input = viewer.getInput();
		if (input instanceof List) {
			@SuppressWarnings("unchecked")
			List<Informant> list = (List<Informant>) input;
			for (Informant i : list) {
				if (i.getEncryptedData() != null && !i.getEncryptedData().isEmpty()) {
					return false;
				}
			}
		}
		return true;
	}
	
	private void loadData() {
		List<Informant> informantList = null;
		Session s = HibernateManager.openSession();
		try {
			informantList = IntelligenceHibernateManager.getInformants(SmartDB.getCurrentConservationArea(), s, false);
		} catch (Exception e) {
			IntelligencePlugIn.displayLog(Messages.IntelligenceSourceComposite_InformantLoad_Error, e);
			informantList = new ArrayList<Informant>();
		} finally {
			s.close();
		}
		viewer.setInput(informantList);
	}
	
	protected void performLoginOrLogout() {
		boolean isEmptyInput = isEmptySecureInput();
		if (isEmptyInput) {
			if (InformantAesManager.getInstance().isPasswordSet()) {
				performLogout();
			} else {
				performSetPassword();
			}
		} else {
			if (InformantAesManager.getInstance().containsDecrypted()) {
				performLogout();
			} else {
				performLogin(0);
			}
		}
	}
	
	private void performSetPassword() {
		SetPasswordDialog dialog = new SetPasswordDialog(getSite().getShell());
		if (dialog.open() == Window.OK) {
			InformantAesManager.getInstance().setPassword(dialog.getPassword());
			viewer.refresh();
			updateButtons();
		}
	}

	private void performLogin(int attempts) {
		PasswordInputDialog dialog = new PasswordInputDialog(getSite().getShell());
		if (dialog.open() == Window.OK) {
			if (dialog.getPassword() == null){
				loadData();
				updateButtons();
			}else{
				InformantAesManager manager = InformantAesManager.getInstance();
				manager.setPassword(dialog.getPassword());
				
				//attempt to decode the informant; this will ensure
				//the password verification works as expected
				if (viewer.getInput() instanceof List<?>){
					List<?> data = (List<?>) viewer.getInput();
					for (Object object : data) {
						if (object instanceof Informant) {
							if (InformantEditor.getInformant((Informant)object) != null) {
								break; //we found and successfully decoded at least one informant
							}
						}
					}
				}
				if (manager.isPasswordSet() && !manager.containsDecrypted()) {
					MessageDialog.openError(getSite().getShell(), Messages.InformantDataEditor_WrongPassword_Title, Messages.InformantDataEditor_WrongPassword_Message);
					if (attempts < 10){
						//try to login again
						performLogin(attempts + 1);
					}
					return;
				}
				updateButtons();
			}
			firePropertyChange(IWorkbenchPartConstants.PROP_DIRTY);
		}
	}
	
	private void performLogout() {
		InformantAesManager.getInstance().clear();
		viewer.refresh();
		updateButtons();
	}

	protected void performEdit() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		if (selection != null && !selection.isEmpty()) {
			Informant i = (Informant) selection.getFirstElement();
			InformantEditor dialog = new InformantEditor(getSite().getShell(), i);
			dialog.open();
			viewer.refresh();
		}
	}
	
	protected void performDelete() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		if (selection != null && !selection.isEmpty()) {
			if (MessageDialog.openQuestion(getSite().getShell(), Messages.InformantDataEditor_DeleteDialog_Title, Messages.InformantDataEditor_DeleteDialog_Message)) {
				int size = selection.size();
				int count = 0;
				for (Iterator<?> i = selection.iterator(); i.hasNext();) {
					Informant informant = (Informant)  i.next();
					if (IntelligenceHibernateManager.deleteInformant(informant)) {
						count++;
					}
				}
				loadData();
				MessageDialog.openInformation(getSite().getShell(), Messages.InformantDataEditor_DeleteInformant, MessageFormat.format(Messages.InformantDataEditor_SuccessDelete_Message, count, size));
			}
		}
	}

	protected void performAdd() {
		Informant i = new Informant();
		i.setId("informant"); //$NON-NLS-1$
		i.setIsActive(true);
		i.setConservationArea(SmartDB.getCurrentConservationArea());
		NewInformantEditor dialog = new NewInformantEditor(getSite().getShell(), i);
		dialog.open();
		loadData();
	}

	protected void performChangePassword() {
		final ChangePasswordDialog dialog = new ChangePasswordDialog(getSite().getShell());
		if (dialog.open() == Window.OK) {
			ProgressMonitorDialog pmd = new ProgressMonitorDialog(getSite().getShell());
			@SuppressWarnings("unchecked")
			final List<Informant> informantList = (List<Informant>) viewer.getInput();
			try {
				pmd.run(true, false, new IRunnableWithProgress() {
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						InformantAesManager manager = InformantAesManager.getInstance();
						monitor.beginTask(Messages.InformantDataEditor_Task_ChangePassword, 2*informantList.size());
						Map<Informant, Map<InformantDataKey, Object>> i2data = new HashMap<>();
						monitor.subTask(Messages.InformantDataEditor_Task_ExtractInformantData);
						for (Informant informant : informantList) {
							Map<InformantDataKey, Object> info = InformantEditor.getInformant(informant);
							i2data.put(informant, info);
						}
						
						manager.setPassword(dialog.getPassword());
						for (Informant informant : informantList) {
							monitor.subTask(MessageFormat.format(Messages.InformantDataEditor_Task_EncryptInformantData, informant.getId()));
							Map<InformantDataKey, Object> info = i2data.get(informant);
							if (info != null) {
								InformantEditor.setInformant(informant, info);
								EncryptedData encryptedData = informant.getEncryptedData();
					    		File dataFile = informant.getDataFile();
								if (!encryptedData.isEmpty()) {
					    			PersistentManager.toFile(dataFile, encryptedData);
					    		} else if (dataFile.exists()) {
					    			try {
										FileUtils.forceDelete(dataFile);
									} catch (IOException e) {
										IntelligencePlugIn.log("Cannot delete file", e); //$NON-NLS-1$
									}
					    		}
							}
						}
					}
				});
				MessageDialog.openInformation(getSite().getShell(), Messages.InformantDataEditor_SuccessDialog_Title, Messages.InformantDataEditor_SuccessDialog_Message);
			} catch (Exception e) {
				IntelligencePlugIn.displayLog(Messages.InformantDataEditor_ChangePassword_Error, e);
			}
		}
	}
	
	private void addColumns(TableViewer v) {
		//public data
		final TableViewerColumn idColumn = new TableViewerColumn(v, SWT.NONE);
		idColumn.getColumn().setText(Messages.InfromantColumn_ID);
		idColumn.getColumn().setWidth(80);
		idColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Informant) {
					return ((Informant)element).getId();
				}
				return super.getText(element);
			}
		});
		informantSorter.set(idColumn.getColumn(), idComparator);
		idColumn.getColumn().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				informantSorter.setSortColumn(idColumn.getColumn());
			}	
		});


		TableViewerColumn activeColumn = new TableViewerColumn(v, SWT.NONE);
		activeColumn.getColumn().setText(Messages.InfromantColumn_Active);
		activeColumn.getColumn().setWidth(50);
		activeColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof Informant) {
					return ((Informant)element).getIsActive() ? Messages.InfromantColumn_Active_Yes : Messages.InfromantColumn_Active_No;
				}
				return super.getText(element);
			}
		});
		
		//secure data
		for (InfromantColumn column : InfromantColumn.values()) {
			TableViewerColumn viewerColumn = new TableViewerColumn(v, SWT.NONE);
			viewerColumn.getColumn().setText(column.getGuiName());
			viewerColumn.getColumn().setWidth(column.getWidth());
			viewerColumn.setLabelProvider(new InformantColumnLabelProvider(column.getKey()));
			securedColumnsMap.put(viewerColumn.getColumn(), column.getWidth());
		}
	}

	public void setSelection(Informant i) {
		if (viewer != null && !viewer.getTable().isDisposed()) {
			viewer.setSelection(new StructuredSelection(i));
		}
	}

	@Override
	public void setFocus() {
		viewer.getTable().setFocus();
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public void dispose(){
		super.dispose();
		if (toolkit != null){
			toolkit.dispose();
		}
	}
	
	@Override
	public int promptToSaveOnClose() {
		InformantAesManager manager = InformantAesManager.getInstance();
		if (manager.isPasswordSet()) {
			if (manager.containsDecrypted()) {
				//prompt that user will be logged out
				if (!MessageDialog.openQuestion(getSite().getShell(), Messages.InformantDataEditor_LogoutDialog_Title, Messages.InformantDataEditor_LogoutDialog_Message)) {
					return ISaveablePart2.CANCEL;
				}
			}
			manager.clear();
		}
		return ISaveablePart2.NO;
	}
	
	@Override
	public boolean isDirty() {
		InformantAesManager manager = InformantAesManager.getInstance();
		return manager.isPasswordSet() && manager.containsDecrypted();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing
	}

	@Override
	public void doSaveAs() {
		//not allowed
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

}
