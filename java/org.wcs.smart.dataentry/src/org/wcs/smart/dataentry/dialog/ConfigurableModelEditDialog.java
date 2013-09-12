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
package org.wcs.smart.dataentry.dialog;

import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.dialog.ConfigurableModelTreeContentProvider.CmRootNode;
import org.wcs.smart.dataentry.dialog.composite.AbstractInfoComposite.IModelChangedListener;
import org.wcs.smart.dataentry.dialog.composite.CmNodeInfoComposite;
import org.wcs.smart.dataentry.dialog.composite.CmRootNodeInfoComposite;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * Dialog for editing Configurable Models.
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class ConfigurableModelEditDialog extends AbstractPropertyJHeaderDialog {
	
	private static final int DIALOG_WIDTH = 680;
	private static final int DIALOG_HEIGHT = 680;

	private ConfigurableModel model;
	
	private TreeViewer modelTreeViewer;

	private Composite infoInnerPanel;
	private Composite emptyComposite;
	private CmRootNodeInfoComposite rootNodeComposite;
	private CmNodeInfoComposite groupNodeComposite;
	private CmNodeInfoComposite categoryNodeComposite;
	
	public ConfigurableModelEditDialog(ConfigurableModel model) {
		super(Display.getDefault().getActiveShell(), Messages.ConfigurableModelEditDialog_Title);
		this.model = model;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(DIALOG_WIDTH, DIALOG_HEIGHT);
	}
	
	@Override
	protected Composite createContent(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, true));

		modelTreeViewer = new TreeViewer(container, SWT.V_SCROLL | SWT.H_SCROLL);
		modelTreeViewer.setLabelProvider(new ConfigurableModelLabelProvider());
		modelTreeViewer.setContentProvider(new ConfigurableModelTreeContentProvider(true));
		modelTreeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		modelTreeViewer.setInput(model);
		modelTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				updateRightPanelState();
			}
		});

		Composite rightPanel = new Composite(container, SWT.NONE);
		rightPanel.setLayout(new GridLayout(1, false));
		rightPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		infoInnerPanel = new Composite(rightPanel, SWT.NONE);
		infoInnerPanel.setLayout(new StackLayout());
		infoInnerPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		emptyComposite = new Composite(infoInnerPanel, SWT.NONE);
		emptyComposite.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
		
		IModelChangedListener modelChangeListener = new IModelChangedListener() {
			@Override
			public void modelChanged() {
				setChangesMade(true);
				modelTreeViewer.refresh();
			}
		};

		 //NOTE: session is already opened and should not be closed until dialog is closed
		Session session = getSession();
		rootNodeComposite = new CmRootNodeInfoComposite(infoInnerPanel, model, session);
		rootNodeComposite.addModelChangedListener(modelChangeListener);

		groupNodeComposite = new CmNodeInfoComposite(infoInnerPanel, model, session, true);
		groupNodeComposite.addModelChangedListener(modelChangeListener);
		
		categoryNodeComposite = new CmNodeInfoComposite(infoInnerPanel, model, session, false);
		categoryNodeComposite.addModelChangedListener(modelChangeListener);
		
		setTitle(Messages.ConfigurableModelEditDialog_Title);
		setMessage(Messages.ConfigurableModelEditDialog_Message);
		
		return container;
	}

	@Override
	protected boolean performSave() {
		DataentryHibernateManager.saveConfigurableModel(model);
		setChangesMade(false);
		return true;
	}
	
	private void updateRightPanelState() {
		IStructuredSelection selection = (IStructuredSelection) modelTreeViewer.getSelection();
		Object obj = selection.getFirstElement();

		if (obj instanceof CmNode) {
			CmNode node = (CmNode) obj;
			CmNodeInfoComposite cmp = node.isGroup() ? groupNodeComposite : categoryNodeComposite;
			cmp.setSourceObject(node);
			((StackLayout)infoInnerPanel.getLayout()).topControl = cmp;
			
		} else if (obj instanceof CmRootNode) {
			rootNodeComposite.setSourceObject((CmRootNode)obj);
			((StackLayout)infoInnerPanel.getLayout()).topControl = rootNodeComposite;
			
		} else {
			((StackLayout)infoInnerPanel.getLayout()).topControl = emptyComposite;
			
		}
		infoInnerPanel.layout();
	}
	
}
