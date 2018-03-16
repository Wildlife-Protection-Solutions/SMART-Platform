package org.wcs.smart.event.ui;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.event.EventPlugIn;

public class ConfigureEventsDialog extends TitleAreaDialog {

	public ConfigureEventsDialog(Shell parentShell) {
		super(parentShell);
	}

	public Point getInitialSize() {
		return new Point(600,600);
	}
	@Override
	public Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true ));
		
		TabFolder tabs = new TabFolder(main, SWT.NONE);
		tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		EventsPanel eventPanel = new EventsPanel(tabs, SWT.NONE);
		FiltersPanel filtersPanel = new FiltersPanel(tabs, SWT.NONE);
		ActionsPanel actionPanel = new ActionsPanel(tabs, SWT.NONE);
		ActionTypesPanel typesPanel = new ActionTypesPanel(tabs, SWT.NONE);
		
		TabItem eventTab = new TabItem(tabs, SWT.NONE);
		eventTab.setText("Events");
		eventTab.setControl(eventPanel);
		eventTab.setImage(EventPlugIn.getDefault().getImageRegistry().get(EventPlugIn.ICON_ACTION_EVENT));
		
		TabItem filtersTab = new TabItem(tabs, SWT.NONE);
		filtersTab.setText("Filters");
		filtersTab.setControl(filtersPanel);
		filtersTab.setImage(EventPlugIn.getDefault().getImageRegistry().get(EventPlugIn.ICON_FILTER));
		
		TabItem actionsTab = new TabItem(tabs, SWT.NONE);
		actionsTab.setText("Actions");
		actionsTab.setControl(actionPanel);
		actionsTab.setImage(EventPlugIn.getDefault().getImageRegistry().get(EventPlugIn.ICON_ACTION));
		
		TabItem typesTab = new TabItem(tabs, SWT.NONE);
		typesTab.setText("Action Types");
		typesTab.setControl(typesPanel);
		typesTab.setImage(EventPlugIn.getDefault().getImageRegistry().get(EventPlugIn.ICON_ACTION_TYPE));
		
		setTitle("System Triggers");
		getShell().setText("System Triggers");
		setMessage("Configure the events that are fired when new observations are created.");
		
		return parent;
	}
	
	
	@Override
	public boolean isResizable() {
		return true;
	}
}
