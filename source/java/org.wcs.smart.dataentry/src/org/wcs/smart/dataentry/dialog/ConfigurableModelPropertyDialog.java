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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.dataentry.DataentryHibernateManager;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * Dialog for editing Configurable Models.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ConfigurableModelPropertyDialog extends AbstractPropertyJHeaderDialog {

	private TableViewer modelListViewer;
	private TreeViewer modelTreeViewer;

	public ConfigurableModelPropertyDialog(Shell parent) {
		super(parent, Messages.ConfigurableModelPropertyDialog_Title);
	}

	@Override
	protected Composite createContent(Composite parent) {
		List<ConfigurableModel> modelList = new ArrayList<ConfigurableModel>();
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try {
			modelList = DataentryHibernateManager.getConfigurableModels(s);
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Display.getDefault().getActiveShell(), Messages.ConfigurableModelPropertyDialog_LoadModelsListError, ex);
		} finally {
			s.getTransaction().rollback();
			s.close();
		}

		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));

		modelListViewer = new TableViewer(container, SWT.V_SCROLL | SWT.H_SCROLL);
		modelListViewer.setLabelProvider(new NamedItemLabelProvider());
		modelListViewer.setContentProvider(ArrayContentProvider.getInstance());
		modelListViewer.setInput(modelList.toArray());
		modelListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		modelListViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection selection = (IStructuredSelection) modelListViewer.getSelection();
				if (!selection.isEmpty()) {
					ConfigurableModel cm = (ConfigurableModel) selection.getFirstElement();
					cm = DataentryHibernateManager.getFullConfigurableModel(cm);
					modelTreeViewer.setInput(cm);
				}
			}
		});
		
		modelTreeViewer = new TreeViewer(container, SWT.V_SCROLL | SWT.H_SCROLL);
		modelTreeViewer.setLabelProvider(new NamedItemLabelProvider());
		modelTreeViewer.setContentProvider(new ConfigurableModelTreeContentProvider());
		modelTreeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		setTitle(Messages.ConfigurableModelPropertyDialog_Title);
		setMessage(Messages.ConfigurableModelPropertyDialog_Message);
		
		//setChangesMade(true); //TODO: remove this line
		
		return container;
	}

	@Override
	protected boolean performSave() {
		//addFakeModel();
		return true;
	}

	//TODO: remove this method after it is not required
	void addFakeModel() {
		ConfigurableModel model = new ConfigurableModel();
		model.setName("Model-B"); //$NON-NLS-1$
		model.updateName(SmartDB.getCurrentLanguage(), model.getName());
		model.setConservationArea(SmartDB.getCurrentConservationArea());
		List<CmNode> nodes = new ArrayList<CmNode>();
		CmNode node1 = new CmNode();
		node1.setName("Group B1"); //$NON-NLS-1$
		node1.updateName(SmartDB.getCurrentLanguage(), node1.getName());
		node1.setModel(model);
		node1.setNodeOrder(0);
		nodes.add(node1);

		CmNode node2 = new CmNode();
		node2.setName("Group B2"); //$NON-NLS-1$
		node2.updateName(SmartDB.getCurrentLanguage(), node2.getName());
		node2.setModel(model);
		node2.setNodeOrder(1);
		nodes.add(node2);
		
		CmNode node11 = new CmNode();
		node11.setName("Group B1-1"); //$NON-NLS-1$
		node11.updateName(SmartDB.getCurrentLanguage(), node11.getName());
		node11.setParent(node1);
		node11.setModel(model);
		node11.setNodeOrder(0);
		node1.setChildren(new ArrayList<CmNode>());
		node1.getChildren().add(node11);
		
		model.setNodes(nodes);
		DataentryHibernateManager.saveConfigurableModel(model);
	}
	
}
