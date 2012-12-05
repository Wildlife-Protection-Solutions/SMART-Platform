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
package org.wcs.smart.patrol.internal.ui.observation.field;

import java.text.MessageFormat;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.TreeDropDown;
import org.wcs.smart.patrol.model.AttributeValidator;
import org.wcs.smart.ui.properties.AttributeTreeContentProvider;
import org.wcs.smart.ui.properties.AttributeTreeLabelProvider;
import org.wcs.smart.util.SmartUtils;

/**
 * A attribute field for tree attributes.
 * <p>
 * This attribute is represented as a text box and a separate
 * drop-down tree object that is displayed when users start typing
 * in the text box.
 * </p>
 * 
 * @author egouge
 *
 */
public class TreeAttributeField implements IAttributeField<AttributeTreeNode> {

	private Attribute attribute;
	private AttributeTreeNode originalValue;
	private AttributeTreeNode lastValidSelection = null; //last valid selection; tracked to escape can revert
	
	private TreeDropDown tree = null;
	private ControlDecoration cd;
	private Composite dropDownComposite;
	private Text txtText;
	private Button btnDownArrow;
	
	private Listener focusListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			if (!(event.widget == btnDownArrow || event.widget == txtText || event.widget == tree.getTreeViewer().getControl() || event.widget == tree.getTreeViewer().getControl().getParent())){
				if (tree.isVisible()){
					tree.hide();
				}
			}
		}
	};
	
	/**
	 * Creates a new attribute tree field
	 * @param attribute
	 */
	public TreeAttributeField(Attribute attribute){
		this.attribute = attribute;
	}
	
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#getValue()
	 * @return the selected AttributeTreeNode or null if no valid node selected
	 */
	@Override
	public AttributeTreeNode getValue() {
		return (AttributeTreeNode) txtText.getData();
	}


	/*
	 * Updates the ui text field with the given selection.
	 */
	private void updateSelection(AttributeTreeNode selection){
		if (selection == null){
			txtText.setText(""); //$NON-NLS-1$
			txtText.setData(null);
		}else{
			txtText.setText(SmartUtils.formatStringForLabel(selection.getName()));
			txtText.setData(selection);
			lastValidSelection = selection;
		}
		txtText.selectAll();
		validate();
	}
	
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createComposite(Composite parent) {
		parent.getShell().getDisplay().addFilter(SWT.FocusIn, focusListener);
		
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(attribute.getName() + ":"); //$NON-NLS-1$
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
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

		txtText = new Text(dropDownComposite, SWT.NONE);
		txtText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		tree = new TreeDropDown(parent.getShell());
		tree.getTreeViewer().setContentProvider(new AttributeTreeContentProvider(true, false));
		tree.getTreeViewer().setLabelProvider(new AttributeTreeLabelProvider());
		tree.getTreeViewer().setInput(attribute);
		tree.getTreeViewer().expandToLevel(2);
		tree.setFilterTextBox(txtText);

		btnDownArrow = new Button(dropDownComposite, SWT.ARROW | SWT.DOWN);
		btnDownArrow.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showTree(true);
			}
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
					//TODO: Test on MAC
					public void run() {
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
							AttributeTreeNode sel = (AttributeTreeNode) tree.getSelection().iterator().next();
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
					if (!tree.getText().equals(txtText.getText())){
						//text has changed
						tree.setText(txtText.getText());
						txtText.setData(null);
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
	 * shows the tree
	 */
	private void showTree(boolean focus){
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
					if (x instanceof AttributeTreeNode) {
						updateSelection((AttributeTreeNode) x);
					} else {
						updateSelection(null);										
					}
				}
				tree.hide();
			}
		});
		if (focus){
			tree.setFocus();
		}
	}
	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#validate()
	 */
	@Override
	public String validate() {
		String error = null;
		if (txtText.getText().length() > 0 && txtText.getData() == null){
			error = MessageFormat.format(Messages.TreeAttributeField_InvalidTreeValue, new Object[]{ attribute.getName()});
		}else{
			error = AttributeValidator.validateAttribute(attribute, getValue());
		}
		cd.hide();
		if (error != null){
			cd.setDescriptionText(error);
			cd.show();
		}
		return error;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#getAttribute()
	 */
	@Override
	public Attribute getAttribute() {
		return this.attribute;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#clear()
	 */
	@Override
	public void clear() {
		txtText.setText(""); //$NON-NLS-1$
		txtText.setData(null);
		originalValue = null;
		validate();
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#isModified()
	 */
	@Override
	public boolean isModified() {
		AttributeTreeNode current = getValue();
		if (current == null && originalValue == null){
			return false;
		}else if (current != null && originalValue != null && current.equals(originalValue)){
			return false;
		}
		return true;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#setValue(java.lang.Object)
	 * @param x an <code>AttributeTreeNode</code> object or null if empty
	 */
	@Override
	public void setValue(Object x) {
		if (x instanceof AttributeTreeNode){
			txtText.setText(((AttributeTreeNode) x).getName());
			txtText.setData(x);
			this.originalValue = (AttributeTreeNode)x;
		}else{
			txtText.setText(""); //$NON-NLS-1$
			txtText.setData(null);
			this.originalValue = null;
			
		}
		this.lastValidSelection = this.originalValue;
	}

	/**
	 * @see org.wcs.smart.patrol.internal.ui.observation.field.IAttributeField#setFocus()
	 */
	@Override
	public void setFocus() {
		txtText.setFocus();
	}


	@Override
	public void dispose() {
		if (tree != null){
			tree.dispose();
		}
		txtText.getShell().getDisplay().removeFilter(SWT.FocusIn, focusListener);
	}

}
