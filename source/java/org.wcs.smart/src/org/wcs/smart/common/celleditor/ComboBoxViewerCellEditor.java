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
package org.wcs.smart.common.celleditor;

import java.lang.reflect.Method;
import java.text.MessageFormat;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
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

/**
 * A modified copy of the org.eclipse.jface.viewers.ComboVoxViewerCellEditor that
 * displays the combo box immediately on editor selection.
 * 
 * @author Emily
 *
 */
public class ComboBoxViewerCellEditor extends CellEditor {

	/**
	 * The custom combo box control.
	 */
	protected ComboViewer viewer;

	protected Object selectedValue;

	/**
	 * The list is dropped down when the activation is done through the mouse
	 */
	public static final int DROP_DOWN_ON_MOUSE_ACTIVATION = 1;

	/**
	 * The list is dropped down when the activation is done through the keyboard
	 */
	public static final int DROP_DOWN_ON_KEY_ACTIVATION = 1 << 1;

	/**
	 * The list is dropped down when the activation is done without
	 * ui-interaction
	 */
	public static final int DROP_DOWN_ON_PROGRAMMATIC_ACTIVATION = 1 << 2;

	/**
	 * The list is dropped down when the activation is done by traversing from
	 * cell to cell
	 */
	public static final int DROP_DOWN_ON_TRAVERSE_ACTIVATION = 1 << 3;

	private int activationStyle = SWT.NONE;



	/**
	 * Default ComboBoxCellEditor style
	 */
	private static final int defaultStyle = SWT.NONE;

	/**
	 * Creates a new cell editor with a combo viewer and a default style
	 *
	 * @param parent
	 *            the parent control
	 */
	public ComboBoxViewerCellEditor(Composite parent) {
		this(parent, defaultStyle);
	}

	/**
	 * Creates a new cell editor with a combo viewer and the given style
	 *
	 * @param parent
	 *            the parent control
	 * @param style
	 *            the style bits
	 */
	public ComboBoxViewerCellEditor(Composite parent, int style) {
		super(parent, style);
		setValueValid(true);
	}

	@Override
	public void activate(ColumnViewerEditorActivationEvent activationEvent) {
		super.activate(activationEvent);
		if (activationStyle != SWT.NONE) {
			boolean dropDown = false;
			if ((activationEvent.eventType == ColumnViewerEditorActivationEvent.MOUSE_CLICK_SELECTION || activationEvent.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION)
					&& (activationStyle & DROP_DOWN_ON_MOUSE_ACTIVATION) != 0 ) {
				dropDown = true;
			} else if (activationEvent.eventType == ColumnViewerEditorActivationEvent.KEY_PRESSED
					&& (activationStyle & DROP_DOWN_ON_KEY_ACTIVATION) != 0 ) {
				dropDown = true;
			} else if (activationEvent.eventType == ColumnViewerEditorActivationEvent.PROGRAMMATIC
					&& (activationStyle & DROP_DOWN_ON_PROGRAMMATIC_ACTIVATION) != 0) {
				dropDown = true;
			} else if (activationEvent.eventType == ColumnViewerEditorActivationEvent.TRAVERSAL
					&& (activationStyle & DROP_DOWN_ON_TRAVERSE_ACTIVATION) != 0) {
				dropDown = true;
			}

			if (dropDown) {
				getControl().getDisplay().asyncExec(new Runnable() {

					@Override
					public void run() {
						((CCombo) getControl()).setListVisible(true);
					}

				});

			}
		}
	}

