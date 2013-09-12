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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.hibernate.Session;
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
	
	public CmNodeInfoComposite(Composite parent, ConfigurableModel model, Session session, boolean isGroup) {
		super(parent, model, session);
		this.setLayout(new GridLayout(1, false));
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		if (isGroup)
			createGroupControls();
		else
			createCategoryControls();
	}
	
	private void createGroupControls() {
		createAddButtons(this);
		createDeleteButton();
		
		Composite container = createContentContainer(this);
		createDisplayNameControls(container);
		
	}

	private void createCategoryControls() {
		createDeleteButton();
		
		Composite container = createContentContainer(this);

		createDisplayNameControls(container);
		
		Label label = new Label(container, SWT.NONE);
		label.setText("Category:");
		lblCategory = new Label(container, SWT.NONE);
		lblCategory.setText(""); //$NON-NLS-1$

		label = new Label(container, SWT.NONE);
		label.setText("Key:");
		lblKey = new Label(container, SWT.NONE);
		lblKey.setText(""); //$NON-NLS-1$
	}

	private void createDeleteButton() {
		Button btnDelete = new Button(this, SWT.PUSH);
		btnDelete.setText("Delete");
		btnDelete.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleDeleteNode();
			}
		});
	}

	private void handleDeleteNode() {
		CmNode node = getSourceObject();
		CmNode parentNode = node.getParent();
		if (parentNode == null) {
			//this is the root node
			getModel().getNodes().remove(node);
		} else {
			//not a root node
			parentNode.getChildren().remove(node);
		}
		node.setParent(null);
		node.setModel(null);
		
		fireModelChanged();
	}
	
	@Override
	public CmNode getSourceObject() {
		return node;
	}

	public void setSourceObject(CmNode node) {
		this.node = node;
		if (!node.isGroup()) {
			if (lblCategory != null)
				lblCategory.setText(node.getCategory().getFullCategoryName());
			if (lblKey != null)
				lblKey.setText(node.getCategory().getKeyId());
			layout(true, true);
		}
	}
}
