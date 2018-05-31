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
package org.wcs.smart.i2.ui.dialogs;

import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.i2.model.RelationshipDiagramStyle;
import org.wcs.smart.ui.NamedItemLabelProvider;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

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
	
	public RelationshipDiagramStylesDialog(Shell parent) {
		super(parent, "Relationship Diagram Styles");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(DIALOG_WIDTH, DIALOG_HEIGHT);
	}

	@Override
	protected Composite createContent(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		stylesViewer = new TableViewer(main, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		stylesViewer.setLabelProvider(new NamedItemLabelProvider()); //TODO: ZZZZZZZZ need custom label provider that will mark default style
		stylesViewer.setContentProvider(ArrayContentProvider.getInstance());
		stylesViewer.setInput(getStylesList());
		stylesViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
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

		Composite btnCmp = new Composite(main, SWT.NONE);
		btnCmp.setLayout(new GridLayout(1, false));
		btnCmp.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));
		
		btnCreate = new Button(btnCmp, SWT.PUSH);
		btnCreate.setText("Create New...");
		btnCreate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnCreate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				createNewStyle();
			}
		});

		btnEdit = new Button(btnCmp, SWT.PUSH);
		btnEdit.setText("Edit...");
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnEdit.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editCurrentStyle();
			}
		});
		
		btnDelete = new Button(btnCmp, SWT.PUSH);
		btnDelete.setText("Delete");
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				deleteCurrentStyle();
			}
		});

		updateState();
		setTitle("Relationship Diagram Styles");
		setMessage("Manage the list of available styles for relationship diagram.\nDouble click to edit.");
		
		return main;
	}
	
	protected void updateState() {
		RelationshipDiagramStyle p = getSelectedStyle();
		btnEdit.setEnabled(p != null);
		btnDelete.setEnabled(p != null && !p.isDefault());
	}
	
	private void reloadData() {
		stylesViewer.setInput(getStylesList());
		
		stylesViewer.refresh();
		updateState();
	}
	
	protected RelationshipDiagramStyle getSelectedStyle() {
		IStructuredSelection selection = (IStructuredSelection) stylesViewer.getSelection();
		return (!selection.isEmpty() && selection.getFirstElement() instanceof RelationshipDiagramStyle) ?
				(RelationshipDiagramStyle) selection.getFirstElement() : null;
	}

	protected void createNewStyle() {
//		CreateNewStyleOpDialog opDialog = new CreateNewStyleOpDialog(getShell(), getStylesList());
//		if (opDialog.open() == Window.OK){
//			RelationshipDiagramStyle initStyle  = null;
//		
//			try{
//				initStyle = opDialog.getStyle();
//			}catch (Exception ex){
//				SmartPlugIn.displayLog(Messages.ManageStylesDialog_CreateStyle_Erorr + ex.getLocalizedMessage(), ex);
//				return;
//			}
//			if (initStyle == null){
//				//cancelled or invalid model
//				return;
//			}
//			Dialog dialog = new CyberTrackerPropertiesDialog(getShell(), initStyle);
//			dialog.open();
//			
//			//refresh list
//			reloadData();
//		}
	}

	protected void deleteCurrentStyle() {
//		final RelationshipDiagramStyle p = getSelectedStyle();
//		if (p == null){
//			return;
//		}
//		if (!MessageDialog.openConfirm(getShell(), Messages.ManageStylesDialog_DeleteConfirmDialog_Title, MessageFormat.format(Messages.ManageStylesDialog_DeleteConfirmDialog_Message, p.getName()))){
//			return;
//		}
//
//		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
//		try {
//			pmd.run(true, false, new IRunnableWithProgress() {
//
//				@Override
//				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//					monitor.beginTask(Messages.ManageStylesDialog_DeleteTask_Name, 1);
//					
//					try(Session session = HibernateManager.openSession()){
//						session.beginTransaction();
//						try {
//							CyberTrackerHibernateManager.deleteStyle(session, p);
//							session.getTransaction().commit();							
//						}catch (Exception ex){
//							session.getTransaction().rollback();
//							SmartPlugIn.displayLog(Messages.ManageStylesDialog_DeleteTask_Error, ex);
//						}
//					} finally {
//						monitor.done();
//					}
//				}
//			});
//		} catch (Exception ex) {
//			SmartPlugIn.displayLog(Messages.ManageStylesDialog_DeleteTask_Error, ex);
//		}
//		
//		reloadData();
	}

	protected void editCurrentStyle() {
//		Dialog dialog = new CyberTrackerPropertiesDialog(getShell(), getSelectedStyle());
//		dialog.open();
//		reloadData();
	}

	private List<RelationshipDiagramStyle> getStylesList() {
//		final List<RelationshipDiagramStyle> styleList = new ArrayList<RelationshipDiagramStyle>();
//		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
//		try {
//			pmd.run(true, false, new IRunnableWithProgress() {
//				@Override
//				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//					monitor.beginTask(Messages.ManageStylesDialog_LoadStyleList_Task, 1);
//					try(Session session = HibernateManager.openSession()){
//						session.beginTransaction();
//						try {
//							styleList.addAll(CyberTrackerHibernateManager.getPropertiesStyles(session));
//							Collections.sort(styleList, new CtStyleDefaultNameComparator());
//						} catch (Exception ex) {
//							SmartPlugIn.displayLog(Messages.ManageStylesDialog_LoadStyleList_Error, ex);
//						} finally {
//							session.getTransaction().rollback();
//						}
//					}
//				}
//			});
//		} catch (Exception e) {
//			SmartPlugIn.displayLog(Messages.ManageStylesDialog_LoadStyleList_Error, e);
//			return Collections.emptyList();
//		}
//		return styleList;
		return Collections.emptyList();
	}

	@Override
	protected boolean performSave() {
		return true;
	}

}
