/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
import java.util.Collection;
import java.util.Collections;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Drop down check box viewer cell editor
 * 
 * @author Emily
 *
 */
public class CheckBoxDropDownCellEditor extends CellEditor {

	/**
	 * The custom combo box control.
	 */
	protected CheckBoxDropDown viewer;

	protected Collection<Object> selectedValues = new ArrayList<>();

	
	/**
	 * Creates a new cell editor with a combo viewer and a default style
	 *
	 * @param parent
	 *            the parent control
	 */
	public CheckBoxDropDownCellEditor(Composite parent) {
		this(parent, SWT.NONE);
	}

	/**
	 * Creates a new cell editor with a combo viewer and the given style
	 *
	 * @param parent
	 *            the parent control
	 * @param style
	 *            the style bits
	 */
	public CheckBoxDropDownCellEditor(Composite parent, int style) {
		super(parent, style);
		setValueValid(true);
	}

	@Override
	public void activate(ColumnViewerEditorActivationEvent activationEvent) {
		super.activate(activationEvent);

		getControl().getDisplay().asyncExec(new Runnable() {
				@Override
			public void run() {
				viewer.dropDown(true);
			}
		});
	}
	
	@Override
	protected Control createControl(Composite parent) {
		viewer = new CheckBoxDropDown(parent) {
			@Override
			protected void cancelEdit() {
				fireCancelEditor();
			}
		};
		viewer.setFont(parent.getFont());
		
		viewer.addKeyListener(new KeyAdapter() {
			// hook key pressed - see PR 14201
			@Override
			public void keyPressed(KeyEvent e) {
				keyReleaseOccured(e);
			}
		});

		viewer.addSelectionChangedListener(e->{
			applyEditorValueAndDeactivate();
		});

		return viewer;
	}

	/**
	 * 
	 *
	 * @return a list of selected values
	 */
	@Override
	protected Object doGetValue() {
		return selectedValues;
	}

	@Override
	protected void doSetFocus() {
		viewer.setFocus();
	}

	/**
	 * The <code>ComboBoxCellEditor</code> implementation of this
	 * <code>CellEditor</code> framework method sets the minimum width of the
	 * cell. The minimum width is 10 characters if <code>comboBox</code> is
	 * not <code>null</code> or <code>disposed</code> eles it is 60 pixels
	 * to make sure the arrow button and some text is visible. The list of
	 * CCombo will be wide enough to show its longest item.
	 */
	@Override
	public LayoutData getLayoutData() {
		LayoutData layoutData = super.getLayoutData();
		if ((viewer == null) || viewer.isDisposed()) {
			layoutData.minimumWidth = 60;
		} else {
			// make the comboBox 10 characters wide
			GC gc = new GC(viewer);
			layoutData.minimumWidth = (gc.getFontMetrics()
					.getAverageCharWidth() * 10) + 10;
			gc.dispose();
		}
		layoutData.verticalAlignment = SWT.CENTER;
		layoutData.minimumHeight = getControl().computeSize(SWT.DEFAULT, SWT.DEFAULT,true).y;
		return layoutData;
	}

	/**
	 * Set a new value; must be a collection of objects
	 *
	 * @param value
	 *            the new value
	 */
	@Override
	protected void doSetValue(Object value) {
	    Assert.isTrue(viewer != null);
	    if (value instanceof Collection<?>) {
	    	selectedValues = (Collection<Object>)value;
	    	viewer.setValue(selectedValues);
	    }else {
	    	selectedValues.clear();
	    	viewer.setValue(Collections.emptySet());
	    }
	}

	/**
	 * @param labelProvider
	 *            the label provider used
	 * @see StructuredViewer#setLabelProvider(IBaseLabelProvider)
	 */
	public void setLabelProvider(ILabelProvider labelProvider) {
		viewer.setLabelProvider(labelProvider);
	}

	/**
	 * @param provider
	 *            the content provider used
	 * @see StructuredViewer#setContentProvider(IContentProvider)
	 * @since 3.7
	 */
	public void setContentProvider(IStructuredContentProvider provider) {
		viewer.setContentProvider(provider);
	}


	/**
	 * @param input
	 *            the input used
	 * @see StructuredViewer#setInput(Object)
	 */
	public void setInput(Collection<?> input) {
		viewer.setInput(input);
	}

	/**
	 * @return get the viewer
	 */
	public CheckBoxDropDown getViewer() {
		return viewer;
	}

	/**
	 * Applies the currently selected value and deactiavates the cell editor
	 */
	void applyEditorValueAndDeactivate() {
		// must set the selection before getting value
		selectedValues = new ArrayList<>();
		for (Object x : viewer.getCheckedElements()) {
			selectedValues.add(x);
		}
		
		Object newValue = doGetValue();
		markDirty();
		boolean isValid = isCorrect(newValue);
		setValueValid(isValid);

		if (!isValid) {
			//TODO
//			MessageFormat.format(getErrorMessage(),
//					new Object[] { selectedValue });
		}

		fireApplyEditorValue();
		deactivate();
	}

	@Override
	protected void focusLost() {
		if (isActivated()) {
			applyEditorValueAndDeactivate();
		}
	}

	@Override
	protected void keyReleaseOccured(KeyEvent keyEvent) {
		if (keyEvent.character == '\u001b') { // Escape character
			fireCancelEditor();
		} else if (keyEvent.character == '\t') { // tab key
			applyEditorValueAndDeactivate();
		}
	}
}
