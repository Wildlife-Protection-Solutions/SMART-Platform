/*
 * Copyright (C) 2022 Wildlife Conservation Society
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
package org.wcs.smart.ui.internal.preference;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.wcs.smart.CaSizeReport;
import org.wcs.smart.common.attachment.AttachmentUtil;

/**
 * Preference page from managing inactivity timeout option
 * @author Emily
 *
 */
public class CaSizeReportPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {


	private StyledText txtReport;
	
	public CaSizeReportPreferencePage() {
	}

	public CaSizeReportPreferencePage(String title) {
		super(title);
	}

	public CaSizeReportPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}


	
	@Override
	public boolean performOk() {

		return true;
	}
	
	@Override
	protected Control createContents(Composite parent) {
		Composite temp = new Composite(parent, SWT.NONE);
		temp.setLayout(new GridLayout());
		temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		txtReport = new StyledText(temp, SWT.MULTI| SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
		txtReport.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txtReport.setEditable(false);
		txtReport.setBackground(txtReport.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		((GridData)txtReport.getLayoutData()).widthHint = 200;
		((GridData)txtReport.getLayoutData()).heightHint = 400;

		CaSizeReport report = new CaSizeReport();
		String text = report.generateReport();
		
		txtReport.setText(text);
		
		//add hyperlink
		StyleRange style = new StyleRange();
		style.underline = true;
		style.underlineStyle = SWT.UNDERLINE_LINK;

		int start = text.indexOf(CaSizeReport.INFO_URL);
		int[] ranges = {start,CaSizeReport.INFO_URL.length()};
		
		StyleRange[] styles = {style};
		txtReport.setStyleRanges(ranges, styles);
		
		txtReport.addListener(SWT.MouseDown, event -> {
			int offset = txtReport.getOffsetAtPoint(new Point (event.x, event.y));
			if (offset != -1) {
				StyleRange style1 = null;
				try {
					style1 = txtReport.getStyleRangeAtOffset(offset);
				} catch (IllegalArgumentException e) {
					// no character under event.x, event.y
				}
				if (style1 != null && style1.underline && style1.underlineStyle == SWT.UNDERLINE_LINK) {
					AttachmentUtil.launch(CaSizeReport.INFO_URL);
				}
			}	
		});
		
		return temp;
	}

	@Override
	public void init(IWorkbench workbench) {
	
	}
}
