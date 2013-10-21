package org.wcs.smart.dataentry.dialog.composite;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.ui.NamedItemLabelProvider;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

public class ListItemsConfigDialog extends AbstractPropertyJHeaderDialog {

	private CmAttribute cmAttribute;
	private TableViewer listViewer;
	
	protected ListItemsConfigDialog(CmAttribute cmAttribute) {
		super(Display.getDefault().getActiveShell(), Messages.ListItemsConfigDialog_Title);
		this.cmAttribute = cmAttribute; 
	}

	@Override
	protected Composite createContent(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, true));

		listViewer = new TableViewer(container, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		listViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		listViewer.setLabelProvider(new NamedItemLabelProvider());
		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		listViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		listViewer.setInput(cmAttribute.getAttribute().getActiveListItems());

		
		setTitle(Messages.ListItemsConfigDialog_Title);
		setMessage(Messages.ListItemsConfigDialog_Message);

		setChangesMade(false);
		return container;
	
	}

	@Override
	protected boolean performSave() {
		// TODO Auto-generated method stub
		return false;
	}

}
