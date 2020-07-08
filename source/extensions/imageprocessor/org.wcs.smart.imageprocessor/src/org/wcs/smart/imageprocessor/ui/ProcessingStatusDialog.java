/*
 * Copyright (C) 2018 Wildlife Conservation Society
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
package org.wcs.smart.imageprocessor.ui;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.imageprocessor.IProcessingMonitor;
import org.wcs.smart.imageprocessor.ImageProcessingPlugIn;
import org.wcs.smart.imageprocessor.ImageResizeProcessor;
import org.wcs.smart.imageprocessor.ProcessingItem;
import org.wcs.smart.imageprocessor.internal.Messages;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for displaying the processing status of images
 * to the user.
 * 
 * @author Emily
 *
 */
public class ProcessingStatusDialog extends SmartStyledTitleDialog{
	
	private TableViewer resultsTable;
	private ImageResizeProcessor processingJob;
	private boolean isCancelled = false;
	
	public ProcessingStatusDialog(Shell parentShell, ImageResizeProcessor processingJob) {
		super(parentShell);
		setShellStyle(SWT.TITLE | SWT.RESIZE | SWT.SYSTEM_MODAL);
		this.processingJob = processingJob;
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		resultsTable = new TableViewer(main, SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
		resultsTable.setContentProvider(ArrayContentProvider.getInstance());
		resultsTable.getTable().setLinesVisible(true);
		resultsTable.getTable().setHeaderVisible(true);
		resultsTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TableViewerColumn colStatus = new TableViewerColumn(resultsTable, SWT.NONE);
		colStatus.getColumn().setText(Messages.ProcessingStatusDialog_StatusColumnName);
		colStatus.getColumn().setWidth(30);
		colStatus.setLabelProvider(new TableLabelProvider(0));
		
		TableViewerColumn colFile = new TableViewerColumn(resultsTable, SWT.NONE);
		colFile.getColumn().setText(Messages.ProcessingStatusDialog_FileColumnName);
		colFile.getColumn().setWidth(150);
		colFile.setLabelProvider(new TableLabelProvider(1));
		
		TableViewerColumn colMessage = new TableViewerColumn(resultsTable, SWT.NONE);
		colMessage.getColumn().setText(Messages.ProcessingStatusDialog_MessageColumName);
		colMessage.getColumn().setWidth(300);
		colMessage.setLabelProvider(new TableLabelProvider(2));
		
		
		resultsTable.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		setTitle(Messages.ProcessingStatusDialog_Title);
		getShell().setText(Messages.ProcessingStatusDialog_Title);
		setMessage(Messages.ProcessingStatusDialog_Message);
		
		processingJob.setStatusMonitor(new IProcessingMonitor() {
			@Override
			public void setItems(List<ProcessingItem> items) {
				Display.getDefault().asyncExec(()->ProcessingStatusDialog.this.setFiles(items));
			}
			@Override
			public void done() {
				Display.getDefault().asyncExec(()->ProcessingStatusDialog.this.done());
			}
			@Override
			public void begin() {
				Display.getDefault().asyncExec(()->ProcessingStatusDialog.this.begin());
			}
			@Override
			public void update(ProcessingItem item) {
				Display.getDefault().asyncExec(()->ProcessingStatusDialog.this.refresh(item));
			}
			@Override
			public boolean isCancelled() {
				return ProcessingStatusDialog.this.isCancelled;
			}
			
		});
		processingJob.schedule();
		
		return composite;
	}
	
	public void begin() {
		resultsTable.setInput(new String[] {Messages.ProcessingStatusDialog_SearchingStatus});
	}
	
	public void done() {
		if (isCancelled) {
			setErrorMessage(Messages.ProcessingStatusDialog_CancelledMessage);
		}
		getButton(IDialogConstants.OK_ID).setEnabled(true);
		getButton(IDialogConstants.CANCEL_ID).setEnabled(false);
	}
	
	public void setFiles(List<ProcessingItem> items) {
		resultsTable.setInput(items);
		refresh();
	}
	
	public void refresh() {
		resultsTable.refresh();
	}
	
	public void refresh(ProcessingItem item) {
		resultsTable.refresh(item);
		resultsTable.reveal(item);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
		Button b = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		b.setEnabled(false);
	}
	
	@Override
	public void cancelPressed() {
		this.isCancelled = true;
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
	
	private class TableLabelProvider extends ColumnLabelProvider{
		private int col;
		public TableLabelProvider(int col) {
			super();
			this.col = col;
		}
		
		public Image getImage(Object element) {
			if (element instanceof ProcessingItem) {
				if (((ProcessingItem) element).getStatus() == null) return null;
				if (col == 0) {
					switch(((ProcessingItem) element).getStatus()) {
						case ERROR: return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON);
						case WARNING: return ImageProcessingPlugIn.getDefault().getImageRegistry().get(ImageProcessingPlugIn.WARNING_ICON);
						case OK: return ImageProcessingPlugIn.getDefault().getImageRegistry().get(ImageProcessingPlugIn.OK_ICON);
						case PROCESSING: return ImageProcessingPlugIn.getDefault().getImageRegistry().get(ImageProcessingPlugIn.PROCESSING_ICON);
					}
				}
			}
			return null;
		}
		@Override
		public String getText(Object element) {
			if (element instanceof ProcessingItem) {
				if (col == 0) {
					return ""; //$NON-NLS-1$
				}else if (col == 1) {
					Path f = ((ProcessingItem) element).getAttachment().getAttachmentFile();
					return f.getFileName().toString() + " (" + f.toString() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				}else if (col == 2) {
					return ((ProcessingItem) element).getMessage();
				}
			}
			return super.getText(element);
		}
	}
}
