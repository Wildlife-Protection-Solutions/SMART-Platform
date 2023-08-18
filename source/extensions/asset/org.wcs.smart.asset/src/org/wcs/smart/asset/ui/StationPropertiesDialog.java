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
package org.wcs.smart.asset.ui;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
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
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetStationAttribute;
import org.wcs.smart.asset.model.AssetStationLocationAttribute;
import org.wcs.smart.asset.ui.config.AttributeDialog;
import org.wcs.smart.ca.advisors.DeleteManager;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Dialog for managing attributes collected for each station.
 * 
 * @author Emily
 *
 */
public class StationPropertiesDialog extends SmartStyledTitleDialog {

	private TableViewer tblStationAttribute;
	private TableViewer tblLocationAttribute;
	
	private List<AssetStationAttribute> stationAttributes;
	private List<AssetStationAttribute> deletedStationAttributes;
	
	
	private List<AssetStationLocationAttribute> locationAttributes;
	private List<AssetStationLocationAttribute> deletedLocationAttributes;
	
	private enum Type{STATION, LOCATION};
	
	@Inject
	private IEclipseContext context;
	
	public StationPropertiesDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		Button okBtn = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		
		okBtn.setEnabled(false);
	}
	
	@Override
	public void okPressed() {
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				//location attributes
				for (AssetStationLocationAttribute toDelete : deletedLocationAttributes) {
					if (locationAttributes.contains(toDelete)) continue;
					String deleteQuery = "DELETE FROM AssetStationLocationAttributeValue WHERE id.attribute = :attribute"; //$NON-NLS-1$
					session.createMutationQuery(deleteQuery)
						.setParameter("attribute", toDelete.getAttribute()) //$NON-NLS-1$
						.executeUpdate();
					session.remove(toDelete);
				}
				int index = 0;
				for (AssetStationLocationAttribute toUpdate : locationAttributes) {
					toUpdate.setOrder(index++);
					AssetStationLocationAttribute existing = session.get(AssetStationLocationAttribute.class, toUpdate);
					if (existing == null) {
						session.persist(toUpdate);	
					}else {
						existing.setOrder(toUpdate.getOrder());
					}
				}
				
				//station attributes
				for (AssetStationAttribute toDelete : deletedStationAttributes) {
					if (stationAttributes.contains(toDelete)) continue;
					String deleteQuery = "DELETE FROM AssetStationAttributeValue WHERE id.attribute = :attribute"; //$NON-NLS-1$
					session.createMutationQuery(deleteQuery)
						.setParameter("attribute", toDelete.getAttribute()) //$NON-NLS-1$
						.executeUpdate();
					session.remove(toDelete);
				}
				index = 0;
				for (AssetStationAttribute toUpdate : stationAttributes) {
					toUpdate.setOrder(index++);
					AssetStationAttribute existing = session.get(AssetStationAttribute.class, toUpdate);
					if (existing == null) {
						session.persist(toUpdate);	
					}else {
						existing.setOrder(toUpdate.getOrder());
					}
				}
				session.getTransaction().commit();
			}catch(Exception ex) {
				AssetPlugIn.displayLog(Messages.StationPropertiesDialog_SaveError + ex.getMessage(), ex);
				return;
			}
		}
		
		context.get(IEventBroker.class).post(AssetEvents.ASSETSTATIONLOCATION_CONFIG_MODIFIED, null);

		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	
	
	private void modified() {
		Button btnOk = getButton(IDialogConstants.OK_ID);
		btnOk.setEnabled(true);
	}
	
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite core = new Composite(parent, SWT.NONE);
		core.setLayout(new GridLayout(2, false));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		tblStationAttribute = createTableSection(core, Type.STATION);
		tblLocationAttribute = createTableSection(core, Type.LOCATION);
		
		initTable();
		
		setTitle(Messages.StationPropertiesDialog_Title);
		setMessage(Messages.StationPropertiesDialog_Message);
		getShell().setText(Messages.StationPropertiesDialog_Title);
		
		return parent;
	}
	
	private TableViewer createTableSection(Composite parent, Type type) {
		Label l = new Label(parent, SWT.NONE);
		if (type == Type.STATION) l.setText(Messages.StationPropertiesDialog_StnAttributeLabel);
		if (type == Type.LOCATION) l.setText(Messages.StationPropertiesDialog_LocationAttributeLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
		
		
		Composite tableComp = new Composite(parent, SWT.NONE);
		tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		TableColumnLayout tlayout = new TableColumnLayout();
		tableComp.setLayout(tlayout);
		
		TableViewer tblAttribute = new TableViewer(tableComp, SWT.FULL_SELECTION | SWT.BORDER | SWT.MULTI);
		tblAttribute.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblAttribute.setContentProvider(ArrayContentProvider.getInstance());
		
		
		TableViewerColumn col = new TableViewerColumn(tblAttribute, SWT.NONE);
		col.setLabelProvider(new AttributeLabelProvider());
		tlayout.setColumnData(col.getColumn(), new ColumnWeightData(1));
		
		tblAttribute.setInput(new String[] {DialogConstants.LOADING_TEXT});
		
		Composite buttonPanel = new Composite(parent, SWT.NONE);
		buttonPanel.setLayout(new GridLayout());
		buttonPanel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		((GridLayout)buttonPanel.getLayout()).marginWidth = 0;
		((GridLayout)buttonPanel.getLayout()).marginHeight = 0;
		
		Button btnAdd = new Button(buttonPanel, SWT.PUSH);
		btnAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		btnAdd.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnAdd.setText((DialogConstants.ADD_BUTTON_TEXT));
		btnAdd.addListener(SWT.Selection, a->addAttribute(type));
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Button btnEdit = new Button(buttonPanel, SWT.PUSH);
		btnEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		btnEdit.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		btnEdit.addListener(SWT.Selection, a->editAttribute(type));
		btnEdit.setEnabled(false);
		btnEdit.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Button btnDelete = new Button(buttonPanel, SWT.PUSH);
		btnDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		btnDelete.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		btnDelete.addListener(SWT.Selection, a->removeAttributes(type));
		btnDelete.setEnabled(false);
		btnDelete.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		l = new Label(buttonPanel, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnMoveUp = new Button(buttonPanel, SWT.PUSH);
		btnMoveUp.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnMoveUp.setText(Messages.StationPropertiesDialog_MoveUp);
		btnMoveUp.addListener(SWT.Selection, a->moveAttribute(type, SWT.DOWN));
		btnMoveUp.setEnabled(false);
		btnMoveUp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Button btnMoveDown = new Button(buttonPanel, SWT.PUSH);
		btnMoveDown.setBackground(buttonPanel.getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
		btnMoveDown.setText(Messages.StationPropertiesDialog_MoveDown);
		btnMoveDown.addListener(SWT.Selection, a->moveAttribute(type, SWT.UP));
		btnMoveDown.setEnabled(false);
		btnMoveDown.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Menu mnu = new Menu(tblAttribute.getControl());
		MenuItem mnuAdd = new MenuItem(mnu, SWT.PUSH);
		mnuAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
		mnuAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		mnuAdd.addListener(SWT.Selection, e->addAttribute(type));
		
		MenuItem mnuEdit = new MenuItem(mnu, SWT.PUSH);
		mnuEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		mnuEdit.addListener(SWT.Selection, e->editAttribute(type));
		
		MenuItem mnuDelete = new MenuItem(mnu, SWT.PUSH);
		mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
		mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		mnuDelete.addListener(SWT.Selection, e->removeAttributes(type));
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		MenuItem mnuUp = new MenuItem(mnu, SWT.PUSH);
		mnuUp.setText(Messages.StationPropertiesDialog_MoveUp);
		mnuUp.addListener(SWT.Selection, e->moveAttribute(type, SWT.DOWN));
		
		MenuItem mnuDown = new MenuItem(mnu, SWT.PUSH);
		mnuDown.setText(Messages.StationPropertiesDialog_MoveDown);
		mnuDown.addListener(SWT.Selection, e->moveAttribute(type, SWT.UP));
		
		mnu.addMenuListener(new MenuListener() {
			
			@Override
			public void menuShown(MenuEvent e) {
				boolean hasSelection = !((IStructuredSelection)tblAttribute.getSelection()).isEmpty();
				mnuEdit.setEnabled(hasSelection);
				mnuDelete.setEnabled(hasSelection);
				mnuUp.setEnabled(hasSelection);
				mnuDown.setEnabled(hasSelection);
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});
		tblAttribute.getControl().setMenu(mnu);
		
		tblAttribute.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				boolean hasSelection = !((IStructuredSelection)tblAttribute.getSelection()).isEmpty();
				btnEdit.setEnabled(hasSelection);
				btnDelete.setEnabled(hasSelection);
				btnMoveUp.setEnabled(hasSelection);
				btnMoveDown.setEnabled(hasSelection);
				
			}
		});
		return tblAttribute;
	}
	
	private void initTable() {
		deletedStationAttributes = new ArrayList<>();
		deletedLocationAttributes = new ArrayList<>();
		Job j = new Job(Messages.StationPropertiesDialog_loadingStationJobName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<AssetStationAttribute> items = new ArrayList<>();
				List<AssetStationLocationAttribute> locationitems = new ArrayList<>();
				try(Session session = HibernateManager.openSession()){
					String hsql = "FROM AssetStationAttribute a WHERE a.attribute.conservationArea = :ca ORDER BY a.order"; //$NON-NLS-1$
					Query<AssetStationAttribute> q = session.createQuery(hsql, AssetStationAttribute.class);
					q.setParameter("ca",  SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
					items.addAll(q.getResultList());
					items.forEach(a->{
						a.getAttribute().getName();
					});
					
					hsql = "FROM AssetStationLocationAttribute a WHERE a.attribute.conservationArea = :ca ORDER BY a.order"; //$NON-NLS-1$
					Query<AssetStationLocationAttribute> q2 = session.createQuery(hsql, AssetStationLocationAttribute.class);
					q2.setParameter("ca",  SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
					locationitems.addAll(q2.getResultList());
					locationitems.forEach(a->a.getAttribute().getName());
				}
				stationAttributes = items;
				locationAttributes = locationitems;
				
				Display.getDefault().syncExec(()->{
					tblStationAttribute.setInput(stationAttributes);
					tblLocationAttribute.setInput(locationAttributes);
					
				});
				
				return Status.OK_STATUS;
			}
			
		};
		j.setSystem(true);
		j.schedule();
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void moveAttribute(Type type, int direction){
		TableViewer tblViewer = null;
		List attributes = null;
		if (type == Type.STATION) {
			tblViewer = tblStationAttribute;
			attributes = stationAttributes;
		}else if (type == Type.LOCATION) {
			tblViewer = tblLocationAttribute;
			attributes = locationAttributes;
		}else {
			return;
		}		
		
		for (Iterator<?> iterator = tblViewer.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object toMove = iterator.next();
		
			int index = attributes.indexOf(toMove);
			if (direction == SWT.UP){
				index ++;
				if(index >= attributes.size()){
					index = attributes.size() - 1;
				}
			}else if (direction == SWT.DOWN){
				index --;
				if(index < 0) index = 0;
			}
			attributes.remove(toMove);
			attributes.add(index, toMove);
		}
		modified();
		tblViewer.refresh();
	}
	
	private void addAttribute( Type type ){
		SelectAttributeDialog dialog = new SelectAttributeDialog(getShell(), Messages.StationPropertiesDialog_AddStationAttributes);
		ContextInjectionFactory.inject(dialog, context);
		if (dialog.open() == Window.OK){
			for (AssetAttribute ia : dialog.getSelectedAttributes()){
				if (type == Type.STATION) {
					AssetStationAttribute a  = new AssetStationAttribute();
					a.setAttribute(ia);
					if (!stationAttributes.contains(a)) stationAttributes.add(a);
				}else if (type == Type.LOCATION) {
					AssetStationLocationAttribute a  = new AssetStationLocationAttribute();
					a.setAttribute(ia);
					if (!locationAttributes.contains(a)) locationAttributes.add(a);
				}
			}
			if (type == Type.STATION)
				tblStationAttribute.refresh();
			else if (type == Type.LOCATION) {
				tblLocationAttribute.refresh();
			}
			modified();
		}
	}
	
	private void editAttribute( Type type ){
		TableViewer viewer = null;
		if (type == Type.STATION) {
			viewer = tblStationAttribute;
		}else if (type == Type.LOCATION) {
			viewer = tblLocationAttribute;
		}else {
			return;
		}
		
		Object x = viewer.getStructuredSelection().getFirstElement();
		if (x == null) return;
		
		AssetAttribute a = null;
		if (x instanceof AssetStationAttribute){
			a = ((AssetStationAttribute)x).getAttribute();
		}else if (x instanceof AssetStationLocationAttribute) {
			a = ((AssetStationLocationAttribute)x).getAttribute();
		}
		if (a == null) return;
		
		AttributeDialog.showAttributeDialog(getShell(), a, context); 
		//refresh
		viewer.refresh();
		modified();
	}

	private void removeAttributes( Type type ){
		if (type == Type.STATION) removeStationAttributes();
		if (type == Type.LOCATION) removeLocationAttributes();
	}
	
	private void removeStationAttributes( ){
		IStructuredSelection items = (IStructuredSelection)tblStationAttribute.getSelection();
		final List<AssetStationAttribute> toDelete = new ArrayList<>();
		
		for (Iterator<?> iterator = items.iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof AssetStationAttribute){
				toDelete.add((AssetStationAttribute)x);
			}
		}
		
		final List<String> warnings = new ArrayList<String>();
		final List<AssetStationAttribute> aToDelete = new ArrayList<>();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress(){
	
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					try(Session session = HibernateManager.openSession()){

						for (AssetStationAttribute x : toDelete){
							try{
								DeleteManager.canDelete(x, session);
								aToDelete.add(x);
							}catch (Exception ex){
								warnings.add(MessageFormat.format(Messages.StationPropertiesDialog_CannotRemoveAttribute, x.getAttribute().getName(), ex.getMessage()));
							}
						}
					}
				}
				
			});
		}catch (Exception ex){
			AssetPlugIn.log(ex.getMessage(), ex);
			warnings.add(ex.getMessage());
		}
		if(!warnings.isEmpty()){
			WarningDialog wd = new WarningDialog(getShell(), Messages.StationPropertiesDialog_WarningTitle, Messages.StationPropertiesDialog_WarningsMessage, warnings);
			wd.open();
		}
		
		if (aToDelete.size() > 0 ){
			StringBuilder sb = new StringBuilder();
			for (AssetStationAttribute d: aToDelete){
				sb.append(d.getAttribute().getName());
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			if (MessageDialog.openConfirm(getShell(), Messages.StationPropertiesDialog_RemoveAttributeTitle, MessageFormat.format(Messages.StationPropertiesDialog_RemoveAttributeTitleMsg1, sb.toString()))){
				stationAttributes.removeAll(aToDelete);
				deletedStationAttributes.addAll(aToDelete);
			}
		}
		
		tblStationAttribute.refresh();
		modified();
	}
	
	private void removeLocationAttributes(  ){
		IStructuredSelection items = (IStructuredSelection)tblLocationAttribute.getSelection();
		final List<AssetStationLocationAttribute> toDelete = new ArrayList<>();
		
		for (Iterator<?> iterator = items.iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof AssetStationLocationAttribute){
				toDelete.add((AssetStationLocationAttribute)x);
			}
		}
		
		final List<String> warnings = new ArrayList<String>();
		final List<AssetStationLocationAttribute> aToDelete = new ArrayList<>();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(getShell());
		try{
			pmd.run(true, false, new IRunnableWithProgress(){
	
				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					try(Session session = HibernateManager.openSession()){

						for (AssetStationLocationAttribute x : toDelete){
							try{
								DeleteManager.canDelete(x, session);
								aToDelete.add(x);
							}catch (Exception ex){
								warnings.add(MessageFormat.format(Messages.StationPropertiesDialog_CannotRemoveAttribute, x.getAttribute().getName(), ex.getMessage()));
							}
						}
					}
				}
				
			});
		}catch (Exception ex){
			AssetPlugIn.log(ex.getMessage(), ex);
			warnings.add(ex.getMessage());
		}
		if(!warnings.isEmpty()){
			WarningDialog wd = new WarningDialog(getShell(), Messages.StationPropertiesDialog_WarningTitle, Messages.StationPropertiesDialog_WarningsMessage, warnings);
			wd.open();
		}
		
		if (aToDelete.size() > 0 ){
			StringBuilder sb = new StringBuilder();
			for (AssetStationLocationAttribute d: aToDelete){
				sb.append(d.getAttribute().getName());
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			if (MessageDialog.openConfirm(getShell(), Messages.StationPropertiesDialog_RemoveAttributeTitle, MessageFormat.format(Messages.StationPropertiesDialog_RemoveAttributeTitleMsg2, sb.toString()))){
				locationAttributes.removeAll(aToDelete);
				deletedLocationAttributes.addAll(aToDelete);
			}
		}
		
		tblLocationAttribute.refresh();
		modified();
	}
	
}