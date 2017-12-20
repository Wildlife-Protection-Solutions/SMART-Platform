package org.wcs.smart.asset.ui.metadata;

import java.util.List;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.ui.properties.DialogConstants;

public class XmpTagSelector extends Dialog {

	private TableViewer metadata;
	private List<String[]> xmpMetadata;
	
	private String selectedPath;
	
	public XmpTagSelector(Shell parent,  List<String[]> xmpMetadata) {
		super(parent);
		this.xmpMetadata = xmpMetadata;
	}

	protected void createButtonsForButtonBar(Composite parent) {
		Button btnOk = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT,true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		btnOk.setEnabled(false);
	}
	
	public String getXmpPath() {
		return selectedPath;
	}
	
	protected void okPressed() {
		Object data = metadata.getStructuredSelection().getFirstElement();
		if (data instanceof String[]) {
			selectedPath = ((String[])data)[0];
			super.okPressed();
		}
	}
	
	private void validate() {
		boolean ok = false;
		if (!metadata.getSelection().isEmpty()) {
			ok = metadata.getStructuredSelection().getFirstElement() instanceof Object[];
		}
		getButton(IDialogConstants.OK_ID).setEnabled(ok);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		
		parent = (Composite) super.createDialogArea(parent);
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		metadata = new TableViewer(main, SWT.FULL_SELECTION);
		metadata.getTable().setLinesVisible(false);
		metadata.getTable().setHeaderVisible(true);
		metadata.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridData)metadata.getTable().getLayoutData()).widthHint = 400;
		((GridData)metadata.getTable().getLayoutData()).heightHint = 400;
		metadata.setContentProvider(ArrayContentProvider.getInstance());
		metadata.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				validate();
			}
		});
		metadata.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				okPressed();
			}
		});
		Color bgColor = new Color(metadata.getControl().getDisplay(), 160,185,224);
		metadata.getControl().addListener(SWT.Dispose, e->bgColor.dispose());

		TableViewerColumn colTag = new TableViewerColumn(metadata, SWT.NONE);
		colTag.getColumn().setText("Tag");
		colTag.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof String[]) return (String) ((String[])element)[0];
				if (element instanceof String) return (String)element;
				return "";
			}
		});
		
		
		TableViewerColumn colTagValue = new TableViewerColumn(metadata, SWT.NONE);
		colTagValue.getColumn().setText("Value");
		colTagValue.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof String[]) return (String) ((String[])element)[1];
				return "";
			}
			@Override
			public Color getBackground(Object element) {
				if (element instanceof String) return bgColor;
				return null;
			}
		});
		
		metadata.setInput(xmpMetadata);
		int w = ( main.computeSize(SWT.DEFAULT, SWT.DEFAULT).x - 20 ) / metadata.getTable().getColumnCount();
		for (TableColumn c : metadata.getTable().getColumns()) c.setWidth(w);
		return parent;
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}
}
