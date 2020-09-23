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
package org.wcs.smart.asset.ui.views.stationlocation;

import java.text.Collator;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.AssetSecurityManager;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetHistoryRecord;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetStationLocationHistoryRecord;
import org.wcs.smart.asset.ui.CommentDialog;
import org.wcs.smart.asset.ui.DateCommentDialog;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.ui.properties.DialogConstants;
/**
 * Station location even page for station location editor
 * @author Emily
 *
 */
public class StationLocationEventPage {

	private StationLocationEditor parentEditor;
	
	private TableViewer tblEvents;
	
	private List<AssetStationLocationHistoryRecord> activeHistoryRecords;
	
	public StationLocationEventPage(StationLocationEditor editor) {
		this.parentEditor = editor;
	}
	
	
	public void createControl(Composite parent, FormToolkit toolkit) {
		Composite panel = toolkit.createComposite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		ToolBar historyToolbar = null;
		if (AssetSecurityManager.INSTANCE.canEditStationLocationHistory()) {
			historyToolbar = new ToolBar(panel, SWT.FLAT | SWT.HORIZONTAL);
			historyToolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		}
		
		tblEvents = new TableViewer(panel, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);
		tblEvents.setContentProvider(ArrayContentProvider.getInstance());
		tblEvents.setInput(new String[] {DialogConstants.LOADING_TEXT});
		tblEvents.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblEvents.getTable().setHeaderVisible(true);
		tblEvents.getTable().setLinesVisible(true);
		
		TableViewerColumn col = new TableViewerColumn(tblEvents, SWT.NONE);
		col.getColumn().setText(Messages.StationLocationEventPage_DateColumnName);
		col.getColumn().setWidth(150);
		col.getColumn().setResizable(true);
		col.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof AssetStationLocationHistoryRecord)
					return ((AssetStationLocationHistoryRecord)element).getDate().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));

				return super.getText(element);
			}
		});
		col.getColumn().addListener(SWT.Selection, e->{
			if (col.getColumn().equals(tblEvents.getTable().getSortColumn())) {
				int dir = tblEvents.getTable().getSortDirection();
				tblEvents.getTable().setSortDirection(dir == SWT.UP ? SWT.DOWN : SWT.UP);
			}else {
				tblEvents.getTable().setSortColumn(col.getColumn());
			}
			tblEvents.refresh();
		});
		
		TableViewerColumn col2 = new TableViewerColumn(tblEvents, SWT.NONE);
		col2.getColumn().setText(Messages.StationLocationEventPage_CommentColumnName);
		col2.getColumn().setWidth(150);
		col2.getColumn().setResizable(true);
		col2.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof AssetStationLocationHistoryRecord) return ((AssetStationLocationHistoryRecord) element).getComment();
				return super.getText(element);
			}
		});
		col2.getColumn().setWidth(tblEvents.getControl().getSize().x - col.getColumn().getWidth());
		col2.getColumn().addListener(SWT.Selection, e->{
			if (col2.getColumn().equals(tblEvents.getTable().getSortColumn())) {
				int dir = tblEvents.getTable().getSortDirection();
				tblEvents.getTable().setSortDirection(dir == SWT.UP ? SWT.DOWN : SWT.UP);
			}else {
				tblEvents.getTable().setSortColumn(col2.getColumn());
			}
			tblEvents.refresh();
		});
		
		tblEvents.getControl().addListener(SWT.Resize, e->col2.getColumn().setWidth(tblEvents.getControl().getSize().x - col.getColumn().getWidth()));
		
		tblEvents.getTable().setSortDirection(SWT.DOWN);
		tblEvents.getTable().setSortColumn(col.getColumn());
		tblEvents.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				if (e1 instanceof AssetStationLocationHistoryRecord && e2 instanceof AssetStationLocationHistoryRecord) {
					if (tblEvents.getTable().getSortColumn() == col.getColumn()) {
						return (tblEvents.getTable().getSortDirection() == SWT.UP ? 1 : -1) * ((AssetStationLocationHistoryRecord)e1).getDate().compareTo(((AssetStationLocationHistoryRecord)e2).getDate());
					}else if (tblEvents.getTable().getSortColumn() == col2.getColumn()){
						return (tblEvents.getTable().getSortDirection() == SWT.UP ? 1 : -1) * Collator.getInstance().compare( ((AssetStationLocationHistoryRecord)e1).getComment(), ((AssetStationLocationHistoryRecord)e2).getComment());
					}
				}
				return super.compare(viewer, e1, e2);
			}
		});
		
		if (historyToolbar != null) {
			ToolItem deleteItem = new ToolItem(historyToolbar,SWT.PUSH);
			deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			deleteItem.setToolTipText(Messages.StationLocationEventPage_deletetooltip);
			deleteItem.addListener(SWT.Selection, e->deleteHistoryRecords());
			deleteItem.setEnabled(false);
			
			ToolItem editItem = new ToolItem(historyToolbar,SWT.PUSH);
			editItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
			editItem.setToolTipText(Messages.StationLocationEventPage_edittooltip);
			editItem.addListener(SWT.Selection, e->editHistoryRecord());
			editItem.setEnabled(false);
	
			ToolItem addItem = new ToolItem(historyToolbar,SWT.PUSH);
			addItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			addItem.setToolTipText(Messages.StationLocationEventPage_createtooltip);
			addItem.addListener(SWT.Selection, e->addHistoryRecord());
			
			tblEvents.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					deleteItem.setEnabled(!tblEvents.getSelection().isEmpty());
					editItem.setEnabled(!tblEvents.getSelection().isEmpty());
				}
			});
		}
		if (AssetSecurityManager.INSTANCE.canEditStationLocationHistory()) {
			Menu mnu = new Menu(tblEvents.getControl());
			
			MenuItem mnuAdd = new MenuItem(mnu, SWT.PUSH);
			mnuAdd.setText(DialogConstants.ADD_BUTTON_TEXT);
			mnuAdd.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
			mnuAdd.addListener(SWT.Selection, e->addHistoryRecord());
			
			MenuItem mnuEdit = new MenuItem(mnu, SWT.PUSH);
			mnuEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
			mnuEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
			mnuEdit.addListener(SWT.Selection, e->editHistoryRecord());
			
			MenuItem mnuDelete = new MenuItem(mnu, SWT.PUSH);
			mnuDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
			mnuDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			mnuDelete.addListener(SWT.Selection, e->deleteHistoryRecords());
			
			mnu.addMenuListener(new MenuListener() {
				@Override
				public void menuShown(MenuEvent e) {
					mnuDelete.setEnabled(!tblEvents.getSelection().isEmpty());
					mnuEdit.setEnabled(!tblEvents.getSelection().isEmpty());
				}
				
				@Override
				public void menuHidden(MenuEvent e) {}
			});

			tblEvents.getControl().setMenu(mnu);
		}
	}
		
	
	private void addHistoryRecord() {
		DateCommentDialog dialog = new DateCommentDialog(parentEditor.getSite().getShell(), Messages.StationLocationEventPage_NewHistoryTitle,
				Messages.StationLocationEventPage_NewHistoryMsg);
		if (dialog.open() != CommentDialog.OK) return;
		
		AssetStationLocationHistoryRecord record = new AssetStationLocationHistoryRecord();
		record.setDate(dialog.getSelectedDateTime());
		record.setStationLocation(parentEditor.getAssetStationLocation());
		record.setComment(dialog.getComment());
		
		try (Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				session.saveOrUpdate(record);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.log(Messages.StationLocationEventPage_NewHistorySaveError + ex.getMessage(), ex);
				return;
			}
		}
		activeHistoryRecords.add(record);
		tblEvents.refresh();	
	}
	
	private void editHistoryRecord() {
		Object x = ((IStructuredSelection)tblEvents.getSelection()).getFirstElement();
		if (!(x instanceof AssetStationLocationHistoryRecord)) return;
		AssetStationLocationHistoryRecord toEdit = (AssetStationLocationHistoryRecord)x;
		
		DateCommentDialog dialog = new DateCommentDialog(parentEditor.getSite().getShell(), Messages.StationLocationEventPage_NewLocationHistoryTitle,
				Messages.StationLocationEventPage_NewLocationHistoryMsg);
		dialog.setValues(toEdit.getDate(), toEdit.getComment());
		
		if (dialog.open() != CommentDialog.OK) return;
		
		try (Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				toEdit.setDate(dialog.getSelectedDateTime());
				toEdit.setComment(dialog.getComment());
				session.saveOrUpdate(toEdit);
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.log(Messages.StationLocationEventPage_NewLocationHistoryError + ex.getMessage(), ex);
				return;
			}
		}
		tblEvents.refresh();	
	}
	
	private void deleteHistoryRecords() {
		if (tblEvents == null) return;
		List<AssetStationLocationHistoryRecord> toDelete = new ArrayList<>();
		for (Iterator<?> iterator = ((IStructuredSelection)tblEvents.getSelection()).iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof AssetStationLocationHistoryRecord) {
				toDelete.add((AssetStationLocationHistoryRecord)x);			
			}
		}
		if (toDelete.isEmpty()) return;
		
		if (!MessageDialog.openQuestion(parentEditor.getSite().getShell(), Messages.StationLocationEventPage_DeleteTitle, 
				MessageFormat.format(Messages.StationLocationEventPage_DeleteMsg, toDelete.size()))){
			return;
		}
		try (Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				toDelete.forEach(x->{
					session.delete(x);
				});
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				AssetPlugIn.log(Messages.StationLocationEventPage_DeleteError + ex.getMessage(), ex);
				return;
			}
		}
		activeHistoryRecords.removeAll(toDelete);
		tblEvents.refresh();
	}
	
	public void initialize(AssetStationLocation location) {
		activeHistoryRecords = new ArrayList<>();
		Job j = new Job(Messages.StationLocationEventPage_loadJobName) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (location.getUuid() != null) {
					try(Session s = HibernateManager.openSession()){
						activeHistoryRecords.addAll(
								QueryFactory.buildQuery(s, AssetStationLocationHistoryRecord.class, "stationLocation", location).list()); //$NON-NLS-1$
					}
				}
				Display.getDefault().syncExec(()->{
					tblEvents.setInput(activeHistoryRecords);
				});
				
				return Status.OK_STATUS;
			}
		};
		j.schedule();
	}
	
}
