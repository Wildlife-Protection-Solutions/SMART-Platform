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
package org.wcs.smart.common.control;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.SmartPlugIn;

/**
 * Resizeable text box for editing text attributes
 * 
 * @author Emily
 *
 */
public class MultiLineText extends Composite implements Listener{

	private Text text;
	private Integer minSize;
	private Label move;
	private Integer lastY = null;
	private Integer lastHeight = null;
	
	private static final int offset = 4;
	
	public MultiLineText(Composite parent) {
		super(parent, SWT.BORDER | SWT.FLAT);

//		Image x = new Image(getDisplay(),"C:\\data\\SMART\\Source\\trunk\\source\\java\\org.wcs.smart\\images\\icons\\resize.png");
		move = new Label(this, SWT.NONE);
		move.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.RESIZE_IMAGE));
		int imagewidth = move.getImage().getBounds().width;
		move.setBounds(super.getBounds().width - imagewidth - offset, super.getBounds().height - imagewidth - offset, imagewidth, imagewidth);
		
		move.addListener(SWT.MouseMove, this);
		move.addListener(SWT.MouseDown, this);
		move.addListener(SWT.MouseUp, this);
		move.addListener(SWT.MouseEnter, this);
		move.addListener(SWT.MouseExit, this);
		
		
		text = new Text(this, SWT.MULTI | SWT.WRAP  | SWT.V_SCROLL );
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		//auto hide scroll bar and cascade modify event
		Listener scrollBarListener = new Listener (){
		    @Override
		    public void handleEvent(Event event) {
		        Text t = (Text)event.widget;
		        Rectangle r1 = t.getClientArea();
		        // use r1.x as wHint instead of SWT.DEFAULT
		        Rectangle r2 = t.computeTrim(r1.x, r1.y, r1.width, r1.height); 
		        Point p = t.computeSize(r1.x,  SWT.DEFAULT,  true); 
		        t.getVerticalBar().setVisible(r2.height <= p.y);
		        if (event.type == SWT.Modify){
		           t.getParent().layout(true);
		           t.showSelection();
		           
		           MultiLineText.this.notifyListeners(SWT.Modify, event);
		        }
		    }};
		    
		text.addListener(SWT.Resize, scrollBarListener);
		text.addListener(SWT.Modify, scrollBarListener);
		text.setBounds(0, 0, super.getBounds().width - offset, super.getBounds().height - offset);
		
		
		
		addListener(SWT.Resize, e->{
			text.setBounds(0, 0, super.getBounds().width - offset, super.getBounds().height - offset);
			move.setBounds(super.getBounds().width - imagewidth - offset, super.getBounds().height - imagewidth - offset, imagewidth, imagewidth);

		});
		
		minSize = text.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		Rectangle r = super.getBounds();
		r.height = minSize + offset + 2;
		super.setBounds(r);
	}
	
	@Override
	public void handleEvent(Event event) {
		if (event.type == SWT.MouseEnter) {
			getShell().setCursor(getDisplay().getSystemCursor(SWT.CURSOR_SIZENS));
		}else if (event.type == SWT.MouseExit) {
			getShell().setCursor(null);
		}else if (event.type == SWT.MouseDown) {
			lastY = move.toDisplay(event.x, event.y).y;
			lastHeight = getBounds().height;
		}else if (event.type == SWT.MouseUp) {
			lastY = null;
		}else if (event.type == SWT.MouseMove) {
			if (lastY == null) return;
			int newy = move.toDisplay(event.x, event.y).y;
			int offset = newy - lastY;
			lastY = newy;
			int newHeight = lastHeight + offset;
			Rectangle r = getBounds();
			if (newHeight < minSize) {
				r.height = minSize;
			}else {
				r.height = newHeight;	
			}
			setBounds(r);
			lastHeight = r.height;
			getParent().layout(true, true);
			layout(true);	
		}
	}
	
	
	/**
	 * 
	 * @return the text 
	 */
	public String getText() {
		return this.text.getText();
	}
	
	/**
	 * Initializes the text box and the initial size
	 * of the component.  If the text contains line feed character
	 * or more than 150 characters long the default size will be enough
	 * for four lines, otherwise the default size will be a single
	 * line.
	 * 
	 * @param text
	 */
	public void setText(String text) {
		this.text.setText(text);
		if (text.contains("\n") || text.length() > 75) { //$NON-NLS-1$

			Rectangle r = super.getBounds();
			r.height = minSize * 4 + offset + 2;
			super.setBounds(r);
		}
	}
	
	/*
	public static void main(String[] args) {
		Display display = new Display ();
		Shell shell = new Shell (display);
		shell.setLayout(new GridLayout());
		Text text = new Text(shell, SWT.BORDER);
		text.setText("abc");
		MultiLineText txt = new MultiLineText(shell);
		txt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		shell.pack();
		shell.open();
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		display.dispose ();
	}
	*/
}
