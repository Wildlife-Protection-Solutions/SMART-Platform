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
package org.wcs.smart.i2.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.i2.Intelligence2PlugIn;

/**
 *  Header for tabs that displays a list of strings
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class SectionTabHeader extends Composite implements Listener{

	private static final String TAB_MOUSEOVER_CLASS = "SMARTTabMouseOver"; //$NON-NLS-1$
	private static final String TAB_BAR_CLASS = "SMARTTabBar"; //$NON-NLS-1$
	private static final String TAB_CLASS = "SMARTTab"; //$NON-NLS-1$
	private static final String TAB_SELECTED_CLASS = "SMARTTabSelected"; //$NON-NLS-1$
	
	private Composite[] parts;
	private Composite stackPanel;
	
	private CLabel[] headers;
	
	private List<Listener> listeners;
	
	private Color borderColor = null;

	
	public SectionTabHeader(String tabs[], Composite parent, FormToolkit toolkit, Runnable onMaximize){
		super(parent, SWT.NONE);
		
		WidgetElement.setCSSClass(this, TAB_BAR_CLASS);
		
		addListener(SWT.Paint, e->{
			CLabel t = null;
			for (CLabel l : headers) {
				if (WidgetElement.getCSSClass(l).equalsIgnoreCase(TAB_SELECTED_CLASS)){
					t = l;
				}
			}
			borderColor = t.getBackground();
			drawBorder(e.gc);
		});
		
		setLayout(new GridLayout(tabs.length*2-1 + (onMaximize == null ? 0 : 1), false));
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		((GridLayout)getLayout()).horizontalSpacing = 0;
	
		listeners = new ArrayList<>();
		
		headers = new CLabel[tabs.length];
		for (int i = 0; i < tabs.length; i ++){
			CLabel header = new CLabel(this, SWT.NONE);
			header.setText(tabs[i]);
			header.setMargins(3, 5, 7, 3);
			WidgetElement.setCSSClass(header, TAB_CLASS);
			header.addListener(SWT.Paint,e->drawBorder(e.gc));
			
			headers[i] = header;
			
			header.setData(i);
			header.addListener(SWT.MouseUp, this);
			
			header.addMouseTrackListener(new MouseTrackListener() {

				private String lastClass = null;
				@Override
				public void mouseEnter(MouseEvent e) {
					header.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
					
					lastClass = WidgetElement.getCSSClass(header);
					if (!lastClass.equals(TAB_SELECTED_CLASS)) {
						WidgetElement.setCSSClass(header, TAB_MOUSEOVER_CLASS);
						WidgetElement.applyStyles(header, true);
					}
				}

				@Override
				public void mouseExit(MouseEvent e) {
					header.setCursor(null);
					String currentClass = WidgetElement.getCSSClass(header);
					if (!currentClass.equals(TAB_SELECTED_CLASS)) {
						WidgetElement.setCSSClass(header, lastClass);
						WidgetElement.applyStyles(header, true);
					}
				}

				@Override
				public void mouseHover(MouseEvent e) {
				}

			});
		}
		

		if (onMaximize != null){
			Label max = toolkit.createLabel(this, ""); //$NON-NLS-1$
			max.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
			max.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_SECTION_EXPAND));
			max.addListener(SWT.MouseUp, (e)->onMaximize.run());
			max.setBackground(max.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			max.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		}
	}
	
	
	public SectionTabHeader(String tabs[], Composite parent, FormToolkit toolkit){
		this(tabs, parent, toolkit, null);
	}
	
	private void drawBorder(GC gc) {
		if (borderColor == null) return;
		Rectangle rr = SectionTabHeader.this.getBounds();

		gc.setForeground(borderColor);
		gc.setLineWidth(2);
		gc.drawLine(0, rr.height-1, rr.width, rr.height-1);
		gc.setLineWidth(1);
	}
	
	public void addTabSelectionListener(Listener l) {
		listeners.add(l);
	}
	
	private void fireListeners() {
		Event evt = new Event();
		listeners.forEach(l->l.handleEvent(evt));
	}
	
	public int getSelection() {
		for (int i = 0; i < parts.length; i ++) {
			if (((StackLayout)stackPanel.getLayout()).topControl == parts[i]) return i;
		}
		return -1;
	}
	
	public void setContent(Composite[] parts, Composite stackPanel){
		this.parts = parts;
		this.stackPanel = stackPanel;
	}
	
	public void selectTab(int tab){
		Event evt = new Event();
		evt.widget = headers[tab];
		handleEvent(evt);
	}


	@Override
	public void handleEvent(Event event) {
		for (CLabel link : headers){
			if (link == event.widget){
				((StackLayout)stackPanel.getLayout()).topControl = parts[(int)link.getData()];
				selectTabItem(link);
				fireListeners();
				break;
			}
		}
		layout(true);
		stackPanel.layout();
	}
	
	private void selectTabItem(CLabel link) {
		for (CLabel l : headers) WidgetElement.setCSSClass(l, TAB_CLASS);
		WidgetElement.setCSSClass(link, TAB_SELECTED_CLASS);
		WidgetElement.applyStyles(this, true);	
	}
}

