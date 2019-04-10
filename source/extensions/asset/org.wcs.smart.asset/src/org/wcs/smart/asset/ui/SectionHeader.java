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
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * Asset editor section header.
 * 
 * @author Emily
 *
 */
public class SectionHeader extends Composite{

	private static final String EVENT_KEY = "EVENT"; //$NON-NLS-1$
	
	private List<CLabel> links;
	
	private Color borderColor = null; 
	
	public SectionHeader(Composite parent, int style, String[] headers, Listener[] actions) {
		super(parent, style);
		WidgetElement.setCSSClass(this, "SMARTTabBar");
		//TODO: fix this - currently this is the easiest way
		//I could figure out how to get the selected
		//tab color from the css
		Composite temp = new Composite(parent, SWT.NONE);
		WidgetElement.setCSSClass(temp, "SMARTTabSelected");
		WidgetElement.applyStyles(temp, true);
		borderColor = temp.getBackground();
		temp.dispose();

		addListener(SWT.Paint, e->{
			drawBorder(e.gc);
		});
		createComponent(headers, actions);
	}
	
	private void drawBorder(GC gc) {
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
		links.forEach(l->WidgetElement.setCSSClass(l, "SMARTTab"));
		WidgetElement.setCSSClass(link, "SMARTTabSelected");
		WidgetElement.applyStyles(this, true);
	}
	
	private CLabel createTab(String text, Listener action) {
		
		CLabel tab = new CLabel(this, SWT.NONE);
		tab.setText(text);
		tab.setMargins(3, 5, 5, 3);
		tab.addListener(SWT.Paint,e->drawBorder(e.gc));
		WidgetElement.setCSSClass(tab, "SMARTTab");
		
		tab.addMouseTrackListener(new MouseTrackListener() {

			private String lastClass = null;
			@Override
			public void mouseEnter(MouseEvent e) {
				tab.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
				
				lastClass = WidgetElement.getCSSClass(tab);
				if (!lastClass.equals("SMARTTabSelected")) {
					WidgetElement.setCSSClass(tab, "SMARTTabMouseOver");
					WidgetElement.applyStyles(tab, true);
				}
			}

			@Override
			public void mouseExit(MouseEvent e) {
				tab.setCursor(null);
				String currentClass = WidgetElement.getCSSClass(tab);
				if (!currentClass.equals("SMARTTabSelected")) {
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
