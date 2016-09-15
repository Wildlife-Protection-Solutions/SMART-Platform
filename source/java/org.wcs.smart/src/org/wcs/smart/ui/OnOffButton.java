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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Boolean on/off slider style button.
 * 
 * @author Emily
 * @since 4.1.0
 *
 */
public class OnOffButton extends Canvas{

	protected String yesText = SmartLabelProvider.BOOLEAN_TRUE_LABEL;
	protected String noText = SmartLabelProvider.BOOLEAN_FALSE_LABEL;
	
	private boolean isEnabled;
	private boolean state;
	private boolean isMouseIn;
	private boolean hasFocus;
	
	private int gap = 8;
	private int hpadding = 12;
	private int vpadding = 6;
	
    private final List<SelectionListener> selectionlisteners = new ArrayList<SelectionListener>();
    
	public OnOffButton(Composite parent, int style) {
		super(parent, style | SWT.DOUBLE_BUFFERED);
		state = false;
		isMouseIn = false;
		
		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				paint(e);
			}
		});
		
		
		Listener listener = new Listener(){

			@Override
			public void handleEvent(Event event) {
				switch(event.type){
				case SWT.FocusIn:
					hasFocus=true;
					refresh();
					break;
				case SWT.FocusOut:
					hasFocus=false;
					refresh();
					break;
				case SWT.MouseUp:
					handleMouseUp(event);
					break;
				case SWT.MouseEnter:
					isMouseIn = true;
					refresh();
					break;
				case SWT.Resize:
					refresh();
					break;
				case SWT.MouseExit:
					isMouseIn = false;
					refresh();
					break;
				case SWT.MouseDown:
					break;
				case SWT.KeyDown:
					if (event.character == '\r' || event.character==' ') {
	                	updateState(!state, event);
	                }
					break;
				case SWT.Traverse:
					if (event.detail == SWT.TRAVERSE_RETURN){
						event.doit = false;
						return;
					}
					event.doit = true;
					break;
				}
				
			}
		};
		addListener(SWT.MouseUp, listener);
		addListener(SWT.MouseEnter, listener);
		addListener(SWT.MouseExit, listener);
		addListener(SWT.Resize, listener);
		addListener(SWT.FocusIn, listener);
		addListener(SWT.FocusOut, listener);
//		addListener(SWT.MouseDown, listener);
		addListener(SWT.KeyDown, listener);
		addListener(SWT.Traverse, listener);
	}
	
	public void addSelectionListener(SelectionListener listener){
		selectionlisteners.add(listener);
	}
	
	public void reomveSelectionListener(SelectionListener listener){
		selectionlisteners.remove(listener);
	}
	
	public void setEnabled(boolean enabled){
		this.isEnabled = enabled;
		refresh();
	}
	
	public boolean isEnabled(){
		return this.isEnabled;
	}
	
	private void updateState(boolean newState, Event e){
		if (!this.isEnabled) return;
		boolean doit = true;
		for (final SelectionListener listener : this.selectionlisteners) {
			final Event event = new Event();
			event.button = e.button;
	        event.display = this.getDisplay();
	        event.item = null;
	        event.widget = this;
	        event.data = null;
	        event.time = e.time;
	        event.x = e.x;
	        event.y = e.y;

	        final SelectionEvent selEvent = new SelectionEvent(event);
	        listener.widgetSelected(selEvent);
	        if (!selEvent.doit) {
	        	doit = false;
	        }
		}
		if (doit){
			state = newState;
			refresh();
		}
	}
	private void refresh(){
		redraw();
	}
	public void setText(String yesText, String noText){
		this.yesText = yesText;
		this.noText = noText;
	}

	public boolean getSelection(){
		return this.state;
	}

	/**
	 * Sets the state, does not fire events.
	 * @param state
	 */
	public void setSelection(boolean state){
		this.state = state;
		refresh();
	}
	private void handleMouseUp(Event event){
		if (event.x < 0 || event.y < 0 || event.x >  getSize().x || event.y > getSize().y) return;
		updateState(!state, event);
	}

	@Override
	public Point computeSize(final int wHint, final int hHint,
			final boolean changed) {
		this.checkWidget();

		GC gc = new GC(this);

		Point yes = gc.textExtent(yesText);
		Point no = gc.textExtent(noText);

		int x = Math.max(yes.x, no.x);
		int y = Math.max(yes.y, no.y);

		return new Point(x * 2 + hpadding * 2 + gap, y + vpadding);
	}
	
	/**
     * Paint the widget
     * 
     * @param event
     *            paint event
     */
    private void paint(final PaintEvent event) {
        final Rectangle rect = this.getClientArea();
        if (rect.width == 0 || rect.height == 0) {
            return;
        }
     
        GC gc = event.gc;
        gc.setAntialias(SWT.ON);

        
        //draw border
    	int middle = rect.width / 2;
    	int round = 5;
    	
    	
    	//fill background
    	Color backgroundColor = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
    	Color yesTextColor = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT);
    	Color noTextColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
    	
    	if (!state){
    		backgroundColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
    	}
    	if (!isEnabled){
    		backgroundColor = getDisplay().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND);
    		yesTextColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
    		noTextColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
    	}
    	
    	gc.setBackground(backgroundColor);
    	gc.fillRoundRectangle(0, 0, rect.width-1, rect.height-1, round, round);
    	
    	gc.setForeground(yesTextColor);
    	gc.drawText(yesText, hpadding/2, vpadding/2);
    	
    	gc.setForeground(noTextColor);
    	gc.drawText(noText, middle + hpadding/2 , vpadding/2);
    	    	
    	//slider
    	Color sliderColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW);
    	gc.setBackground(sliderColor);
    	gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BORDER));
    	if (state){
    		gc.fillRoundRectangle(middle, 0, middle-1, rect.height-1, round, round);
    		gc.drawRoundRectangle(middle, 0, middle-1, rect.height-1, round, round);
    	}else{
    		gc.fillRoundRectangle(0, 0, middle, rect.height-1, round, round);
    		gc.drawRoundRectangle(0, 0, middle, rect.height-1, round, round);
    	}
	        	
    	
    	//border
    	Color borderColor = getDisplay().getSystemColor(SWT.COLOR_WIDGET_BORDER);
    	if (!isEnabled){
    		borderColor = getDisplay().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND);
    	}
    	gc.setForeground(borderColor);
    	gc.drawRoundRectangle(0, 0, rect.width-1, rect.height-1, round, round);
    	
    	
    	if (isMouseIn){
    		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_LIGHT_SHADOW));
    		if (state){
        		gc.drawRoundRectangle(middle+1, 1, middle-3, rect.height-3, round, round);
        	}else{
        		gc.drawRoundRectangle(1, 1, middle-2, rect.height-3, round, round);
        	}
    	}
    	
    	if (hasFocus){
    		int offset = 3;
    		if (state){
    			gc.drawFocus(middle+offset, offset, middle - offset*2, rect.height-offset*2);
    		}else{
    			gc.drawFocus(offset, offset, middle - offset*2, rect.height-offset*2);
    		}
    	}

    }

}
