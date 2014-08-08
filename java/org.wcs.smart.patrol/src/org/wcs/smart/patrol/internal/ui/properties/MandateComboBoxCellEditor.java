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
package org.wcs.smart.patrol.internal.ui.properties;

import java.text.MessageFormat;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.ui.properties.LanguageViewer;

/**
 * Adapted from ComboBoxCellEditor to display patrol mandates
 * and use patrol mandate objects instead of text strings
 * 
 * @author Emily
 * @since 1.0.0
 */
public class MandateComboBoxCellEditor extends CellEditor {

	/**
	 * The list of items to present in the combo box.
	 */
	private PatrolMandate[] values;

	private LanguageViewer langViewer;
	/**
	 * The zero-based index of the selected item.
	 */
	int selection;

	/**
	 * The custom combo box control.
	 */
	CCombo comboBox;


	/**
	 * Creates a new cell editor with a combo containing the given list of
	 * choices and parented under the given control. The cell editor value is
	 * the zero-based index of the selected item. Initially, the cell editor has
	 * no cell validator and the first item in the list is selected.
	 *
	 * @param parent
	 *            the parent control
	 * @param items
	 *            the list of strings for the combo box
	 * @param style
	 *            the style bits
	 * @since 2.1
	 */
	public MandateComboBoxCellEditor(Composite parent, PatrolMandate[] items, int style, LanguageViewer langViewer) {
		super(parent, style);
		this.langViewer = langViewer;
		setItems(items);
	}



	/**
	 * Returns the list of choices for the combo box
	 *
	 * @return the list of choices for the combo box
	 */
	public PatrolMandate[] getItems() {
		return this.values;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.CellEditor#activate(org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent)
	 */
	public void activate(ColumnViewerEditorActivationEvent activationEvent) {
		super.activate(activationEvent);
		getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				((CCombo) getControl()).setListVisible(true);
			}

		});

	}
	
	/**
	 * Sets the list of choices for the combo box
	 *
	 * @param items
	 *            the list of choices for the combo box
	 */
	public void setItems(PatrolMandate[] items) {
		Assert.isNotNull(items);
		this.values = items;
		populateComboBoxItems();
	}

	//see https://bugs.eclipse.org/bugs/show_bug.cgi?id=230398#c2
	private int lastArrow = -1;
	/*
	 * (non-Javadoc) Method declared on CellEditor.
	 */
	protected Control createControl(Composite parent) {

		comboBox = new CCombo(parent, getStyle());
		comboBox.setFont(parent.getFont());

		populateComboBoxItems();

		comboBox.addKeyListener(new KeyAdapter() {
			// hook key pressed - see PR 14201
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_UP || e.keyCode== SWT.ARROW_DOWN){
					lastArrow = e.time;
				}
				keyReleaseOccured(e);
			}
		});

		comboBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent event) {
				applyEditorValueAndDeactivate();
			}

			@Override
			public void widgetSelected(SelectionEvent event) {
				if (lastArrow != event.time){
					applyEditorValueAndDeactivate();
				}
				
			}
		});

		comboBox.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE
						|| e.detail == SWT.TRAVERSE_RETURN) {
					e.doit = false;
				}
			}
		});
		comboBox.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent e) {
				MandateComboBoxCellEditor.this.focusLost();
			}
		});
		return comboBox;
	}

	/**
	 * The <code>ComboBoxCellEditor</code> implementation of this
	 * <code>CellEditor</code> framework method returns the zero-based index
	 * of the current selection.
	 *
	 * @return the zero-based index of the current selection wrapped as an
	 *         <code>Integer</code>
	 */
	protected Object doGetValue() {
		return values[selection];
	}

	/*
	 * (non-Javadoc) Method declared on CellEditor.
	 */
	protected void doSetFocus() {
		comboBox.setFocus();
	}

	/**
	 * The <code>ComboBoxCellEditor</code> implementation of this
	 * <code>CellEditor</code> framework method sets the minimum width of the
	 * cell. The minimum width is 10 characters if <code>comboBox</code> is
	 * not <code>null</code> or <code>disposed</code> else it is 60 pixels
	 * to make sure the arrow button and some text is visible. The list of
	 * CCombo will be wide enough to show its longest item.
	 */
	public LayoutData getLayoutData() {
		LayoutData layoutData = super.getLayoutData();
		if ((comboBox == null) || comboBox.isDisposed()) {
			layoutData.minimumWidth = 60;
		} else {
			// make the comboBox 10 characters wide
			GC gc = new GC(comboBox);
			layoutData.minimumWidth = (gc.getFontMetrics()
					.getAverageCharWidth() * 10) + 10;
			gc.dispose();
		}
		return layoutData;
	}

	/**
	 * The <code>ComboBoxCellEditor</code> implementation of this
	 * <code>CellEditor</code> framework method accepts a zero-based index of
	 * a selection.
	 *
	 * @param value
	 *            the zero-based index of the selection wrapped as an
	 *            <code>Integer</code>
	 */
	protected void doSetValue(Object value) {
		Assert.isTrue(comboBox != null && (value == null || value instanceof PatrolMandate));
		
		
		for (int i = 0; i < values.length; i ++){
			if ((values[i] == null && value == null) || (values[i] != null && value != null && values[i].equals((PatrolMandate)value))){
				selection = i;
				break;
			}
		}
		
		comboBox.select(selection);
	}

	/**
	 * Updates the list of choices for the combo box for the current control.
	 */
	private void populateComboBoxItems() {
		if (comboBox != null && values != null) {
			comboBox.removeAll();
			for (int i = 0; i < values.length; i++) {
				if (values[i] == null){
					comboBox.add(""); //$NON-NLS-1$
				}else{
					comboBox.add(values[i].findName(langViewer.getCurrentSelection()), i);
				}
			}

			setValueValid(true);
			selection = 0;
		}
	}

	/**
	 * Applies the currently selected value and deactivates the cell editor
	 */
	void applyEditorValueAndDeactivate() {
		// must set the selection before getting value
		selection = comboBox.getSelectionIndex();
		Object newValue = doGetValue();
		markDirty();
		boolean isValid = isCorrect(newValue);
		setValueValid(isValid);

		if (!isValid) {
			// Only format if the 'index' is valid
			if (values.length > 0 && selection >= 0 && selection < values.length) {
				// try to insert the current value into the error message.
				setErrorMessage(MessageFormat.format(getErrorMessage(),
						new Object[] { values[selection] }));
			} else {
				// Since we don't have a valid index, assume we're using an
				// 'edit'
				// combo so format using its text value
				setErrorMessage(MessageFormat.format(getErrorMessage(),
						new Object[] { comboBox.getText() }));
			}
		}

		fireApplyEditorValue();
		deactivate();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.CellEditor#focusLost()
	 */
	protected void focusLost() {
		if (isActivated()) {
			applyEditorValueAndDeactivate();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.CellEditor#keyReleaseOccured(org.eclipse.swt.events.KeyEvent)
	 */
	protected void keyReleaseOccured(KeyEvent keyEvent) {
		if (keyEvent.character == '\u001b') { // Escape character
			fireCancelEditor();
		} else if (keyEvent.character == '\t') { // tab key
			applyEditorValueAndDeactivate();
		}
	}
}
