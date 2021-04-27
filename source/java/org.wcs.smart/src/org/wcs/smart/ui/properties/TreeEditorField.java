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
package org.wcs.smart.ui.properties;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

/**
 * An field editor that displays the text box that is used to filter
 * a drop down tree. Users can type and select from the list or use a drop down arrow and
 * pick from the tree.
 *  
 *  Users must dispose of the editor when they are finished with it.
 *  
 * @author Emily
 *
 */
public class TreeEditorField<T>  {

	
	private T originalValue;
	private T lastValidSelection = null; //last valid selection; tracked to escape can revert
	
	protected TreeDropDown tree = null;
	protected ControlDecoration cd;
	private Composite dropDownComposite;
	protected Text txtText;
	private Button btnDownArrow;
	private Display focusDisplay = null;
	private Collection<Listener> listeners = null; 
		
	private Listener focusListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			if (event.widget.isDisposed()){
				return;
			}
			if (!(event.widget == btnDownArrow || 
				  event.widget == txtText || 
				  event.widget == tree.getTreeViewer().getControl() || 
				  event.widget == tree.getTreeViewer().getControl().getParent())){
				if (tree.isVisible()){
					tree.hide();
				}
			}
		}
	};
	
	/**
	 * Creates a new tree drop down field
	 * 
	 */
	public TreeEditorField(){
		listeners = new ArrayList<Listener>();
	}
	
	/**
	 * Disables, enables the control
	 * 
	 * @param enabled
	 */
	public void setEnabled(boolean enabled) {
		if (txtText != null) txtText.setEnabled(enabled);
		if (btnDownArrow != null) btnDownArrow.setEnabled(enabled);
		if (tree != null) tree.getTreeViewer().getControl().setEnabled(enabled);
	}
	/**
	 * Gets the value selected from the tree
	 */
	public T getValue() {
		return  (T)txtText.getData();
	}


	/**
	 * Adds a selection changed event
	 * @param listener
	 */
	public void addSelectionChangedListener(Listener listener){
		listeners.add(listener);
	}
	
	/*
	 * Updates the ui text field with the given selection.
	 */
	private void updateSelection(T selection){
		if (selection == null){
			txtText.setText(""); //$NON-NLS-1$
			txtText.setData(null);
		}else{
			txtText.setText(((LabelProvider)tree.getTreeViewer().getLabelProvider()).getText(selection));
			txtText.setData(selection);
			lastValidSelection = selection;
		}
		txtText.selectAll();
		validate();
		
		//fire events
		Event event = new Event();
		for (Listener listener : listeners){
			listener.handleEvent(event);
		}
	}
	
	/**
	 * Gets the drop down tree that is used.  Use this to change or update
	 * the content provider of label providers. 
	 * @return
	 */
	public TreeDropDown getDropDown() {
		return this.tree;
	}
	
	/**
	 * Creates the control.
	 * @param parent the parent control
	 * @param contentProvider the tree content provider
	 * @param labelProvider the label provider for the tree
	 */
	public void createComposite(Composite parent, IContentProvider contentProvider, IBaseLabelProvider labelProvider) {
		focusDisplay = parent.getShell().getDisplay();
		focusDisplay.addFilter(SWT.FocusIn, focusListener);
		
		dropDownComposite = new Composite(parent,  SWT.BORDER );
		dropDownComposite.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = 
				layout.marginHeight = 
				layout.marginLeft = 
				layout.marginRight = 
				layout.marginTop = 
				layout.marginBottom = 
				layout.horizontalSpacing = 
				layout.verticalSpacing = 0;
		dropDownComposite.setLayout(layout);
		dropDownComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)dropDownComposite.getLayoutData()).horizontalIndent = 5;
		((GridData)dropDownComposite.getLayoutData()).widthHint = 50;

		txtText = new Text(dropDownComposite, SWT.NONE);
		txtText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		tree = new TreeDropDown(parent.getShell()){
			@Override
			public void hide(){
				tree.setText(""); //$NON-NLS-1$
				updateSelection(lastValidSelection);
				super.hide();
			}
		};
		tree.getTreeViewer().setContentProvider(contentProvider);
		tree.getTreeViewer().setLabelProvider(labelProvider);
		
		tree.getTreeViewer().expandToLevel(2);
		tree.setFilterTextBox(txtText);

		btnDownArrow = new Button(dropDownComposite, SWT.ARROW | SWT.DOWN);
		btnDownArrow.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showTree(true);
			}
		});
		
		//added this so the drop down show when click on field
		//this makes it more similar to list drop down behaviour
		txtText.addListener(SWT.MouseUp, e->{
			//if visible but text is selected ignore
			if (tree.isVisible() && txtText.getSelectionCount() > 0) return;
			showTree(false);
		});
		
		txtText.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				validate();
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				Display.getDefault().asyncExec(new Runnable(){
					@Override
					public void run() {
						if (txtText.isDisposed()) return;
						txtText.selectAll();
					}
				});
			}
		});
		
		txtText.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if (e.keyCode == SWT.CR ){
					if (tree.isVisible()){
						//update the selection and hide the tree
						if (!tree.getSelection().isEmpty()){
							T sel = (T) tree.getSelection().iterator().next();
							updateSelection(sel);
							tree.hide();
						}
					}
					e.doit = false;
				}else if (e.keyCode == SWT.ESC){
					//revert the selection and hid the tree
					updateSelection(lastValidSelection);
					tree.hide();
					e.doit = false;
				}else if (tree.isVisible()){
					//set focus to the tree
					tree.setFocus();
					e.doit = false;
				}
				
			}
		});
		
		txtText.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if (tree.treeItemsVisible() && e.keyCode == SWT.ARROW_DOWN) {
					//key down sets focus on tree
					tree.setFocus();
					return;
				}
			}
			
			public void keyReleased(KeyEvent e) {
				if (e.keyCode == SWT.ESC) return;
				if (e.keyCode != SWT.CR){
					//any key that is not cr and changes
					//the text field should display the tree
					txtText.setData(null);
					if (!tree.getText().equals(txtText.getText())){
						//text has changed
						tree.setText(txtText.getText());
						if (!tree.isVisible()){
							showTree(false);
						}
					}
				}
			}
		});
		
		cd = new ControlDecoration(txtText, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		
		
		validate();
		originalValue = null;
	}
	
	/**
	 * Sets the input for the tree viewer
	 * @param input
	 */
	public void setInput(Object input) {
		getDropDown().getTreeViewer().setInput(input);
	}
	
	/*
	 * shows the tree
	 */
	private void showTree(boolean focus){
		if (tree.isVisible()){
			tree.hide();
		}else{
			tree.positionAndShow(dropDownComposite, null, null, new ISelectionListener() {
				
				@Override
				public void selectionChanged(IWorkbenchPart part, ISelection selection) {
					if (selection == null){
						//should revert back to previous selection
						updateSelection(lastValidSelection);
					}else if (selection.isEmpty()){
						//nothing selected
						updateSelection(null);
					}else{
						//pick first selection
						Object x = ((IStructuredSelection)selection).getFirstElement();
						updateSelection((T) x);
					}
					tree.hide();
				}
			});
			if (focus){
				tree.setFocus();
			}
		}
	}
	
	/**
	 * Validate the selection.  Users should override if they want
	 * to provide validation.
	 * Users must update the state of the ControlDecoration in this method
	 * 
	 * @return null if item is valid otherwise a string that represents the error message
	 */
	public String validate() {
		cd.hide();
		return null;
	}


	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#clear()
	 */
	public void clear() {
		txtText.setText(""); //$NON-NLS-1$
		txtText.setData(null);
		originalValue = null;
		validate();
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#isModified()
	 */
	public boolean isModified() {
		T current = getValue();
		if (current == null && originalValue == null){
			return false;
		}else if (current != null && originalValue != null && current.equals(originalValue)){
			return false;
		}
		return true;
	}

	/**
	 */
	public void setSelectedValue(T x) {
		if (x != null) {
			txtText.setText(((LabelProvider)tree.getTreeViewer().getLabelProvider()).getText(x));
			txtText.setData(x);
			this.originalValue = (T) x;
		}else{
			txtText.setText(""); //$NON-NLS-1$
			txtText.setData(null);
			this.originalValue = null;
			
		}
		this.lastValidSelection = this.originalValue;
		validate();
		
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#setFocus()
	 */
	public void setFocus() {
		txtText.setFocus();
	}

	/**
	 * Disposes of controls and removes focus listeners
	 */
	public void dispose() {
		if (tree != null ){
			tree.dispose();
		}
		focusDisplay.removeFilter(SWT.FocusIn, focusListener);
	}

}