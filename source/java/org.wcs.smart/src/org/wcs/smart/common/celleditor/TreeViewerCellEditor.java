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

import java.text.MessageFormat;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.wcs.smart.ui.ca.datamodel.TreeDropDownViewer;

/**
 * Tree editor for editing table cells with drop down tree.
 * 
 * @author Emily
 *
 */
public class TreeViewerCellEditor extends CellEditor {

	/**
	 * The custom combo box control.
	 */
	protected TreeDropDownViewer treeViewer;

	protected Object selectedValue;

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
	public TreeViewerCellEditor(Composite parent) {
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
	public TreeViewerCellEditor(Composite parent, int style) {
		super(parent, style);
		setValueValid(true);
	}

	@Override
	public void activate(ColumnViewerEditorActivationEvent activationEvent) {
		super.activate(activationEvent);		
		treeViewer.clearSearchText();
	}

	@Override
	protected Control createControl(Composite parent) {
		treeViewer = new TreeDropDownViewer(parent);
		treeViewer.setSelectionListener(new ISelectionListener() {
			
			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection) {
				applyEditorValueAndDeactivate();
			}
		});
		treeViewer.getFilteredTree().getFilterControl().addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				TreeViewerCellEditor.this.focusLost();
			}
		});
		
		Tree tree = treeViewer.getTreeViewer().getTree();
		tree.setFont(parent.getFont());
		tree.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ESC){
					fireCancelEditor();
				}
			}
		});
		

		return treeViewer.getComposite();
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
		treeViewer.getTreeViewer().getControl().setFocus();
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
		if ((treeViewer.getTreeViewer().getControl() == null) || treeViewer.getTreeViewer().getControl().isDisposed()) {
			layoutData.minimumWidth = 60;
		} else {
			// make the comboBox 10 characters wide
			GC gc = new GC(treeViewer.getTreeViewer().getControl());
			layoutData.minimumWidth = (gc.getFontMetrics()
					.getAverageCharWidth() * 10) + 10;
			gc.dispose();
		}
		layoutData.minimumHeight = 200;
		layoutData.verticalAlignment = SWT.TOP;
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
	    Assert.isTrue(treeViewer != null);
	    selectedValue = value;
	    if (value == null) {
	    	treeViewer.getTreeViewer().setSelection(StructuredSelection.EMPTY);
	    } else {
	    	treeViewer.getTreeViewer().setSelection(new StructuredSelection(value));
	    }
	}

	/**
	 * @param labelProvider
	 *            the label provider used
	 * @see StructuredViewer#setLabelProvider(IBaseLabelProvider)
	 */
	public void setLabelProvider(IBaseLabelProvider labelProvider) {
		treeViewer.getTreeViewer().setLabelProvider(labelProvider);
	}

	/**
	 * @param provider
	 *            the content provider used
	 * @see StructuredViewer#setContentProvider(IContentProvider)
	 * @since 3.7
	 */
	public void setContentProvider(IStructuredContentProvider provider) {
		treeViewer.getTreeViewer().setContentProvider(provider);
	}

	/**
	 * @param input
	 *            the input used
	 * @see StructuredViewer#setInput(Object)
	 */
	public void setInput(Object input) {
		treeViewer.getTreeViewer().setInput(input);
	}

	/**
	 * @return get the viewer
	 */
	public TreeViewer getViewer() {
		return treeViewer.getTreeViewer();
	}

	/**
	 * Applies the currently selected value and deactiavates the cell editor
	 */
	void applyEditorValueAndDeactivate() {
		// must set the selection before getting value
		ISelection selection = treeViewer.getTreeViewer().getSelection();
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
		fireCancelEditor();
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
