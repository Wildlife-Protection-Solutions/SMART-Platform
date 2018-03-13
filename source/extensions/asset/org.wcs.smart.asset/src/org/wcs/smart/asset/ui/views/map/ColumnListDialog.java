/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.views.map;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for managing the asset overview map table columns.
 * 
 * @author Emily
 *
 */
public class ColumnListDialog extends TitleAreaDialog {

	private CheckboxTableViewer chColumns;
	private AssetMapColumnConfiguration configuration;
	
	
	/**
	 * Creates a new dialog for managing the asset table columns.  The configuration
	 * provided here is modified so provide a clone if you don't want changes to affect
	 * you.
	 * 
	 * @param parentShell
	 * @param configuration
	 */
	public ColumnListDialog(Shell parentShell, AssetMapColumnConfiguration configuration) {
		super(parentShell);
		this.configuration = configuration;
	}


	@Override
	public void cancelPressed() {
		if (getButton(IDialogConstants.OK_ID).isEnabled()) {
			//some changes were made, we need to reload from the database
			
		}
		super.cancelPressed();
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
		
		chColumns = CheckboxTableViewer.newCheckList(parent, SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
		chColumns.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		chColumns.setContentProvider(ArrayContentProvider.getInstance());
		chColumns.setLabelProvider(new TableLabelProvider());
		chColumns.setInput(configuration.getColumns());
		chColumns.setCheckedElements(configuration.getColumns().stream().filter(e->e.isVisible()).collect(Collectors.toList()).toArray());
		
		chColumns.addCheckStateListener(e->{
			getButton(IDialogConstants.OK_ID).setEnabled(true);
		});
		
		chColumns.addDoubleClickListener(new IDoubleClickListener() {		
			@Override
			public void doubleClick(DoubleClickEvent event) {
				edit();
			}
		});
		chColumns.getControl().addListener(SWT.KeyDown, e->{
			if (e.keyCode == SWT.SPACE) {
				IStructuredSelection selection = chColumns.getStructuredSelection();
				boolean value = chColumns.getChecked( selection.getFirstElement() );
				for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
					Object tp = (Object) iterator.next();
					chColumns.setChecked(tp, !value);
				}
				e.doit = false;
			}
		});
		Composite btnPanel = new Composite(parent, SWT.NONE);
		btnPanel.setLayout(new GridLayout());
		btnPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		((GridLayout)btnPanel.getLayout()).marginWidth = 0;
		((GridLayout)btnPanel.getLayout()).marginHeight = 0;
		
		Button btnNew = new Button(btnPanel, SWT.PUSH);
		btnNew.setText(DialogConstants.ADD_BUTTON_TEXT);
		btnNew.addListener(SWT.Selection, e->add());
		btnNew.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnRename = new Button(btnPanel, SWT.PUSH);
		btnRename.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnRename.addListener(SWT.Selection, e->edit());
		btnRename.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnRename.setEnabled(false);
		
		Button btnDelete = new Button(btnPanel, SWT.PUSH);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.addListener(SWT.Selection, e->delete());
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnDelete.setEnabled(false);

		Label l = new Label(btnPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnMoveUp = new Button(btnPanel, SWT.PUSH);
		btnMoveUp.setText("Move Up");
		btnMoveUp.addListener(SWT.Selection, e->move(-1));
		btnMoveUp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnMoveUp.setEnabled(false);
		
		Button btnMoveDown = new Button(btnPanel, SWT.PUSH);
		btnMoveDown.setText("Move Down");
		btnMoveDown.addListener(SWT.Selection, e->move(1));
		btnMoveDown.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnMoveDown.setEnabled(false);
		
		
		Menu menu = new Menu(chColumns.getControl());
		
		MenuItem addMenu = new MenuItem(menu, SWT.PUSH);
		addMenu.setText(DialogConstants.ADD_BUTTON_TEXT);
		addMenu.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		addMenu.addListener(SWT.Selection, e->add());
		
		MenuItem renameMenu = new MenuItem(menu, SWT.PUSH);
		renameMenu.setText(btnRename.getText());
		renameMenu.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		renameMenu.addListener(SWT.Selection, e->edit());
		renameMenu.setEnabled(false);
		
		MenuItem deleteMenu = new MenuItem(menu, SWT.PUSH);
		deleteMenu.setText(DialogConstants.DELETE_BUTTON_TEXT);
		deleteMenu.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteMenu.addListener(SWT.Selection, e->delete());
		deleteMenu.setEnabled(false);
		
		new MenuItem(menu, SWT.SEPARATOR);
		
		MenuItem moveUpMenu = new MenuItem(menu, SWT.PUSH);
		moveUpMenu.setText(btnMoveUp.getText());
		moveUpMenu.addListener(SWT.Selection, e->move(-1));
		moveUpMenu.setEnabled(false);
		
		MenuItem moveDownMenu = new MenuItem(menu, SWT.PUSH);
		moveDownMenu.setText(btnMoveDown.getText());
		moveDownMenu.addListener(SWT.Selection, e->move(1));
		moveDownMenu.setEnabled(false);
		
		chColumns.getControl().setMenu(menu);
		
		chColumns.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean canEdit = false;
				for (Iterator<?> iterator = chColumns.getStructuredSelection().iterator(); iterator.hasNext();) {
					Object item = iterator.next();
					if (item instanceof OverviewTableColumnWrapper && !((OverviewTableColumnWrapper) item).isFixed()) {
						canEdit = true;
						break;
					}
				}
				btnDelete.setEnabled(canEdit);
				
				deleteMenu.setEnabled(canEdit);
				btnMoveDown.setEnabled(!chColumns.getStructuredSelection().isEmpty());
				btnMoveUp.setEnabled(!chColumns.getStructuredSelection().isEmpty());
				
				btnRename.setEnabled(chColumns.getStructuredSelection().getFirstElement() instanceof OverviewTableColumnWrapper && !((OverviewTableColumnWrapper)chColumns.getStructuredSelection().getFirstElement()).isFixed());
				renameMenu.setEnabled(btnRename.isEnabled());
				
				moveUpMenu.setEnabled(!chColumns.getStructuredSelection().isEmpty());
				moveDownMenu.setEnabled(!chColumns.getStructuredSelection().isEmpty());
			}
		});
		
		
		setTitle("Asset Overview Map");
		getShell().setText("Asset Overview Map");
		setMessage("Configure columns in the asset overview map.  Checked items will be visible in table.  All columns will be avaliable in the map for styling.");
		return parent;
	}
	
	private void move(int amount) {
		List<OverviewTableColumnWrapper> toMove = new ArrayList<>();
		for (Iterator<?> iterator = chColumns.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object item = iterator.next();
			if (item instanceof OverviewTableColumnWrapper) {
				toMove.add((OverviewTableColumnWrapper) item);
			}
		}
		configuration.moveColumns(toMove, amount);
		chColumns.refresh();
		enableOk();
	}
	
	private void edit() {
		Object x = chColumns.getStructuredSelection().getFirstElement();
		if (!(x instanceof OverviewTableColumnWrapper)) return;
		OverviewTableColumnWrapper c = (OverviewTableColumnWrapper)x;
		if (c.isFixed()) return;
		if (!(c.getColumn() instanceof CategoryOverviewColumn || c.getColumn() instanceof CombinedOverviewColumn)) return;
		
		
		List<IOverviewTableColumn> allcolumns = configuration.getColumns().stream().map(e->e.getColumn()).collect(Collectors.toList());
		CategoryColumnDialog dialog = new CategoryColumnDialog(getShell(), c.getColumn(), allcolumns);
		if (dialog.open() == Window.OK) {
			chColumns.refresh();
			enableOk();
		}
	}
	
	private void delete() {
		for (Iterator<?> iterator = chColumns.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object item = iterator.next();
			if (item instanceof OverviewTableColumnWrapper) {
				if (!((OverviewTableColumnWrapper)item).isFixed()){
					configuration.removeColumn((OverviewTableColumnWrapper)item);
				}
			}
		}
		chColumns.refresh();
		enableOk();
	}
	
	private void add() {
		List<IOverviewTableColumn> allcolumns = configuration.getColumns().stream().map(e->e.getColumn()).collect(Collectors.toList());
		CategoryColumnDialog dialog = new CategoryColumnDialog(getShell(), allcolumns);
		dialog.open();
		IOverviewTableColumn c = dialog.getNewColumn();
		if (c != null) {
			OverviewTableColumnWrapper x = configuration.addColumn(c);
			chColumns.refresh();
			chColumns.setChecked(x,  true);
			enableOk();
		}
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button btn = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		btn.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, true);
	}
	
	private void enableOk() {
		getButton(IDialogConstants.OK_ID).setEnabled(true);
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
