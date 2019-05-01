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

import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Asset editor section header.
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class SectionHeader extends Composite{

	private static final String TAB_MOUSEOVER_CLASS = "SMARTTabMouseOver"; //$NON-NLS-1$
	public static final String TAB_BAR_CLASS = "SMARTTabBar"; //$NON-NLS-1$
	private static final String TAB_CLASS = "SMARTTab"; //$NON-NLS-1$
	private static final String TAB_SELECTED_CLASS = "SMARTTabSelected"; //$NON-NLS-1$
	
	private static final String EVENT_KEY = "EVENT"; //$NON-NLS-1$
	
	private List<CLabel> links;
	
	private Color borderColor = null;
	private int fontsize = 0;
	
	public SectionHeader(Composite parent, int style, String[] headers, Listener[] actions) {
		this(parent, style, 0, headers,actions);
	}
	public SectionHeader(Composite parent, int style, int fontsize, String[] headers, Listener[] actions) {
		super(parent, style);
		this.fontsize = fontsize;
		WidgetElement.setCSSClass(this, TAB_BAR_CLASS);
		WidgetElement.applyStyles(this, true);
		
		addListener(SWT.Paint, e->{
			CLabel t = null;
			for (CLabel l : links) {
				if (WidgetElement.getCSSClass(l).equalsIgnoreCase(TAB_SELECTED_CLASS)){
					t = l;
				}
			}
			borderColor = t.getBackground();
			drawBorder(e.gc);
		});
		createComponent(headers, actions);
	}
	
	private void drawBorder(GC gc) {
		if (borderColor == null) return;
		Rectangle rr = SectionHeader.this.getBounds();
		gc.setForeground(borderColor);
		gc.setLineWidth(2);
		gc.drawLine(0, rr.height-1, rr.width, rr.height-1);
		gc.setLineWidth(1);
	}
	
	private void createComponent(String[] headers, Listener[] actions) {
		
		setLayout(new GridLayout(headers.length, false));
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		((GridLayout)getLayout()).horizontalSpacing = 0;
		
		links = new ArrayList<>();
		for (int i = 0; i < headers.length; i ++) {
			links.add(createTab(headers[i], actions[i]));
		}
		
	}
	
	public void selectPanel(int index) {
		selectTab(links.get(index));
		((Listener)links.get(index).getData(EVENT_KEY)).handleEvent(new Event());
	}

	private void selectTab(CLabel link) {
		links.forEach(l->WidgetElement.setCSSClass(l, TAB_CLASS));
		WidgetElement.setCSSClass(link, TAB_SELECTED_CLASS);
		WidgetElement.applyStyles(this, true);
	}
	
	private CLabel createTab(String text, Listener action) {
		
		CLabel tab = new CLabel(this, SWT.NONE);
		tab.setText(text);
		tab.setMargins(3, 5, 7, 3);
		
		if (fontsize != 0) {
			FontData fd = tab.getFont().getFontData()[0];
			fd.setHeight(fd.getHeight() + fontsize);
			Font font = new Font(tab.getDisplay(),fd);
			tab.setFont(font);
			tab.addListener(SWT.Dispose, e->font.dispose());
		}
		tab.addListener(SWT.Paint,e->drawBorder(e.gc));
		WidgetElement.setCSSClass(tab, TAB_CLASS);
		
		tab.addMouseTrackListener(new MouseTrackListener() {

			private String lastClass = null;
			@Override
			public void mouseEnter(MouseEvent e) {
				tab.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
				
				lastClass = WidgetElement.getCSSClass(tab);
				if (!lastClass.equals(TAB_SELECTED_CLASS)) {
					WidgetElement.setCSSClass(tab, TAB_MOUSEOVER_CLASS);
					WidgetElement.applyStyles(tab, true);
				}
			}

			@Override
			public void mouseExit(MouseEvent e) {
				tab.setCursor(null);
				String currentClass = WidgetElement.getCSSClass(tab);
				if (!currentClass.equals(TAB_SELECTED_CLASS)) {
					WidgetElement.setCSSClass(tab, lastClass);
					WidgetElement.applyStyles(tab, true);
				}
			}

			@Override
			public void mouseHover(MouseEvent e) {
			}

		});
		
		Listener click = e->{
			action.handleEvent(null);
			selectTab(tab);
			layout();
		};
		tab.addListener(SWT.MouseUp, click);
		tab.setData(EVENT_KEY, click);
		return tab;
	}
}
