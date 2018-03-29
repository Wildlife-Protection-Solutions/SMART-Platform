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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;

/**
 * Asset editor section header.
 * 
 * @author Emily
 *
 */
public class SectionHeader extends Composite{

	private static final String EVENT_KEY = "EVENT"; //$NON-NLS-1$
	
	private FormToolkit toolkit;
	private List<Hyperlink> links;
	
	private Font normalFont;
	private Font boldFont;
	
	public SectionHeader(Composite parent, int style, String[] headers, Listener[] actions, FormToolkit toolkit) {
		super(parent, style);
		this.toolkit = toolkit;
		createComponent(headers, actions);
	}
	
	private void createComponent(String[] headers, Listener[] actions) {
		
		setLayout(new GridLayout(headers.length, false));
		((GridLayout)getLayout()).marginWidth = 2;
		((GridLayout)getLayout()).marginHeight = 2;
		setBackground( toolkit.getColors().getColor(IFormColors.TB_BG) );
		
		FontData fd = getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(getDisplay(), fd);
		addListener(SWT.Dispose, e->boldFont.dispose());
		normalFont = getFont();
		
		links = new ArrayList<>();
		for (int i = 0; i < headers.length; i ++) {
			links.add(createHyperlink(headers[i], actions[i]));
		}
		
	}
	
	public void selectPanel(int index) {
		((IHyperlinkListener)links.get(index).getData(EVENT_KEY)).linkActivated(new HyperlinkEvent(links.get(index), null, "", 0)); //$NON-NLS-1$
	}

	private Hyperlink createHyperlink(String text, Listener action) {
		
		Hyperlink lnkEvents = toolkit.createHyperlink(this, text, SWT.NONE);
		lnkEvents.setBackground(getBackground());
		
		IHyperlinkListener listener = new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				action.handleEvent(null);
				
				links.forEach(lnk->lnk.setFont(normalFont));
				lnkEvents.setFont(boldFont);
				layout();
			}
		};
		lnkEvents.addHyperlinkListener(listener);
		lnkEvents.setData(EVENT_KEY, listener);
		return lnkEvents;
	}
}
