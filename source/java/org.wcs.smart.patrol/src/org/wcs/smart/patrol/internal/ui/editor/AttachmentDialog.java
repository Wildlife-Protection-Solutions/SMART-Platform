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
package org.wcs.smart.patrol.internal.ui.editor;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.databinding.observable.list.WritableList;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.Waypoint;
import org.wcs.smart.patrol.model.WaypointAttachment;

/**
 * Dialog for displaying attachments associated 
 * with a given waypoint.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttachmentDialog extends TitleAreaDialog {

	private TableViewer tblAttachments;
	private WritableList attachments = new WritableList();
	private Button btnRemove;
	private Button btnOpen;

	
	/**
	 * @param parentShell
	 */
	public AttachmentDialog(Shell parentShell, Waypoint wp) {
		super(parentShell);
		
		if (wp.getAttachments() != null){
			this.attachments.addAll(wp.getAttachments());
		}
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite)super.createDialogArea(parent);
		
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label lblAttachments = new Label(main, SWT.NONE);
		lblAttachments.setText("Attachments:");
		lblAttachments.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		tblAttachments = new TableViewer(main, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		tblAttachments.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tblAttachments.getTable().getLayoutData()).heightHint = 100;
		((GridData)tblAttachments.getTable().getLayoutData()).widthHint = 200;
		tblAttachments.setContentProvider(new ObservableListContentProvider());
		tblAttachments.setLabelProvider(new LabelProvider(){
			public String getText(Object element) {
				if (element instanceof WaypointAttachment){
					return ((WaypointAttachment) element).getFilename();
				}
				return super.getText(element);
			}
		});
		tblAttachments.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				btnOpen.setEnabled(!tblAttachments.getSelection().isEmpty());
				btnRemove.setEnabled(!tblAttachments.getSelection().isEmpty());
				
			}
		});
		
		Composite buttonPanel = new Composite(main, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(1, false));
		buttonPanel.setLayoutData(new GridData(SWT.TOP, SWT.LEFT, false, false));
		Button btnAdd = new Button(buttonPanel, SWT.PUSH);
		btnAdd.setText("Add");
		btnAdd.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				FileDialog fd = new FileDialog(AttachmentDialog.this.getShell());
				String file = fd.open();
				if (file == null){
					return;
				}
				File f = new File(file);
				if (!f.exists()){
						SmartPatrolPlugIn.displayLog("The selected file does not exist: '" + file + "'", null);
					return;
				}
				WaypointAttachment wpa = new WaypointAttachment();
				wpa.setCopyFromLocation(f);
				wpa.setFilename(f.getName());
				attachments.add(wpa);
				tblAttachments.refresh();
//				}
			}
		});
		
		btnRemove = new Button(buttonPanel, SWT.PUSH);
		btnRemove.setText("Delete");
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = (IStructuredSelection)tblAttachments.getSelection();
				for (Iterator iterator = sel.iterator(); iterator.hasNext();) {
					WaypointAttachment type = (WaypointAttachment) iterator.next();
					attachments.remove(type);
				}
				tblAttachments.refresh();
			}
		});
		btnRemove.setEnabled(false);
		
		btnOpen = new Button(buttonPanel, SWT.PUSH);
		btnOpen.setText("Open");
		btnOpen.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = (IStructuredSelection)tblAttachments.getSelection();
				WaypointAttachment first = (WaypointAttachment) sel.getFirstElement();
				if (first.getCopyFromLocation()!= null){
					Program.launch(first.getCopyFromLocation().getAbsolutePath());
				}else{
					Program.launch(first.getFullFile().getAbsolutePath());
				}
			}
		});
		btnOpen.setEnabled(false);
		
		tblAttachments.setInput(this.attachments);
		tblAttachments.refresh();
		
		setMessage("List of attachments associated with this waypoint.");
		getShell().setText("Waypoint Attachments");
		return composite; 
	}


	/**
	 * @return all attachments selected by the user
	 */
	public List<WaypointAttachment> getAttchments(){
		return this.attachments;
	}

	/** dialog is resizable
	 * @see org.eclipse.jface.dialogs.Dialog#isResizable()
	 */
	protected boolean isResizable() {
		return true;
	}
}
