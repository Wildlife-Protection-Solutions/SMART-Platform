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

import java.text.MessageFormat;
import java.util.Date;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.wcs.smart.util.SmartUtils;

/**
 * Table cell editor for modifying date and time values.
 * 
 * @author Emily
 *
 */
public class DateCellEditor extends CellEditor {

	 /**
     * The date time control
     */
    protected DateTime dtControl;

    private SelectionAdapter modifyListener;


    /**
     * Creates a new date/time cell editor parented under the given control.
     *  
     * Initially, the cell editor has no cell validator.
     *
     * @param parent the parent control
     * @param style the style bits
     * @since 2.1
     */
    public DateCellEditor(Composite parent, int style) {
        super(parent, style);
    }


    @Override
	protected Control createControl(Composite parent) {
        dtControl = new DateTime(parent, getStyle());
        dtControl.addKeyListener(new KeyAdapter() {
            // hook key pressed - see PR 14201  
            @Override
			public void keyPressed(KeyEvent e) {
                keyReleaseOccured(e);
                // as a result of processing the above call, clients may have
                // disposed this cell editor
                if ((getControl() == null) || getControl().isDisposed()) {
					return;
				}
            }
        });
        dtControl.addTraverseListener(new TraverseListener() {
            @Override
			public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_ESCAPE
                        || e.detail == SWT.TRAVERSE_RETURN) {
                    e.doit = false;
                }
            }
        });
        
        dtControl.addFocusListener(new FocusAdapter() {
            @Override
			public void focusLost(FocusEvent e) {
                DateCellEditor.this.focusLost();
            }
        });
        dtControl.setFont(parent.getFont());
        dtControl.setBackground(parent.getBackground());
        SmartUtils.initDateDateTimeWidget(dtControl, new Date());
        dtControl.addSelectionListener(getSelectionListener());
        return dtControl;
    }

    /**
     * The <code>TextCellEditor</code> implementation of
     * this <code>CellEditor</code> framework method returns
     * the text string.
     *
     * @return the text string
     */
    @Override
	protected Object doGetValue() {
    	if ((getStyle() & SWT.DATE) == SWT.DATE){
    		return SmartUtils.getDate(dtControl);
    	}else if ((getStyle() & SWT.TIME) == SWT.TIME){
    		return SmartUtils.getTime(dtControl);
    	}
    	return null;
    }

    @Override
	protected void doSetFocus() {
        if (dtControl != null) {
        	dtControl .setFocus();
        }
    }

    /**
     * The <code>TextCellEditor</code> implementation of
     * this <code>CellEditor</code> framework method accepts
     * a text string (type <code>String</code>).
     *
     * @param value a text string (type <code>String</code>)
     */
    @Override
	protected void doSetValue(Object value) {
        Assert.isTrue(dtControl != null && (value instanceof Date));
        dtControl.removeSelectionListener(getSelectionListener());
        if ((getStyle() & SWT.DATE) == SWT.DATE){
        	SmartUtils.initDateDateTimeWidget(dtControl, ((Date)value));
        }else if ((getStyle() & SWT.TIME) == SWT.TIME){
        	SmartUtils.initTimeDateTimeWidget(dtControl, ((Date)value));
        }
        dtControl.addSelectionListener(getSelectionListener());
    }

    /**
     * Processes a modify event that occurred in this text cell editor.
     * This framework method performs validation and sets the error message
     * accordingly, and then reports a change via <code>fireEditorValueChanged</code>.
     * Subclasses should call this method at appropriate times. Subclasses
     * may extend or reimplement.
     *
     * @param e the SWT modify event
     */
    protected void editOccured(SelectionEvent e) {
        Date value = SmartUtils.getDate(dtControl);
        if (value == null) {
			value = new Date();//$NON-NLS-1$
		}
        Object typedValue = value;
        boolean oldValidState = isValueValid();
        boolean newValidState = isCorrect(typedValue);
        if (!newValidState) {
            // try to insert the current value into the error message.
            setErrorMessage(MessageFormat.format(getErrorMessage(),
                    new Object[] { value }));
        }
        valueChanged(oldValidState, newValidState);
    }

    /**
     * Since a text editor field is scrollable we don't
     * set a minimumSize.
     */
    @Override
	public LayoutData getLayoutData() {
        LayoutData data = new LayoutData();
        data.minimumWidth= 0;
        return data;
    }

    /**
     * Return the modify listener.
     */
    private SelectionListener getSelectionListener() {
        if (modifyListener == null) {
            modifyListener = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					editOccured(e);
				}
            };
        }
        return modifyListener;
    }
   
}
