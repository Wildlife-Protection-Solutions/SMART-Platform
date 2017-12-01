package org.wcs.smart.asset.ui.views.data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.asset.data.importer.FileProxy;

public class DeletedFilesPanel {

	private DataImportPage view;
	
	private TableViewer tblResults;
	private Composite item;
	
	public DeletedFilesPanel(Composite parent, DataImportPage view, FormToolkit toolkit) {
		this.view = view;
		createComposite(parent,  toolkit);
	}
	
	public Control getControl() {
		return item;
	}
	
	public void addSelectionChangedListener(ISelectionChangedListener l) {
		tblResults.addSelectionChangedListener(l);
	}
	
	public IStructuredSelection getSelection() {
		return tblResults.getStructuredSelection();
	}
	
	private void createComposite(Composite parent, FormToolkit toolkit) {
		item = toolkit.createComposite(parent);
		item.setLayout(new GridLayout());
		
		tblResults = new TableViewer(item, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		tblResults.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblResults.getTable().setHeaderVisible(false);
		tblResults.getTable().setLinesVisible(false);
		tblResults.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof FileProxy) return ((FileProxy) element).getFile().getFileName().toString();
				return super.getText(element);
			}
		});
		tblResults.setContentProvider(ArrayContentProvider.getInstance());
		tblResults.setInput(view.getDeletedItems());
		tblResults.refresh();
		
		Menu mnu = new Menu(tblResults.getControl());
		MenuItem restore = new MenuItem(mnu, SWT.PUSH);
		restore.setText("Restore");
		restore.addListener(SWT.Selection, e->{
			List<FileProxy> filesToAdd = new ArrayList<>();
			for (Iterator<?> iterator = tblResults.getStructuredSelection().iterator(); iterator.hasNext();) {
				FileProxy type = (FileProxy) iterator.next();
				filesToAdd.add(type);
			}
			
			view.restoreFiles(filesToAdd);
		});
		tblResults.getControl().setMenu(mnu);
	}
	
	public void refresh() {
		tblResults.refresh();
	}
	
	
	
}
