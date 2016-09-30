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

public class DateCellEditor extends CellEditor {

	 /**
     * The date time control
     */
    protected DateTime dtControl;

    private SelectionAdapter modifyListener;

    /**
     * State information for updating action enablement
     */
    private boolean isSelection = false;

    private boolean isDeleteable = false;

    private boolean isSelectable = false;


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

//    /**
//     * Checks to see if the "deletable" state (can delete/
//     * nothing to delete) has changed and if so fire an
//     * enablement changed notification.
//     */
//    private void checkDeleteable() {
//        boolean oldIsDeleteable = isDeleteable;
//        isDeleteable = isDeleteEnabled();
//        if (oldIsDeleteable != isDeleteable) {
//            fireEnablementChanged(DELETE);
//        }
//    }
//
//    /**
//     * Checks to see if the "selectable" state (can select)
//     * has changed and if so fire an enablement changed notification.
//     */
//    private void checkSelectable() {
//        boolean oldIsSelectable = isSelectable;
//        isSelectable = isSelectAllEnabled();
//        if (oldIsSelectable != isSelectable) {
//            fireEnablementChanged(SELECT_ALL);
//        }
//    }
//
//    /**
//     * Checks to see if the selection state (selection /
//     * no selection) has changed and if so fire an
//     * enablement changed notification.
//     */
//    private void checkSelection() {
//        boolean oldIsSelection = isSelection;
//        isSelection = text.getSelectionCount() > 0;
//        if (oldIsSelection != isSelection) {
//            fireEnablementChanged(COPY);
//            fireEnablementChanged(CUT);
//        }
//    }

    @Override
	protected Control createControl(Composite parent) {
        dtControl = new DateTime(parent, getStyle());
//        dtControl.addSelectionListener(new SelectionAdapter() {
//            @Override
//			public void widgetDefaultSelected(SelectionEvent e) {
//                handleDefaultSelection(e);
//            }
//        });
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
//                checkSelection(); // see explanation below
//                checkDeleteable();
//                checkSelectable();
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
//        // We really want a selection listener but it is not supported so we
//        // use a key listener and a mouse listener to know when selection changes
//        // may have occurred
//        text.addMouseListener(new MouseAdapter() {
//            @Override
//			public void mouseUp(MouseEvent e) {
//                checkSelection();
//                checkDeleteable();
//                checkSelectable();
//            }
//        });
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
