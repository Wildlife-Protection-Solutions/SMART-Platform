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
package org.wcs.smart.i2.ui.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.wcs.smart.i2.Intelligence2PlugIn;

/**
 * Editor section tab  header.
 * 
 * @author Emily
 *
 */
public class SectionTabHeader extends Composite implements IHyperlinkListener{

	private Composite[] parts;
	private Composite stackPanel;
	private Font boldFont;
	private Font normalFont;
	private Hyperlink[] headers;
	
	public SectionTabHeader(String tabs[], Composite parent, FormToolkit toolkit, Color background, Runnable onMaximize){
		super(parent, SWT.NONE);
		
		setLayout(new GridLayout(tabs.length + (onMaximize == null ? 0 : 1), false));
		setBackground(background);
		((GridLayout)getLayout()).marginHeight = 2;
		((GridLayout)getLayout()).marginWidth = 0;
		
		
		headers = new Hyperlink[tabs.length];
		for (int i = 0; i < tabs.length; i ++){
			Hyperlink header = toolkit.createHyperlink(this, tabs[i], SWT.NONE);
			header.setBackground(background);
			headers[i] = header;
			normalFont = header.getFont();
			header.setData(i);
			header.addHyperlinkListener(this);
		}
		
		FontData fd = headers[0].getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		boldFont = new Font(getDisplay(), fd);
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				boldFont.dispose();
			}
		});

		if (onMaximize != null){
			Label max = toolkit.createLabel(this, "");
			max.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
			max.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_SECTION_EXPAND));
			max.addListener(SWT.MouseUp, (e)->onMaximize.run());
			max.setBackground(background);
		}
	}
	public SectionTabHeader(String tabs[], Composite parent, FormToolkit toolkit, Runnable onMaximize){
		this(tabs, parent, toolkit, toolkit.getColors().getColor(IFormColors.TB_BG), onMaximize);
	}
	
	public SectionTabHeader(String tabs[], Composite parent, FormToolkit toolkit, Color background){
		this(tabs, parent, toolkit, background, null);
	}
	public SectionTabHeader(String tabs[], Composite parent, FormToolkit toolkit){
		this(tabs, parent, toolkit, toolkit.getColors().getColor(IFormColors.TB_BG));
	}
	
	public void setContent(Composite[] parts, Composite stackPanel){
		this.parts = parts;
		this.stackPanel = stackPanel;
	}
	
	public void selectTab(int tab){
		linkActivated(new HyperlinkEvent(headers[0], null, null, -1));
	}

	@Override
	public void linkExited(HyperlinkEvent e) {
	}
	
	@Override
	public void linkEntered(HyperlinkEvent e) {
	}
	
	@Override
	public void linkActivated(HyperlinkEvent e) {
		for (Hyperlink link : headers){
			if (link == e.widget){
				((StackLayout)stackPanel.getLayout()).topControl = parts[(int)link.getData()];
				link.setFont(boldFont);
			}else{
				link.setFont(normalFont);
			}
		}
		layout(true);
		stackPanel.layout();
	}
}
