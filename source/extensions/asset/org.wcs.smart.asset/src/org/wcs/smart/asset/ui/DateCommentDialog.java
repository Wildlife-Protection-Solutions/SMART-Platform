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
package org.wcs.smart.asset.ui;

import java.time.LocalDateTime;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * A dialog to collect a comment and associated date time
 * @author Emily
 *
 */
public class DateCommentDialog extends CommentDialog {

	protected DateTime dtDate;
	protected DateTime dtTime;
	
	protected LocalDateTime selectedDateTime;
	
	public DateCommentDialog(Shell parentShell, String title, String message) {
		super(parentShell, title, message);
	}

	public void setValues(LocalDateTime dateTime, String comment) {
		this.selectedDateTime = dateTime;
		this.comment = comment;
	}
	
	public void okPressed() {
		this.selectedDateTime = SmartUtils.toDateTime(dtDate,  dtTime);
		super.okPressed();
	}
	
	public LocalDateTime getSelectedDateTime() {
		return this.selectedDateTime;
	}
	
	@Override
	protected void createMessageContent(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.DateCommentDialog_DateLabel);
	
		dtDate = new DateTime(main, SWT.DATE | SWT.DROP_DOWN);
		dtTime = new DateTime(main, SWT.TIME | SWT.DROP_DOWN);
		if (selectedDateTime == null) selectedDateTime = LocalDateTime.now();
		
		SmartUtils.initDateTimeWidget(dtDate, selectedDateTime.toLocalDate());
		SmartUtils.initDateTimeWidget(dtTime, selectedDateTime.toLocalTime());
		
		l = new Label(main, SWT.NONE);
		l.setText(Messages.DateCommentDialog_CommentLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		txtComment = new Text(main, SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.BORDER);
		txtComment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		if (this.comment != null) txtComment.setText(this.comment);
	}
}
