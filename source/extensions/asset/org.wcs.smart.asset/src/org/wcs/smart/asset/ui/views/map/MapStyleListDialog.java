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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.AssetMapStyle;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Lists all map styles allowing users to rename or delete styles.
 * 
 * @author Emily
 *
 */
public class MapStyleListDialog extends TitleAreaDialog {

	private ListViewer cmbStyles;
	
	private List<AssetMapStyle> mapStyles;
	private List<AssetMapStyle> deletedStyles;
	
	public MapStyleListDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	public void okPressed() {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (AssetMapStyle d : deletedStyles) {
					session.delete(d);
				}
				for (AssetMapStyle s : mapStyles) {
					session.saveOrUpdate(s);
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.displayLog("Unable to save changes to styles.  Close and reopen dialog and try again." + ex.getMessage(), ex);
				return;
			}
		}
		super.okPressed();
	}
	
	@SuppressWarnings("unchecked")
	private void delete() {
		List<AssetMapStyle> toDelete = new ArrayList<>();
		for (Iterator<Object> iterator = cmbStyles.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object obj = iterator.next();
			if (obj instanceof AssetMapStyle) {
				toDelete.add((AssetMapStyle) obj);
			}	
		}
		deletedStyles.addAll(toDelete);
		mapStyles.removeAll(toDelete);
		cmbStyles.refresh();
		
		enableOk(true);
	}
	
	@SuppressWarnings("unchecked")
	private void rename() {
		AssetMapStyle toEdit = null;
		for (Iterator<Object> iterator = cmbStyles.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object obj = iterator.next();
			if (obj instanceof AssetMapStyle) {
				toEdit = (AssetMapStyle)obj;
				break;
			}	
		}
		if (toEdit == null) return;
		
		InputDialog nameDialog = new InputDialog(getShell(), "Map Style Name", "Enter new name for the map style", toEdit.getName(), new IInputValidator() {
			@Override
			public String isValid(String newText) {
				if (newText == null || newText.trim().isEmpty()) return "A name must be provided.";
				if (newText.trim().length() > AssetMapStyle.MAX_NAME_LENGTH) return MessageFormat.format("Name must be fewer than {0} characters.", AssetMapStyle.MAX_NAME_LENGTH);
				return null;
			}
		});
		if (nameDialog.open() != Window.OK) return;
		String newName = nameDialog.getValue().trim();
		toEdit.setName(newName);
		cmbStyles.refresh();
		
		enableOk(true);
	}
	
	private void enableOk(boolean enabled) {
		Button ok = getButton(IDialogConstants.OK_ID);
		if (ok == null) return;
		ok.setEnabled(enabled);
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite c = (Composite) super.createDialogArea(parent);
		
		Composite main = new Composite(c, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		cmbStyles = new ListViewer(main, SWT.BORDER);
		cmbStyles.setContentProvider(ArrayContentProvider.getInstance());
		cmbStyles.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof AssetMapStyle) return ((AssetMapStyle) element).getName();
				return super.getText(element);
			}
		});
		cmbStyles.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite btnPanel = new Composite(main, SWT.NONE);
		btnPanel.setLayout(new GridLayout());
		btnPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridLayout)btnPanel.getLayout()).marginWidth = 0;
		((GridLayout)btnPanel.getLayout()).marginHeight = 0;
		
		Button btnDelete = new Button(btnPanel, SWT.NONE);
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.setEnabled(false);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnDelete.addListener(SWT.Selection, e->delete());
		
		Button btnRename = new Button(btnPanel, SWT.NONE);
		btnRename.setText("Rename");
		btnRename.setEnabled(false);
		btnRename.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		btnRename.addListener(SWT.Selection, e->rename());
		
		Menu mnu = new Menu(cmbStyles.getControl());
		MenuItem miDelete = new MenuItem(mnu, SWT.PUSH);
		miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		miDelete.setEnabled(false);
		miDelete.addListener(SWT.Selection, e->delete());
		
		MenuItem miRename = new MenuItem(mnu, SWT.PUSH);
		miRename.setText(btnRename.getText());
		miRename.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.RENAME_ICON));
		miRename.setEnabled(false);
		miRename.addListener(SWT.Selection, e->rename());
		
		cmbStyles.getControl().setMenu(mnu);
		
		cmbStyles.addSelectionChangedListener(e->{
			btnDelete.setEnabled(!cmbStyles.getSelection().isEmpty());
			btnRename.setEnabled(!cmbStyles.getSelection().isEmpty());
			
			miDelete.setEnabled(!cmbStyles.getSelection().isEmpty());
			miRename.setEnabled(!cmbStyles.getSelection().isEmpty());
		});
		
		setMessage("Managed map styles");
		setTitle("Asset Overview Map Styles");
		getShell().setText("Asset Overview Map Styles");
		
		loadStyleJob.setSystem(true);
		loadStyleJob.schedule();
		
		return c;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button btnOk = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		btnOk.setEnabled(false);
	}

	
	@Override
	public boolean isResizable() {
		return true;
	}
	
	private Job loadStyleJob = new Job("loading asset styles") {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			deletedStyles = new ArrayList<>();
			mapStyles = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				mapStyles.addAll(QueryFactory.buildQuery(session, AssetMapStyle.class,
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).getResultList());
			}
			Display.getDefault().syncExec(()->{
				cmbStyles.setInput(mapStyles);
			});
			return Status.OK_STATUS;
		}
		
	};


}