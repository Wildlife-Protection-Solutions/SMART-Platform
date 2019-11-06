package org.wcs.smart.i2.ui.preference;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.preference.PreferenceContentProvider;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceLabelProvider;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeColumn;
import org.wcs.smart.common.control.SmartUiUtils;


public class IntelPreferenceDialog extends PreferenceDialog {

	
	public IntelPreferenceDialog(Shell parentShell, PreferenceManager manager) {
		super(parentShell, manager);
		
		super.addPageChangedListener(event->((IIntelPreferencePage)event.getSelectedPage()).refresh());
	}
	
	@Override
	protected Control createTreeAreaContents(Composite parent) {
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayout(new GridLayout());
		((GridLayout)outer.getLayout()).marginWidth = 0;
		((GridLayout)outer.getLayout()).marginHeight = 0;
		super.createTreeAreaContents(outer);
		getTreeViewer().getTree().addListener(SWT.MeasureItem, e->{
			e.height = 24;
		});
		
		GridDataFactory.fillDefaults().hint(getLastRightWidth(), convertVerticalDLUsToPixels(120)).grab(false, true)
		.applyTo(outer);
		
		getShell().setText("Profiles Configurations");
		return outer;
	}
	@Override
	protected TreeViewer createTreeViewer(Composite parent) {
		Composite temp = new Composite(parent, SWT.NONE);
		temp.setLayout(new TreeColumnLayout());
		GridDataFactory.fillDefaults().grab(true, true).applyTo(temp);
		
		final TreeViewer viewer = new TreeViewer(temp, SWT.FULL_SELECTION);
		addListeners(viewer);
		viewer.setLabelProvider(new PreferenceLabelProvider());
		viewer.setContentProvider(new PreferenceContentProvider());
		
		TreeColumn tc = new TreeColumn(viewer.getTree(), SWT.NONE);
		((TreeColumnLayout)temp.getLayout()).setColumnData(tc,  new ColumnWeightData(1));
		
		viewer.getTree().addListener(SWT.MeasureItem, e->{
			e.height = 32;
		});
		
		return viewer;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		SmartUiUtils.makeTransparent(parent);
	}
	
	@Override
	protected Composite createPageContainer(Composite parent) {
		Composite p = super.createPageContainer(parent);
		SmartUiUtils.makeTransparent(p);
		parent.getParent().setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		return p;
	}
	@Override
	public void updateButtons() {
	
	}
	@Override
	protected Control createContents(Composite parent) {
		Control c = super.createContents(parent);
		SmartUiUtils.makeTransparent(((Composite)c));
		return c;
	}
	
	
	
}
