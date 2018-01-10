package org.wcs.smart.asset.ui.views.map;

import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.DialogConstants;

public class ColumnListDialog extends TitleAreaDialog {

	private CheckboxTableViewer chColumns;
	private AssetMapColumnConfiguration configuration;
	
	public ColumnListDialog(Shell parentShell, AssetMapColumnConfiguration configuration) {
		super(parentShell);
		
		this.configuration = configuration;
	}


	@Override
	public void okPressed() {
		for (OverviewTableColumnWrapper c : configuration.getColumns()) {
			boolean isVisible = chColumns.getChecked(c);
			if (isVisible == c.isVisible()) continue;
			c.setVisible(isVisible);
		}
		if (!configuration.saveConfiguration()) return;
		super.okPressed();
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout(2, false));
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		
		chColumns = CheckboxTableViewer.newCheckList(parent, SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		chColumns.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		chColumns.setContentProvider(ArrayContentProvider.getInstance());
		chColumns.setLabelProvider(new TableLabelProvider());
		chColumns.setInput(configuration.getColumns());
		chColumns.setCheckedElements(configuration.getColumns().stream().filter(e->e.isVisible()).collect(Collectors.toList()).toArray());
		
		chColumns.addCheckStateListener(e->{
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		});
			
		setTitle("Asset Overview Map");
		getShell().setText("Asset Overview Map");
		setMessage("Configure columns in the asset overview map.  Checked items will be visible in table.  All columns will be avaliable in the map for styling.");
		return parent;
	}
	

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button btn = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		btn.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
	}
	
		
	@Override
	public boolean isResizable(){
		return true;
	}
	

	class TableLabelProvider extends LabelProvider implements ITableColorProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof OverviewTableColumnWrapper) return ((OverviewTableColumnWrapper) element).getColumn().getName();
			return super.getText(element);
		}

		@Override
		public Color getForeground(Object element, int columnIndex) {
			if (element instanceof OverviewTableColumnWrapper) {
				if (((OverviewTableColumnWrapper) element).isFixed()) {
					return Display.getDefault().getSystemColor(SWT.COLOR_DARK_BLUE);
				}
			}
			return null;
		}

		@Override
		public Color getBackground(Object element, int columnIndex) {
			return null;
		}
	}
}
