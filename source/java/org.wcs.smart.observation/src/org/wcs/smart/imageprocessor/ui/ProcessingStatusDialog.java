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
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.imageprocessor.IProcessingMonitor;
import org.wcs.smart.imageprocessor.ImageResizeProcessor;
import org.wcs.smart.imageprocessor.ProcessingItem;
import org.wcs.smart.observation.WaypointSourceEngine;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointAttachment;
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
	
	private Label lblStatus;
	private ProgressBar pbar;
	
	private Session session;
	
	public ProcessingStatusDialog(Shell parentShell, ImageResizeProcessor processingJob) {
		super(parentShell);
		setShellStyle(SWT.TITLE | SWT.RESIZE | SWT.SYSTEM_MODAL);
		this.processingJob = processingJob;
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);
		
		session = HibernateManager.openSession();
		composite.addListener(SWT.Dispose, e->session.close());
		
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		resultsTable = new TableViewer(main, SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
		resultsTable.setContentProvider(ArrayContentProvider.getInstance());
		resultsTable.getTable().setLinesVisible(true);
		resultsTable.getTable().setHeaderVisible(true);
		resultsTable.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)resultsTable.getTable().getLayoutData()).heightHint = 250;
				
		TableViewerColumn colStatus = new TableViewerColumn(resultsTable, SWT.NONE);
		colStatus.getColumn().setText(Messages.ProcessingStatusDialog_StatusColumnName);
		colStatus.getColumn().setWidth(30);
		colStatus.setLabelProvider(new TableLabelProvider(0));
		colStatus.getColumn().addListener(SWT.Selection, e->{
			
			if (resultsTable.getInput() instanceof List) {
				resultsTable.getTable().setSortColumn(colStatus.getColumn());
				resultsTable.getTable().setSortDirection(resultsTable.getTable().getSortDirection() == SWT.UP ? SWT.DOWN : SWT.UP );
				@SuppressWarnings("unchecked")
				List<ProcessingItem> items = (List<ProcessingItem>) resultsTable.getInput();
				
				int factor = 1;
				if (resultsTable.getTable().getSortDirection() == SWT.DOWN) factor = -1;
				final int ffactor = factor;
				items.sort((a,b)->{
					int x = -1;
					if (a.getStatus() != null) x = a.getStatus().ordinal();
					int y = -1;
					if (b.getStatus() != null) y = b.getStatus().ordinal();
					return ffactor * Integer.compare(x,y);
				});
				resultsTable.refresh();
			}
		});
		
		
		TableViewerColumn colFile = new TableViewerColumn(resultsTable, SWT.NONE);
		colFile.getColumn().setText(Messages.ProcessingStatusDialog_FileColumnName);
		colFile.getColumn().setWidth(150);
		colFile.setLabelProvider(new TableLabelProvider(1));
		
		TableViewerColumn colMessage = new TableViewerColumn(resultsTable, SWT.NONE);
		colMessage.getColumn().setText(Messages.ProcessingStatusDialog_MessageColumName);
		colMessage.getColumn().setWidth(300);
		colMessage.setLabelProvider(new TableLabelProvider(2));
		
		TableViewerColumn colSource = new TableViewerColumn(resultsTable, SWT.NONE);
		colSource.getColumn().setText(Messages.ProcessingStatusDialog_SourceTableHeader);
		colSource.getColumn().setWidth(150);
		colSource.setLabelProvider(new TableLabelProvider(3));
		
		resultsTable.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		pbar = new ProgressBar(main,  SWT.HORIZONTAL);
		pbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		lblStatus = new Label(main, SWT.NONE);
		lblStatus.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
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
			lblStatus.setText(Messages.ProcessingStatusDialog_CancelledMessage);
			pbar.setSelection(pbar.getMaximum());
		}else {
			Object x = resultsTable.getInput();
			if (x instanceof List) {
				@SuppressWarnings("unchecked")
				List<ProcessingItem> items = (List<ProcessingItem>) x;
				Map<ProcessingItem.Status, Integer> cnts = new HashMap<>();
				for (ProcessingItem.Status s : ProcessingItem.Status.values()) {
					cnts.put(s, 0);
				}
				for (ProcessingItem i : items) {
					cnts.put(i.getStatus(), cnts.get(i.getStatus())+1);
				}
				StringBuilder sb = new StringBuilder();
				sb.append(MessageFormat.format("{0}: {1}", Messages.ProcessingStatusDialog_OkStatusLbl, cnts.get(ProcessingItem.Status.OK))); //$NON-NLS-1$
				sb.append("  "); //$NON-NLS-1$
				sb.append(MessageFormat.format("{0}: {1}", Messages.ProcessingStatusDialog_WarningStatusLbl, cnts.get(ProcessingItem.Status.WARNING))); //$NON-NLS-1$
				sb.append("  "); //$NON-NLS-1$
				sb.append(MessageFormat.format("{0}: {1}", Messages.ProcessingStatusDialog_ErrorStatusLabel, cnts.get(ProcessingItem.Status.ERROR))); //$NON-NLS-1$
				
				lblStatus.setText(MessageFormat.format(Messages.ProcessingStatusDialog_ProcessingCompleteMsg, sb.toString()));
			}
		}
		getButton(IDialogConstants.OK_ID).setEnabled(true);
		getButton(IDialogConstants.CANCEL_ID).setEnabled(false);
	}
	
	public void setFiles(List<ProcessingItem> items) {
		if (items.isEmpty()) {
			MessageDialog.openInformation(getShell(), Messages.ProcessingStatusDialog_NoDataHeader, Messages.ProcessingStatusDialog_NoDataMessage);
			super.setReturnCode(SWT.CANCEL);
			super.close();
			return;
		}
		pbar.setMaximum(items.size());
		resultsTable.setInput(items);
		refresh();
	}
	
	public void refresh() {
		resultsTable.refresh();
	}
	
	@SuppressWarnings("unchecked")
	public void refresh(ProcessingItem item) {
		resultsTable.refresh(item);
		resultsTable.reveal(item);
		
		List<ProcessingItem> items = (List<ProcessingItem>) resultsTable.getInput();
		pbar.setSelection(items.indexOf(item)+1);
		lblStatus.setText(MessageFormat.format(Messages.ProcessingStatusDialog_ProcessingMsg, item.getAttachment().getAttachmentFile().getFileName().toString()));
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
						case WARNING: return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ICON_STATUS_OKWARN);
						case OK: return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ICON_STATUS_OK);
						case PROCESSING: return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ICON_STATUS_INPROGRESS);
					}
				}
			}
			return null;
		}
		@Override
		public String getText(Object element) {
			if (element instanceof ProcessingItem pi) {
				if (col == 0) {
					return ""; //$NON-NLS-1$
				}else if (col == 1) {
					Path f = pi.getAttachment().getAttachmentFile();
					return f.getFileName().toString();
				}else if (col == 2) {
					return pi.getMessage();
				}else if (col == 3) {
					Waypoint wp = null;
					if (pi.getAttachment() instanceof WaypointAttachment wa) {
						wp = wa.getWaypoint();
					}else if (pi.getAttachment() instanceof ObservationAttachment oa) {
						wp =oa.getObservation().getWaypoint();
					}
					if (wp == null) return ""; //$NON-NLS-1$

					return WaypointSourceEngine.INSTANCE.getSource(wp.getSourceId()).getSourceLabel(wp, session, Locale.getDefault());
				}
			}
			return super.getText(element);
		}

	}
}
