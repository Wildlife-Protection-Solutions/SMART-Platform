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

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.ConservationAreaConfiguration;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

/**
 * Introduction to cross conservation area analysis view.
 * @author Emily
 *
 */
public class CrossCaAnalysisIntroView {

	public static final String ID = "org.wcs.smart.crossCaView"; //$NON-NLS-1$
	
	private FormToolkit toolkit = null;
	private Label lblHeader = null;
	private Font boldFont = null;
	
	private Composite caList = null;
	
	public CrossCaAnalysisIntroView() {
	}

	@PreDestroy
	public void dispose(){
		toolkit.dispose();
	}
	
	@Inject
	@Optional
	private void configurationChanged(@UIEventTopic(SmartDB.CCAA_CONFIGURATION_MODIFIED) ConservationAreaConfiguration newConfig){
		updateCas();
	}
	
	@PostConstruct
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		((FillLayout)parent.getLayout()).marginHeight = 0;
		((FillLayout)parent.getLayout()).marginWidth = 0;
		
		Composite main = toolkit.createComposite(parent);
		
		main.setLayout(new GridLayout(1, false));
		lblHeader = toolkit.createLabel(main, Messages.CrossCaView_Header, SWT.NONE);
		
		FontData fd = lblHeader.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(lblHeader.getDisplay(), fd);
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
		caList = toolkit.createComposite(main);
		caList.setLayout(new GridLayout());
		updateCas();
		Label ll = toolkit.createLabel(main, Messages.CrossCaView_Message2, SWT.WRAP);
		ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)ll.getLayoutData()).widthHint = 100;
		
	}

	@Focus
	public void setFocus() {
		lblHeader.setFocus();
	}
	
	private void updateCas(){
		if (caList != null){
			for (Control c : caList.getChildren()){
				c.dispose();
			}
			
			if (SmartDB.getConservationAreaConfiguration() != null){
				for(ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
					Label l = toolkit.createLabel(caList, ca.getNameLabel());
					l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					((GridData)l.getLayoutData()).horizontalIndent = 10;
				}
			}
			
			caList.getParent().layout(true, true);
		}
		
	}

	public static class CrossCaAnalysisIntroViewWrapper extends DIViewPart<CrossCaAnalysisIntroView>{
		public CrossCaAnalysisIntroViewWrapper(){
			super(CrossCaAnalysisIntroView.class);
		}
	}
}
