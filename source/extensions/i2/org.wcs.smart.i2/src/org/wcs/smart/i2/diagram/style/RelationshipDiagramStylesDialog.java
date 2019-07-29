/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.diagram.style;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.i2.RelationshipDiagramManager;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.RelationshipDiagramStyle;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for managing relationship diagram styles.
 *  
 * @author elitvin
 * @since 6.0.0
 *
 */
public class RelationshipDiagramStylesDialog extends AbstractPropertyJHeaderDialog {

	private static final int DIALOG_WIDTH = 440;
	private static final int DIALOG_HEIGHT = 500;
	
	private TableViewer stylesViewer;
	
	private Button btnCreate;
	private Button btnEdit;
	private Button btnDelete;
	
	private List<RelationshipDiagramStyle> currentStylesList = new ArrayList<>();

	private RelationshipDiagramStyleListLoadJob loadStyleListJob = new RelationshipDiagramStyleListLoadJob() {
		@Override
		protected void processData(List<RelationshipDiagramStyle> styles) {
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					if (!stylesViewer.getControl().isDisposed()) {
						currentStylesList = styles;
						stylesViewer.setInput(styles);
						stylesViewer.refresh();
						updateState();
					}
				}		
			});
		}
	};
	
	public RelationshipDiagramStylesDialog(Shell parent) {
		super(parent, Messages.RelationshipDiagramStylesDialog_Dialog_Title);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(DIALOG_WIDTH, DIALOG_HEIGHT);
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CLOSE_ID,IDialogConstants.CLOSE_LABEL, true);
		getButton(IDialogConstants.CLOSE_ID).setFocus();
		super.setReturnCode(IDialogConstants.CLOSE_ID);
	}
	
	@Override
	protected Composite createContent(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite tableComp = new Composite(main, SWT.NONE);
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		TableColumnLayout tlayout = new TableColumnLayout();
		tableComp.setLayout(tlayout);
		
		stylesViewer = new TableViewer(tableComp, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		stylesViewer.setContentProvider(ArrayContentProvider.getInstance());
		stylesViewer.setInput(Arrays.asList(DialogConstants.LOADING_TEXT));
		
		TableViewerColumn col = new TableViewerColumn(stylesViewer, SWT.NONE);
		col.setLabelProvider(new RelationshipDiagramStyleLabelProvider());
		tlayout.setColumnData(col.getColumn(), new ColumnWeightData(1));
		
		
		stylesViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				editCurrentStyle();
			}
		});
		stylesViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateState();
			}
		});

		Menu mnu = new Menu(stylesViewer.getControl());
		
		MenuItem miAdd = new MenuItem(mnu, SWT.PUSH);
		miAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		miAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		miAdd.addListener(SWT.Selection, e->createNewStyle());
		
		MenuItem miEdit = new MenuItem(mnu, SWT.PUSH);
		miEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		miEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEdit.addListener(SWT.Selection, e->editCurrentStyle());
		
		MenuItem miDelete = new MenuItem(mnu, SWT.PUSH);
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.addListener(SWT.Selection, e->createNewStyle());
		
		mnu.addMenuListener(new MenuListener() {
			@Override
			public void menuHidden(MenuEvent e) {}
			@Override
			public void menuShown(MenuEvent e) {
				miDelete.setEnabled(!stylesViewer.getSelection().isEmpty());
				miEdit.setEnabled(!stylesViewer.getSelection().isEmpty());
			}
		});
		stylesViewer.getControl().setMenu(mnu);
		
		
		Composite btnCmp = new Composite(main, SWT.NONE);
		btnCmp.setLayout(new GridLayout(1, false));
		btnCmp.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		((GridLayout)btnCmp.getLayout()).marginWidth = 0;
		((GridLayout)btnCmp.getLayout()).marginHeight = 0;
		
		btnCreate = new Button(btnCmp, SWT.PUSH);
		btnCreate.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnCreate.setBackground(btnCmp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnCreate.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnCreate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnCreate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				createNewStyle();
			}
		});

		btnEdit = new Button(btnCmp, SWT.PUSH);
		btnEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEdit.setBackground(btnCmp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnEdit.setText(Messages.RelationshipDiagramStylesDialog_Button_Edit);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editCurrentStyle();
			}
		});
		
		btnDelete = new Button(btnCmp, SWT.PUSH);
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDelete.setBackground(btnCmp.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDelete.setText(Messages.RelationshipDiagramStylesDialog_Button_Delete);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteCurrentStyle();
			}
		});

		updateState();
		setTitle(Messages.RelationshipDiagramStylesDialog_Dialog_Title);
		setMessage(Messages.RelationshipDiagramStylesDialog_Dialog_Message);
		
		loadStyleListJob.schedule();
		
		return main;
	}
	
	protected void updateState() {
		RelationshipDiagramStyle p = getSelectedStyle();
		btnEdit.setEnabled(p != null);
		btnDelete.setEnabled(p != null && !p.isDefault());
	}
	
	private void reloadData() {
		loadStyleListJob.cancel();
		loadStyleListJob.schedule();
	}
	
	protected RelationshipDiagramStyle getSelectedStyle() {
		IStructuredSelection selection = (IStructuredSelection) stylesViewer.getSelection();
		return (!selection.isEmpty() && selection.getFirstElement() instanceof RelationshipDiagramStyle) ?
				(RelationshipDiagramStyle) selection.getFirstElement() : null;
	}

	protected void createNewStyle() {
		CreateNewStyleOpDialog opDialog = new CreateNewStyleOpDialog(getShell(), currentStylesList);
		if (opDialog.open() == Window.OK){
			RelationshipDiagramStyle initStyle  = null;
		
			try{
				initStyle = opDialog.getStyle();
			}catch (Exception ex){
				SmartPlugIn.displayLog(Messages.RelationshipDiagramStylesDialog_CreateStyle_Error, ex);
				return;
			}
			if (initStyle == null){
				//cancelled or invalid model
				return;
			}
			
			Dialog dialog = new RelationshipDiagramStyleEditDialog(getShell(), initStyle);
			dialog.open();
			
			//refresh list
			reloadData();
		}
	}

	protected void deleteCurrentStyle() {
		final RelationshipDiagramStyle style = getSelectedStyle();
		if (style == null) {
			return;
		}
		if (!MessageDialog.openConfirm(getShell(), Messages.RelationshipDiagramStylesDialog_ConfirmDelete_Title, MessageFormat.format(Messages.RelationshipDiagramStylesDialog_ConfirmDelete_Message, style.getName()))){
			return;
		}
		RelationshipDiagramManager.INSTANCE.deleteStyle(getShell(), style);
		reloadData();
	}

	protected void editCurrentStyle() {
		Dialog dialog = new RelationshipDiagramStyleEditDialog(getShell(), getSelectedStyle());
		dialog.open();
		reloadData();
	}

	@Override
	protected boolean performSave() {
		return true;
	}

}
