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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
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
import org.wcs.smart.intelligence.informant.aes.InformantAesManager;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Informant;
import org.wcs.smart.intelligence.model.InformantDataKey;
import org.wcs.smart.ui.UserNamePasswordDialog;

/**
 * Editor for viewing informant application data.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class InformantDataEditor extends EditorPart implements ISaveablePart2 {
	
	public static final String ID = "org.wcs.smart.intelligence.informant.InformantDataEditor"; //$NON-NLS-1$

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
	private Button btnLogin;
	private Button btnEdit;
	private Button btnAdd;
	private Button btnDelete;
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

		upperCmp = toolkit.createComposite(main);
		layout = new GridLayout(6, false);
		//layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = 0;
		upperCmp.setLayout(layout);
		upperCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnLogin = toolkit.createButton(upperCmp, Messages.InformantDataEditor_Button_SetPassword, SWT.PUSH);
		btnLogin.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performLoginOrLogout();
			}
		});

		btnAdd = toolkit.createButton(upperCmp, Messages.InformantDataEditor_Button_Add, SWT.PUSH);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performAdd();
			}
		});
		
		btnEdit = toolkit.createButton(upperCmp, Messages.InformantDataEditor_Button_Edit, SWT.PUSH);
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performEdit();
			}
		});

		btnDelete = toolkit.createButton(upperCmp, Messages.InformantDataEditor_Button_Delete, SWT.PUSH);
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performDelete();
			}
		});

		btnShow = toolkit.createButton(upperCmp, Messages.InformantDataEditor_ShowCoulumns, SWT.CHECK);
		btnShow.setLayoutData(new GridData(SWT.LEFT, SWT.BOTTOM, true, false));
		btnShow.setSelection(true);
		btnShow.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showSecuredColumns(btnShow.getSelection());
			}
		});
		
		Hyperlink lnkForgotPwd = toolkit.createHyperlink(upperCmp, Messages.InformantDataEditor_ForgotPassword, SWT.WRAP);
		lnkForgotPwd.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, true, false));
		lnkForgotPwd.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				handleForgotPassword();
			}
		});

		
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
		
		toolkit.createLabel(main, Messages.InformantDataEditor_Note);
		
		loadData();
		informantSorter.setSortColumn(viewer.getTable().getColumn(0));
		updateButtons();
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
					column.setResizable(true);
				}				
			}
		}
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
		if (isEmptyInput) {
			btnLogin.setText(isPasswordSet ? Messages.InformantDataEditor_Button_Logout : Messages.InformantDataEditor_Button_SetPassword);
			btnShow.setVisible(isPasswordSet);
		} else {
			btnLogin.setText(hasDecrypted ? Messages.InformantDataEditor_Button_Logout : Messages.InformantDataEditor_Button_Login);
			btnShow.setVisible(hasDecrypted);
		}
		showSecuredColumns(btnShow.getVisible() && btnShow.getSelection());
		upperCmp.layout();
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

	protected void handleForgotPassword() {
		if (MessageDialog.openQuestion(getSite().getShell(), Messages.InformantDataEditor_ResetDialog_Title, Messages.InformantDataEditor_ResetDialog_Message)) {
			//user need to enter login/password to remove secure data
			UserNamePasswordDialog dialog = new UserNamePasswordDialog(Display.getCurrent().getActiveShell(),
					Messages.InformantDataEditor_UserNameConfirmation_Title,
					Messages.InformantDataEditor_UserNameConfirmation_Message,
					Messages.InformantDataEditor_UserNameConfirmation_Button);
			if (dialog.open() == Window.CANCEL) {
				return;
			}
			
			if (!(dialog.getUserName().equalsIgnoreCase(SmartDB.getCurrentEmployee().getSmartUserId())
				&& dialog.getPassword().equals(SmartDB.getCurrentEmployee().getSmartPassword())	)) {
				
				MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.InformantDataEditor_ConfirmationError_Title, Messages.InformantDataEditor_ConfirmationError_Message);
				return;
			}
			
			IntelligenceHibernateManager.clearInformantsEncrptedData();
			loadData();
			updateButtons();
		}
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
				performLogin();
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

	private void performLogin() {
		PasswordInputDialog dialog = new PasswordInputDialog(getSite().getShell());
		if (dialog.open() == Window.OK) {
			InformantAesManager manager = InformantAesManager.getInstance();
			manager.setPassword(dialog.getPassword());
			viewer.refresh();
			if (manager.isPasswordSet() && !manager.containsDecrypted()) {
				MessageDialog.openError(getSite().getShell(), Messages.InformantDataEditor_WrongPassword_Title, Messages.InformantDataEditor_WrongPassword_Message);
			}
			updateButtons();
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
