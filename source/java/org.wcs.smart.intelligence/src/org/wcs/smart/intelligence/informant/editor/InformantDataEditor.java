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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
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

/**
 * Editor for viewing informant application data.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class InformantDataEditor extends EditorPart {
	
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
	

	public static final String ID = "org.wcs.smart.intelligence.informant.InformantDataEditor"; //$NON-NLS-1$

	private FormToolkit toolkit = new FormToolkit(Display.getCurrent());

	private TableViewer viewer;
    private List<Informant> informantList;
	
	public InformantDataEditor() {
		Session s = HibernateManager.openSession();
		try {
			informantList = IntelligenceHibernateManager.getInformants(SmartDB.getCurrentConservationArea(), s, false);
		} catch (Exception e) {
			IntelligencePlugIn.displayLog(Messages.IntelligenceSourceComposite_InformantLoad_Error, e);
			informantList = new ArrayList<Informant>();
		} finally {
			s.close();
		}
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

		Composite upperCmp = toolkit.createComposite(main);
		layout = new GridLayout(3, false);
		//layout.marginHeight = layout.marginWidth = layout.horizontalSpacing = 0;
		upperCmp.setLayout(layout);
		upperCmp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnSetPwd = toolkit.createButton(upperCmp, Messages.InformantDataEditor_Button_SetPassword, SWT.PUSH);
		btnSetPwd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performSetPassword();
			}
		});

		Button btnClearPwd = toolkit.createButton(upperCmp, Messages.InformantDataEditor_Button_ClearPassword, SWT.PUSH);
		btnClearPwd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performClearPassword();
			}
		});

		Button btnEdit = toolkit.createButton(upperCmp, Messages.InformantDataEditor_Button_Edit, SWT.PUSH);
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				performEdit();
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

		viewer.setItemCount(0);
		addColumns(viewer);
		viewer.setInput(informantList);
	}

	protected void performSetPassword() {
		PasswordInputDialog dialog = new PasswordInputDialog(getSite().getShell());
		if (dialog.open() == Window.OK) {
			InformantAesManager.getInstance().setPassword(dialog.getPassword());
			viewer.refresh();
		}
	}

	protected void performClearPassword() {
		InformantAesManager.getInstance().clear();
		viewer.refresh();
	}

	protected void performEdit() {
		IStructuredSelection selection = (IStructuredSelection) viewer.getSelection();
		if (selection != null && !selection.isEmpty()) {
			Informant i = (Informant) selection.getFirstElement();
			InformantEditor dialog = new InformantEditor(getSite().getShell(), i);
			if (dialog.open() == Window.OK) {
				viewer.refresh();
			}
		}
	}
	
	private void addColumns(TableViewer v) {
		//public data
		TableViewerColumn idColumn = new TableViewerColumn(v, SWT.NONE);
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
	public boolean isDirty() {
		return false;
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
