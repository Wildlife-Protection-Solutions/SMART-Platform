package org.wcs.smart.i2.ui.dialogs;

import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TableColumn;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.ui.ProfileLabelProvider;
import org.wcs.smart.ui.SmartStyledDialog;
import org.wcs.smart.ui.SmartStyledTitleDialog;

public class SelectProfileDialog extends SmartStyledTitleDialog{

	private List<IntelProfile> items;
	
	private TableViewer tblViewer;
	
	private IntelProfile selection;
	
	public SelectProfileDialog(Shell parent, List<IntelProfile> items) {
		super(parent);
		this.items = items;
	}

	public IntelProfile getSelection() {
		return selection;
	}
	
	@Override
	public void cancelPressed() {
		this.selection = null;
		super.cancelPressed();
	}
	
	@Override
	public void okPressed() {
		selection = (IntelProfile) tblViewer.getStructuredSelection().getFirstElement();
		super.okPressed();
	}
	
	@Override
	public Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new TableColumnLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblViewer = new TableViewer(main, SWT.FULL_SELECTION);
		tblViewer.setContentProvider(ArrayContentProvider.getInstance());
		tblViewer.setLabelProvider(new ProfileLabelProvider());
		tblViewer.setInput(items);
		tblViewer.getTable().addListener(SWT.MeasureItem, e->{
			e.height = 25;
		});
		TableColumn tc = new TableColumn(tblViewer.getTable(), SWT.NONE);
		((TableColumnLayout)main.getLayout()).setColumnData(tc, new ColumnWeightData(1));
		tblViewer.addDoubleClickListener(e->okPressed());
		
		setTitle("Profile");
		setMessage("Select a profile");
		getShell().setText("Select Profile");
		return parent;
	}
	
}
