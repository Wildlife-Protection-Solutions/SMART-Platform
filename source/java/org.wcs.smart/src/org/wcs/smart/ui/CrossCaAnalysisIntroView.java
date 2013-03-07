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
package org.wcs.smart.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.ViewPart;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

/**
 * Introduction to cross conservation area analysis view.
 * @author Emily
 *
 */
public class CrossCaAnalysisIntroView extends ViewPart {

	public static final String ID = "org.wcs.smart.crossCaView"; //$NON-NLS-1$
	
	private final FormToolkit toolkit = new FormToolkit(Display.getCurrent());
	private Label lblHeader = null;
	private Font boldFont = null;
	public CrossCaAnalysisIntroView() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void createPartControl(Composite parent) {
		Composite main = toolkit.createComposite(parent);
		
		main.setLayout(new GridLayout(1, false));
		lblHeader = toolkit.createLabel(main, Messages.CrossCaView_Header, SWT.NONE);
		
		FontData fd = lblHeader.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(Display.getCurrent(), fd);
		lblHeader.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)lblHeader.getLayoutData()).widthHint = 100;
		lblHeader.setFont(boldFont);
		lblHeader.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (boldFont != null){
					boldFont.dispose();
					boldFont = null;
				}
			}
		});
		
		toolkit.createLabel(main, Messages.CrossCaView_Message1, SWT.NONE);
		
		if (SmartDB.getConservationAreaConfiguration() != null){
			for(ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
				Label l = toolkit.createLabel(main, ca.getNameLabel());
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				((GridData)l.getLayoutData()).horizontalIndent = 10;
			}
		}
		
		
		Label ll = toolkit.createLabel(main, Messages.CrossCaView_Message2, SWT.WRAP);
		ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)ll.getLayoutData()).widthHint = 100;
	}

	@Override
	public void setFocus() {
		lblHeader.setFocus();
	}

}
