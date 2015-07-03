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

import java.text.MessageFormat;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditDialog;
import org.wcs.smart.dataentry.dialog.ConfigurableModelEditDialog.ControlButton;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Info composite for {@link CmNode}
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class CmNodeInfoComposite extends AbstractInfoComposite {

	private CmNode node;

	private Label lblCategory;
	private Label lblKey;

	private Button btnPhoto;
	private Button btnPhotoRequired;
	private boolean isGroup;
	
	public CmNodeInfoComposite(Composite parent, ConfigurableModel model, Session session, boolean isGroup) {
		super(parent, model, session);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		this.setLayout(layout);
		this.isGroup = isGroup;
		
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		if (isGroup)
			createGroupControls();
		else
			createCategoryControls();
	}
	
	public boolean isButtonValid(ConfigurableModelEditDialog.ControlButton button){
		if (isGroup){
			//groups enable all buttons
			return true;
		}else{
			//categories can only be deleted
			if (button == ControlButton.DELETE){
				return true;
			}
		}
		
		return false;
	}
	
	private void createGroupControls() {
		Composite container = createContentContainer(this);
		createDisplayNameControls(container);		
	}

	private void createCategoryControls() {
		Composite container = createContentContainer(this);
		createDisplayNameControls(container);
		
		Label label = new Label(container, SWT.NONE);
		label.setText(Messages.CmNodeInfoComposite_Category);
		label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		lblCategory = new Label(container, SWT.WRAP);
		lblCategory.setText(""); //$NON-NLS-1$
		lblCategory.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		((GridData)lblCategory.getLayoutData()).widthHint = 100;
		
		label = new Label(container, SWT.NONE);
		label.setText(Messages.CmNodeInfoComposite_Key);
		lblKey = new Label(container, SWT.NONE);
		lblKey.setText(""); //$NON-NLS-1$

		label = new Label(container, SWT.NONE);
		label.setText(Messages.CmNodeInfoComposite_PhotoAllowed);
		label.setToolTipText(Messages.CmNodeInfoComposite_photoOptionTooltip);
		btnPhoto = new Button(container, SWT.CHECK);
		btnPhoto.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSourceObject().setPhotoAllowed(btnPhoto.getSelection());
				btnPhotoRequired.setEnabled(getSourceObject().isPhotoAllowed());
				fireModelChanged();
			}
		});

		
		label = new Label(container, SWT.NONE);
		label.setText(Messages.CmNodeInfoComposite_PhotoRequired);
		label.setToolTipText(Messages.CmNodeInfoComposite_photoRequiredTooltip);
		btnPhotoRequired = new Button(container, SWT.CHECK);
		btnPhotoRequired.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				getSourceObject().setPhotoRequired(btnPhotoRequired.getSelection());
				fireModelChanged();
			}
		});
		
		
		addSourceObjectChangedListener(new ISourceObjectChangedListener() {
			@Override
			public void sourceObjectChanged(Object newObject, Language language) {
				CmNode n = getSourceObject();
				boolean isGroup = n.isGroup();
				if (!isGroup) {
					if (lblCategory != null)
						lblCategory.setText(n.getCategory().getFullCategoryName(language));
					if (lblKey != null)
						lblKey.setText(n.getCategory().getKeyId());
					if (btnPhoto != null)
						btnPhoto.setSelection(n.isPhotoAllowed());
					if (btnPhotoRequired != null) {
						btnPhotoRequired.setSelection(n.isPhotoRequired());
						btnPhotoRequired.setEnabled(n.isPhotoAllowed());
					}
					CmNodeInfoComposite.this.layout(true, true);
				}
			}
		});
	}

	@Override
	protected void handleDeleteNode() {
		
		CmNode node = getSourceObject();
		
		if (!MessageDialog.openConfirm(getShell(), Messages.CmNodeInfoComposite_DeleteDialogTitle, 
				MessageFormat.format(Messages.CmNodeInfoComposite_DeleteConfirmation, new Object[]{node.getName()}))){
			return;
		}
		
		CmNode parentNode = node.getParent();
		if (parentNode == null) {
			//this is the root node
			getModel().getNodes().remove(node);
			//re-order nodes
			int i = 0;
			for (CmNode n : getModel().getNodes()){
				n.setNodeOrder(i++);
			}
		} else {
			//not a root node
			parentNode.getChildren().remove(node);
			
			//re-order nodes
			int i = 0;
			for (CmNode n : parentNode.getChildren()){
				n.setNodeOrder(i++);
			}
		}
		node.setParent(null);
		//remove default tree mapping if present
		Set<Attribute> existingTrees = CmDefaultTreesUtil.getPresentedTreeAttributes(getModel());
		for (Attribute a : CmDefaultTreesUtil.getPresentedTreeAttributes(node)) {
			if (!existingTrees.contains(a)) {
				//attribute is not present in CM anymore -> remove default mapping
				getModel().removeDefaultTrees(a);
			}
		}
		//remove default list mapping if present
		Set<Attribute> existingLists = CmDefaultListsUtil.getPresentedListAttributes(getModel());
		for (Attribute a : CmDefaultListsUtil.getPresentedListAttributes(node)) {
			if (!existingLists.contains(a)) {
				//attribute is not present in CM anymore -> remove default mapping
				getModel().removeDefaultLists(a);
			}
		}
		getSession().flush();
		fireModelChanged();
	}
	
	@Override
	public CmNode getSourceObject() {
		return node;
	}

	public void setSourceObject(CmNode node, Language currentLanguage) {
		this.node = node;
		fireSourceObjectChanged(node, currentLanguage);
	}
}
