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
package org.wcs.smart.common.attachment;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Represents file attachment ui component.
 * 
 * The attachment composite has the ability to display two
 * sets of the attachments.  The "main" attachments which can be
 * added to and removed, and another set of "other" attachments which
 * can only be viewed.  This supports displaying all attachments associated
 * with a waypoint (the waypoint and observation attachments), but only 
 * allows uers to modify the waypoint attachments.  Use the
 * initOtherAttachments function to set the "other" attachments.
 * 
 * @author Emily
 * @author elitvin
 * @since 1.0.0
 */
public abstract class AttachmentComposite<T extends ISmartAttachment> extends Composite {

	private TableViewer tblAttachments;
	
	private ArrayList<T> attachments = new ArrayList<T>();
	private List<ISmartAttachment> other = new ArrayList<ISmartAttachment>();
	private List<ISmartAttachment> all = new ArrayList<ISmartAttachment>();
	
	private Button btnRemove;
	private Button btnOpen;
	
	private List<IAttachmentsChangeListener> attachmentsChangeListeners = new ArrayList<IAttachmentsChangeListener>();

	public AttachmentComposite(Composite parent, int style) {
		super(parent, style);
		createControls();
	}

	private void createControls() {
		this.setLayout(new GridLayout(2, false));
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label lblAttachments = new Label(this, SWT.NONE);
		lblAttachments.setText(Messages.AttachmentComposite_Label_Attachments);
		lblAttachments.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		tblAttachments = new TableViewer(this, SWT.MULTI | SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		tblAttachments.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)tblAttachments.getTable().getLayoutData()).heightHint = 100;
		((GridData)tblAttachments.getTable().getLayoutData()).widthHint = 200;
		tblAttachments.setContentProvider(ArrayContentProvider.getInstance());
		tblAttachments.setLabelProvider(new SmartAttachmentLabelProvider(){
			public String getText(Object element) {
				if (element instanceof ISmartAttachment){
					String text = ""; //$NON-NLS-1$
					if (other.contains(element)){
						text = "**"; //$NON-NLS-1$
					}
					text += super.getText(element);
					return text;
				}
				return super.getText(element);
			}
		});
		tblAttachments.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean empty = true;
				for (Iterator<?> iterator = ((StructuredSelection)tblAttachments.getSelection()).iterator(); iterator.hasNext();) {
					Object type = (Object) iterator.next();
					if (attachments.contains(type)){
						empty = false;
					}
					
				}
				btnOpen.setEnabled(!tblAttachments.getSelection().isEmpty());
				btnRemove.setEnabled(!empty);
				
			}
		});
		tblAttachments.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				ISmartAttachment a = (ISmartAttachment) ((IStructuredSelection)tblAttachments.getSelection()).getFirstElement();
				if (a != null){
					AttachmentUtil.openAttachment(a);
				}
			}
		});
		
		Composite buttonPanel = new Composite(this, SWT.NONE);
		buttonPanel.setLayout(new GridLayout(1, false));
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.LEFT, false, false));
		Button btnAdd = new Button(buttonPanel, SWT.PUSH);
		btnAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnAdd.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent e){
				FileDialog fd = new FileDialog(AttachmentComposite.this.getShell(), SWT.MULTI);
				
				String file = fd.open();
				if (file == null) {
					return;
				}
				for (int i = 0; i < fd.getFileNames().length; i ++){
					File f = new File(fd.getFilterPath() + File.separator +  fd.getFileNames()[i]);
					if (!f.exists()){
						SmartPlugIn.displayLog(MessageFormat.format(Messages.AttachmentComposite_Error_FileNotFound, new Object[]{f.getAbsolutePath()}), null);
						return;
					}
					T wpa = createNewAttachement();
					wpa.setCopyFromLocation(f);
					wpa.setFilename(f.getName());
					attachments.add(wpa);
				}
				fireAttachmentsChangeListeners();
				refreshTable();
			}
		});
		
		btnRemove = new Button(buttonPanel, SWT.PUSH);
		btnRemove.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = (IStructuredSelection)tblAttachments.getSelection();
				if (!sel.isEmpty()) {
					for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
						ISmartAttachment type = (ISmartAttachment) iterator.next();
						if (attachments.contains(type)){
							attachments.remove(type);
						}
					}
					fireAttachmentsChangeListeners();
				}
				refreshTable();
			}
		});
		btnRemove.setEnabled(false);
		
		btnOpen = new Button(buttonPanel, SWT.PUSH);
		btnOpen.setText(Messages.AttachmentComposite_Button_Open);
		btnOpen.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnOpen.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection sel = (IStructuredSelection)tblAttachments.getSelection();
				ISmartAttachment first = (ISmartAttachment) sel.getFirstElement();
				AttachmentUtil.openAttachment(first);
			}
		});
		btnOpen.setEnabled(false);
		
		tblAttachments.setInput(all);
		tblAttachments.refresh();
	}
	
	protected abstract T createNewAttachement();
	
	public void initAttachments(List<T> init){
		this.attachments.addAll(init);
		refreshTable();
	}
	
	private void refreshTable(){
		all.clear();
		all.addAll(this.attachments);
		all.addAll(this.other);
		
		tblAttachments.refresh();
	}
	
	public void initOtherAttachments(List<ISmartAttachment> init){
		other.clear();
		this.other.addAll(init);
		refreshTable();
	}
	
	/**
	 * @return all attachments selected by the user
	 */
	public List<T> getAttchments() {
		return this.attachments;
	}
	
	public void addAttachmentsChangeListener(IAttachmentsChangeListener listener) {
		attachmentsChangeListeners.add(listener);
	}

	public void removeAttachmentsChangeListener(IAttachmentsChangeListener listener) {
		attachmentsChangeListeners.remove(listener);
	}
	
	private void fireAttachmentsChangeListeners() {
		for (IAttachmentsChangeListener listener : attachmentsChangeListeners) {
			listener.attachmentsChanged();
		}
	}
}
