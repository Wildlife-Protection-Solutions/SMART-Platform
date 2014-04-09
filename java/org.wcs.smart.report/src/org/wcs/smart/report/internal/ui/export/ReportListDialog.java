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
package org.wcs.smart.report.internal.ui.export;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.report.internal.Messages;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.ui.LazyReportContentProvider;
import org.wcs.smart.report.ui.ReportLabelProvider;

/**
 * Dialog for displaying list of reports from the current
 * Conservation Area
 * 
 * @author Emily
 *
 */
public class ReportListDialog extends TitleAreaDialog{

	private TreeViewer reportList;
	private List<Report> reports;

	
	public ReportListDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	public Point getInitialSize(){
		Point p = super.getInitialSize();
		return new Point(p.x, Math.max(p.y*2, 500));
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	/**
	 * Create contents of the dialog.
	 */
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);
		
		reportList = new TreeViewer(composite, SWT.MULTI);
		reportList.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		reportList.setContentProvider(new LazyReportContentProvider());
		reportList.setLabelProvider(new ReportLabelProvider());
		reportList.setInput(Messages.ReportListDialog_LoadingLabel);
		reportList.expandToLevel(2);
		
		setMessage(Messages.ReportListDialog_Title);
		setTitle(Messages.ReportListDialog_Message);
		getShell().setText(Messages.ReportListDialog_ShellTitle);
		return parent;
		
	}
		
	@Override
	public void okPressed(){
		IStructuredSelection sel = (IStructuredSelection)reportList.getSelection();
		reports = new ArrayList<Report>();
		for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
			Object obj = (Object) iterator.next();
			if (obj instanceof Report){
				reports.add((Report)obj);
			}
		}
		super.okPressed();
	}
	
	public List<Report> getSelectedReports(){
		return this.reports;
	}

}
