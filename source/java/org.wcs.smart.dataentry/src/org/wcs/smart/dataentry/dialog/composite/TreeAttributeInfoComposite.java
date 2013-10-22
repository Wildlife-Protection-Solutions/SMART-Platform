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
package org.wcs.smart.dataentry.dialog.composite;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.dataentry.dialog.RenameTreeDialog;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.ui.properties.AttributeTreeContentProvider;
import org.wcs.smart.ui.properties.TreeEditorField;

/**
 * Info composite for {@link CmAttribute} of tree type
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class TreeAttributeInfoComposite extends CmAttributeInfoComposite {

	private TreeEditorField defaultValueTreeField;
	private TreeViewer attributeTreeViewer;
	
	/**
	 * @param parent
	 * @param model
	 * @param session
	 */
	public TreeAttributeInfoComposite(Composite parent, ConfigurableModel model, Session session) {
		super(parent, model, session);
	}

	/* (non-Javadoc)
	 * @see org.wcs.smart.dataentry.dialog.composite.CmAttributeInfoComposite#createTypeSpecificControls(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected void createTypeSpecificControls(Composite container) {
		createIsVisibleControl(container);
		createBooleanControl(container, CmAttributeOption.ID_FLATTEN_TREE, Messages.CmAttributeInfoComposite_Option_FlattenTree, Messages.CmAttributeInfoComposite_Option_Flatten_Checkbox);
		createDefaultControl(container);
		createTreeControl(container);	
		createRenameButton(container);
		
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject) {
				//default value field
				defaultValueTreeField.setAttribute(getSourceObject().getAttribute());
				defaultValueTreeField.clear();
				CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_DEFAULT_VALUE);
				if (option.getUuidValue() != null){
					AttributeTreeNode defaultNode = (AttributeTreeNode) getSession().load(AttributeTreeNode.class, option.getUuidValue());
					defaultValueTreeField.setValue(defaultNode);
				}
				
				//tree viewer
				attributeTreeViewer.setInput(getSourceObject().getAttribute());
				attributeTreeViewer.expandToLevel(2);
				
			}
		});
	}

	private void createDefaultControl(Composite container) {
		Label label = new Label(container, SWT.NONE);
		label.setText(Messages.CmAttributeInfoComposite_Option_DefaultValue);
		
		
		defaultValueTreeField = new TreeEditorField();
		defaultValueTreeField.createComposite(container);
		
		getShell().addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				defaultValueTreeField.dispose();
			}
		});
		
		defaultValueTreeField.addSelectionChangedListener(new Listener(){
			@Override
			public void handleEvent(Event event) {
				AttributeTreeNode node = defaultValueTreeField.getValue();
				if (node != null){
					CmAttributeOption option = getSourceObject().getCmAttributeOptions().get(CmAttributeOption.ID_DEFAULT_VALUE);
					option.setUuidValue(node.getUuid());
					fireModelChanged();
				}
			}
		});
	}

	private void createTreeControl(Composite container) {
		Label lblValues = new Label(container, SWT.NONE);
		lblValues.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		lblValues.setText(Messages.TreeAttributeInfoComposite_valueLabel);
		
		attributeTreeViewer = new TreeViewer(container);
		attributeTreeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		attributeTreeViewer.setLabelProvider(new CmTreeLabelProvider(getSession(), getModel()));
		attributeTreeViewer.setContentProvider(new AttributeTreeContentProvider(true, false));
	}
	
	private void createRenameButton(Composite container) {
		Button btnRename = new Button(container, SWT.PUSH);
		btnRename.setText(Messages.TreeAttributeInfoComposite_RenameButton);
		btnRename.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false,2,1));

		btnRename.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!MessageDialog.openConfirm(getShell(), Messages.TreeAttributeInfoComposite_WarnTitle, Messages.TreeAttributeInfoComposite_WarnMessage)){
					return;
				}
				RenameTreeDialog dialog = new RenameTreeDialog(getShell(),getSourceObject().getAttribute(),getModel(),getSession());
				dialog.open();
				
				attributeTreeViewer.refresh();
				fireModelChanged();
			}
		});
		
	}
	
}
