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
package org.wcs.smart.query.common.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.query.common.engine.IPagedQueryResultSet;
import org.wcs.smart.query.common.model.IObservationPagedQueryResultSet;
import org.wcs.smart.query.internal.Messages;
/**
 * Summary info section for an observation query
 * which displays both the observation and incident count. 
 * 
 * @author Emily
 *
 */
public class ObservationQuerySummaryInfo implements ISummaryInfo {

	private Label lblNumResults;
	private Label lblIncidentCnt;
	
	@Override
	public void createControls(Composite parent, FormToolkit toolkit) {
		GridLayout layout = new GridLayout(7,false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		parent.setLayout(layout);
		
		toolkit.createLabel(parent,  Messages.QueryEditorTableContent_NumberOfRecordsLabel1);
		lblNumResults = toolkit.createLabel(parent, Messages.QueryEditorTableContent_NaLabel);
		
		toolkit.createLabel(parent,  "  "); //$NON-NLS-1$
		Label l = toolkit.createLabel(parent, "", SWT.SEPARATOR | SWT.VERTICAL); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		((GridData)l.getLayoutData()).heightHint = 20;
		toolkit.createLabel(parent,  "  "); //$NON-NLS-1$
		
		toolkit.createLabel(parent,  Messages.QueryEditorTableContent_NumberOfIncidentLabel);
		lblIncidentCnt = toolkit.createLabel(parent, Messages.QueryEditorTableContent_NaLabel);
	}

	@Override
	public void updateControls(IPagedQueryResultSet resultSet) {
		lblNumResults.setText(Messages.ObservationQuerySummaryInfo_NAValueLabel);
		lblIncidentCnt.setText(Messages.ObservationQuerySummaryInfo_NAValueLabel);
		
		if (resultSet != null ){
			lblNumResults.setText(String.valueOf(resultSet.getItemCount()));
		}
		if (resultSet != null && resultSet instanceof IObservationPagedQueryResultSet ){
			lblIncidentCnt.setText(String.valueOf(((IObservationPagedQueryResultSet)resultSet).getWpCount()));
		}
		lblNumResults.getParent().layout(true);
	}

}