	/**
	 * This method allows to control how the combo reacts when activated
	 *
	 * @param activationStyle
	 *            the style used
	 */
	public void setActivationStyle(int activationStyle) {
		this.activationStyle = activationStyle;
	}

	
	@Override
	protected Control createControl(Composite parent) {

		CCombo comboBox = new CCombo(parent, getStyle());
		comboBox.setFont(parent.getFont());
		viewer = new ComboViewer(comboBox);

		comboBox.addKeyListener(new KeyAdapter() {
			// hook key pressed - see PR 14201
			@Override
			public void keyPressed(KeyEvent e) {
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
				ISelection selection = viewer.getSelection();
				if (selection.isEmpty()) {
					selectedValue = null;
				} else {
					selectedValue = ((IStructuredSelection) selection)
							.getFirstElement();
				}
				applyEditorValueAndDeactivate();
			}
		});

		comboBox.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE
						|| e.detail == SWT.TRAVERSE_RETURN) {
					e.doit = false;
				}
			}
		});

		comboBox.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				ComboBoxViewerCellEditor.this.focusLost();
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
	@Override
	protected Object doGetValue() {
		return selectedValue;
	}

	@Override
	protected void doSetFocus() {
		viewer.getControl().setFocus();
		
		try{
			CCombo comboBox = (CCombo) getControl();				
			Method method = CCombo.class.getDeclaredMethod("dropDown", boolean.class); //$NON-NLS-1$
			method.setAccessible(true);
			method.invoke(comboBox, true);
		}catch (Exception ex){
			ex.printStackTrace();
		}
	}

	public void recomputeSize(){
		try{
			CCombo comboBox = (CCombo) getControl();				
			Method method = CCombo.class.getDeclaredMethod("dropDown", boolean.class); //$NON-NLS-1$
			method.setAccessible(true);
			method.invoke(comboBox, false);
			method.invoke(comboBox, true);
		}catch (Exception ex){
			ex.printStackTrace();
		}
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
		if ((viewer.getControl() == null) || viewer.getControl().isDisposed()) {
			layoutData.minimumWidth = 60;
		} else {
			// make the comboBox 10 characters wide
			GC gc = new GC(viewer.getControl());
			layoutData.minimumWidth = (gc.getFontMetrics()
					.getAverageCharWidth() * 10) + 10;
			gc.dispose();
		}
		return layoutData;
	}

	/**
	 * Set a new value
	 *
	 * @param value
	 *            the new value
	 */
	@Override
	protected void doSetValue(Object value) {
	    Assert.isTrue(viewer != null);
	    selectedValue = value;
	    if (value == null) {
	        viewer.setSelection(StructuredSelection.EMPTY);
	    } else {
	        viewer.setSelection(new StructuredSelection(value));
	    }
	}

	/**
	 * @param labelProvider
	 *            the label provider used
	 * @see StructuredViewer#setLabelProvider(IBaseLabelProvider)
	 */
	public void setLabelProvider(IBaseLabelProvider labelProvider) {
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
	 * @param provider
	 *            the content provider used
	 * @see StructuredViewer#setContentProvider(IContentProvider)
	 * @deprecated As of 3.7, replaced by
	 *             {@link #setContentProvider(IStructuredContentProvider)}
	 */
	@Deprecated
	public void setContenProvider(IStructuredContentProvider provider) {
		viewer.setContentProvider(provider);
	}

	/**
	 * @param input
	 *            the input used
	 * @see StructuredViewer#setInput(Object)
	 */
	public void setInput(Object input) {
		viewer.setInput(input);
	}

	/**
	 * @return get the viewer
	 */
	public ComboViewer getViewer() {
		return viewer;
	}

	/**
	 * Applies the currently selected value and deactiavates the cell editor
	 */
	void applyEditorValueAndDeactivate() {
		// must set the selection before getting value
		ISelection selection = viewer.getSelection();
		if (selection.isEmpty()) {
			selectedValue = null;
		} else {
			selectedValue = ((IStructuredSelection) selection)
					.getFirstElement();
		}

		Object newValue = doGetValue();
		markDirty();
		boolean isValid = isCorrect(newValue);
		setValueValid(isValid);

		if (!isValid) {
			MessageFormat.format(getErrorMessage(),
					new Object[] { selectedValue });
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
