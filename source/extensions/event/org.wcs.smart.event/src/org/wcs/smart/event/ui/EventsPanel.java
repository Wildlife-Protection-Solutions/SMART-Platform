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
package org.wcs.smart.event.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
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
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.event.ActionTypeManager;
import org.wcs.smart.event.EventPlugIn;
import org.wcs.smart.event.filter.ParsedFilter;
import org.wcs.smart.event.internal.Messages;
import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.EActionEvent;
import org.wcs.smart.event.model.EActionParameterValue;
import org.wcs.smart.event.model.EFilter;
import org.wcs.smart.event.model.IActionParameter;
import org.wcs.smart.event.model.IActionType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Configure dialog events panel
 * 
 * @author Emily
 *
 */
public class EventsPanel extends Composite {

	private TableViewer tblEvents;
	private Composite detailsSection ;
	
	public EventsPanel(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout());
		
		Composite main = new Composite(this, SWT.NONE);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = new Label(main, SWT.NONE);
		l.setText(Messages.EventsPanel_PanelMsg);
		
		SashForm sashForm = new SashForm(main, SWT.NONE);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftPart = new Composite(sashForm, SWT.BORDER);
		leftPart.setLayout(new GridLayout());
		((GridLayout)leftPart.getLayout()).marginWidth = 0;
		((GridLayout)leftPart.getLayout()).marginHeight = 0;
		
		tblEvents = new TableViewer(leftPart, SWT.FULL_SELECTION);
		tblEvents.setContentProvider(ArrayContentProvider.getInstance());
		tblEvents.getTable().setLinesVisible(false);
		tblEvents.getTable().setHeaderVisible(true);
		tblEvents.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TableViewerColumn stateColumn = new TableViewerColumn(tblEvents, SWT.NONE);
		stateColumn.getColumn().setText(Messages.EventsPanel_StateColumn);
		stateColumn.getColumn().setWidth(32);
		stateColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return null;
			}
			public Image getImage(Object element) {
				if (element instanceof EActionEvent) {
					boolean isEnabled = ((EActionEvent) element).isEnabled();
					if (!isEnabled) {
						return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLED_ICON);
					}else {
						return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLED_ICON );
					}
				}
				return super.getImage(element);
			}
			
			@Override
			public Color getForeground(Object element) {
				if (element instanceof EActionEvent) {
					boolean isEnabled = ((EActionEvent) element).isEnabled();
					if (!isEnabled) return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
			}
		});
		
		TableViewerColumn filterColumn = new TableViewerColumn(tblEvents, SWT.NONE);
		filterColumn.getColumn().setText(Messages.EventsPanel_FilterColumn);
		filterColumn.getColumn().setWidth(150);
		filterColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof EActionEvent) return ((EActionEvent) element).getFilter().getId();
				return super.getText(element);
			}
			public Image getImage(Object element) {
				if (element instanceof EActionEvent) return EventPlugIn.getDefault().getImageRegistry().get(EventPlugIn.ICON_FILTER);
				return super.getImage(element);
			}
			@Override
			public Color getForeground(Object element) {
				if (element instanceof EActionEvent) {
					boolean isEnabled = ((EActionEvent) element).isEnabled();
					if (!isEnabled) return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
			}
		});
		TableViewerColumn actionColumn = new TableViewerColumn(tblEvents, SWT.NONE);
		actionColumn.getColumn().setText(Messages.EventsPanel_ActionColumn);
		actionColumn.getColumn().setWidth(150);
		actionColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof EActionEvent) return ((EActionEvent) element).getAction().getId();
				return super.getText(element);
			}
			public Image getImage(Object element) {
				if (element instanceof EActionEvent) return EventPlugIn.getDefault().getImageRegistry().get(EventPlugIn.ICON_ACTION);
				return super.getImage(element);
			}
			@Override
			public Color getForeground(Object element) {
				if (element instanceof EActionEvent) {
					boolean isEnabled = ((EActionEvent) element).isEnabled();
					if (!isEnabled) return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
				}
				return null;
			}
		});
		tblEvents.addSelectionChangedListener(e->updateDetails());
		
		Menu mnu = new Menu(tblEvents.getControl());
		
		MenuItem addItem = new MenuItem(mnu, SWT.PUSH);
		addItem.setText(DialogConstants.ADD_BUTTON_TEXT);
		addItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		addItem.addListener(SWT.Selection, e->addEvent());
		
		new MenuItem(mnu, SWT.SEPARATOR);

		MenuItem enableItem = new MenuItem(mnu, SWT.PUSH);
		enableItem.setText(Messages.EventsPanel_EnableBtn);
		enableItem.addListener(SWT.Selection, e->changeEventState());
		
		new MenuItem(mnu, SWT.SEPARATOR);
		
		MenuItem deleteItem = new MenuItem(mnu, SWT.PUSH);
		deleteItem.setText(DialogConstants.DELETE_BUTTON_TEXT);
		deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteItem.addListener(SWT.Selection, e->deleteEvent());
		deleteItem.setEnabled(false);
		tblEvents.addSelectionChangedListener(e->{
			deleteItem.setEnabled(!tblEvents.getSelection().isEmpty());
			
			Object x = tblEvents.getStructuredSelection().getFirstElement();
			if (x instanceof EActionEvent) {
				if (((EActionEvent) x).isEnabled()) {
					enableItem.setText(Messages.EventsPanel_DisableBtn);
				}else {
					enableItem.setText(Messages.EventsPanel_EnableBtn);
				}
				enableItem.setEnabled(true);
			}else {
				enableItem.setEnabled(false);
			}
		});
		tblEvents.getControl().setMenu(mnu);
		
		Composite rightPart = new Composite(sashForm, SWT.BORDER);
		rightPart.setLayout(new GridLayout());
		
		detailsSection = new Composite(rightPart, SWT.NONE);
		detailsSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		detailsSection.setLayout(new GridLayout());
		((GridLayout)detailsSection.getLayout()).marginWidth = 0;
		((GridLayout)detailsSection.getLayout()).marginHeight = 0;
		detailsSection.setBackground(getShell().getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		updateDetails();
		
		loadDataJob.schedule();
	}
	
	public void refresh() {
		loadDataJob.schedule();
	}
	
	private void changeEventState() {
		Object x = tblEvents.getStructuredSelection().getFirstElement();
		if (x instanceof EActionEvent) {
			EActionEvent evt = (EActionEvent)x;
			evt.setEnabled(!evt.isEnabled());
			
			try(Session session = HibernateManager.openSession()){
				session.beginTransaction();
				try {
					HibernateManager.saveOrMerge(session, evt);
					session.getTransaction().commit();
				}catch (Exception ex) {
					session.getTransaction().rollback();
					EventPlugIn.displayLog(Messages.EventsPanel_UpdateStateError + ex.getMessage(), ex);
				}
			}
			tblEvents.refresh();
			tblEvents.setSelection(tblEvents.getSelection());
		}
	}
	private void updateDetails() {
		for (Control kid : detailsSection.getChildren()) kid.dispose();
		
		Object x = tblEvents.getStructuredSelection().getFirstElement();
		if (x == null || !(x instanceof EActionEvent)) {
			createToolbar(detailsSection, null);
			return;
		}
		EActionEvent actionEvent = (EActionEvent)x;
		
		Composite toolbarComp = new Composite(detailsSection, SWT.NONE);
		toolbarComp.setLayout(new GridLayout(2, false));
		toolbarComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		toolbarComp.setBackground(detailsSection.getBackground());
		((GridLayout)toolbarComp.getLayout()).marginWidth = 0;
		((GridLayout)toolbarComp.getLayout()).marginHeight = 0;
		
		Label ll = new Label(toolbarComp, SWT.NONE);
		if (actionEvent.isEnabled()) {
			ll.setText(Messages.EventsPanel_EnabledLabel);
		}else {
			ll.setText(Messages.EventsPanel_DisabledLabel);
		}
		ll.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		ll.setBackground(toolbarComp.getBackground());
		
		createToolbar(toolbarComp, actionEvent);
		
		ScrolledComposite scroll = new ScrolledComposite(detailsSection, SWT.V_SCROLL );
		scroll.setBackground(detailsSection.getBackground());
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite content = new Composite(scroll, SWT.NONE);
		content.setBackground(detailsSection.getBackground());
		content.setLayout(new GridLayout());
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		scroll.setContent(content);
		
		Label l = new Label(content, SWT.WRAP);
		l.setText(actionEvent.getAction().getId());
		l.setBackground(detailsSection.getBackground());
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		Font boldFont2 = new Font(l.getDisplay(), fd);
		l.addListener(SWT.Dispose, e->boldFont2.dispose());
		l.setFont(boldFont2);
		
		IActionType actionType = ActionTypeManager.INSTANCE.getActionType( actionEvent.getAction().getActionTypeKey() );
		
		l = new Label(content, SWT.WRAP);
		l.setText(MessageFormat.format(Messages.EventsPanel_ActionTypeLabel, actionType.getName(Locale.getDefault())));
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		l.setBackground(detailsSection.getBackground());
		
		for (IActionParameter parameter: actionType.getActionParameters()) {
			StringBuilder sb = new StringBuilder();
			sb.append(parameter.getName(Locale.getDefault()));
			sb.append(": "); //$NON-NLS-1$
			
			for (EActionParameterValue vv : actionEvent.getAction().getParameters()) {
				if (vv.getId().getParameterKey().equals(parameter.getKey())) {
					sb.append(vv.getParameterValue());
				}
			}
			
			l = new Label(content, SWT.WRAP);
			l.setText(sb.toString());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			l.setBackground(detailsSection.getBackground());
			
		}
		
		l = new Label(content, SWT.NONE);
		
		l = new Label(content, SWT.WRAP);
		l.setText(actionEvent.getFilter().getId());
		l.setBackground(detailsSection.getBackground());
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		l.setFont(boldFont2);
		
		try {
			ParsedFilter pFilter = actionEvent.getFilter().getParsedFilter();
			
			StringBuilder sb = new StringBuilder();
			if (pFilter.getSources() == null) {
				sb.append(Messages.EventsPanel_AllLabel);
			}else {
				pFilter.getSources().forEach(s->{
					sb.append(s.getName(Locale.getDefault()));
					sb.append(", "); //$NON-NLS-1$
				});
				sb.deleteCharAt(sb.length() - 1);
				sb.deleteCharAt(sb.length() - 1);
			}
			
			l = new Label(content, SWT.WRAP);
			l.setText(MessageFormat.format(Messages.EventsPanel_SourcesLabel, sb.toString()));
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			l.setBackground(detailsSection.getBackground());
			
			
			l = new Label(content, SWT.WRAP);
			l.setText(MessageFormat.format(Messages.EventsPanel_FilterLabel, pFilter.getFilter() != null ? pFilter.getFilter().asString() : "")); //$NON-NLS-1$
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			l.setBackground(detailsSection.getBackground());
			
		}catch(Exception ex) {
			EventPlugIn.log(ex.getMessage(), ex);
			
			l = new Label(content, SWT.WRAP);
			l.setText(MessageFormat.format(Messages.EventsPanel_ParseError, ex.getMessage()));
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			l.setBackground(detailsSection.getBackground());
		}
		
		detailsSection.layout(true);
		content.setSize(content.computeSize(scroll.getSize().x-20, SWT.DEFAULT));
		scroll.addListener(SWT.Resize, e->{
			content.setSize(content.computeSize(scroll.getSize().x-20, SWT.DEFAULT));	
		});
		
	}
	
	private void createToolbar(Composite parent, EActionEvent action) {
		ToolBar tb = new ToolBar(parent,  SWT.FLAT);
		tb.setBackground(parent.getBackground());
		
		ToolItem adddiableItem = new ToolItem(tb, SWT.PUSH);
		
		if (action == null || action.isEnabled()) {
			adddiableItem.setToolTipText(Messages.EventsPanel_disableTooltip);
			adddiableItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DISABLED_ICON));
		}else {
			adddiableItem.setToolTipText(Messages.EventsPanel_enableTooltip);
			adddiableItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ENABLED_ICON));
		}
		
		adddiableItem.addListener(SWT.Selection, e->changeEventState());
		adddiableItem.setEnabled(action != null);
		
		ToolItem addItem = new ToolItem(tb, SWT.PUSH);
		addItem.setToolTipText(Messages.EventsPanel_createTooltip);
		addItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		addItem.addListener(SWT.Selection, e->addEvent());
		
		ToolItem deleteItem = new ToolItem(tb, SWT.PUSH);
		deleteItem.setToolTipText(Messages.EventsPanel_deleteTooltip);
		deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteItem.addListener(SWT.Selection, e->deleteEvent());
		deleteItem.setEnabled(action != null);
		
		tb.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
	}
	
	private void addEvent() {
		NewActionEventDialog newAction = new NewActionEventDialog(getShell());
		if (newAction.open() == NewActionEventDialog.OK) {
			loadDataJob.schedule();
		}
	}
	
	private void deleteEvent() {
		List<EActionEvent> toDelete = new ArrayList<>();
		for (Iterator<?> iterator = tblEvents.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object eActionEvent = iterator.next();
			if (eActionEvent instanceof EActionEvent) toDelete.add((EActionEvent) eActionEvent);
		}
		if (toDelete.isEmpty()) return;
		
		if (!MessageDialog.openQuestion(getShell(), Messages.EventsPanel_DeleteTitle, MessageFormat.format(Messages.EventsPanel_DeleteMsg, toDelete.size()))) {
			return;
		}
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for(EActionEvent event : toDelete) {
					HibernateManager.saveOrMerge(session, event);
				}
				session.getTransaction().commit();
				((List<?>)tblEvents.getInput()).removeAll(toDelete);
				
			}catch (Exception ex) {
				session.getTransaction().rollback();
				EventPlugIn.log(Messages.EventsPanel_DeleteError, ex);
			}
		}
		tblEvents.refresh();
	}
	
	
	private Job loadDataJob = new Job(Messages.EventsPanel_LoadingJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<EActionEvent> events = new ArrayList<>();
			
			try(Session session = HibernateManager.openSession()){
				events.addAll(QueryFactory.buildQuery(session, EActionEvent.class, new Object[] {"action.conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
				events.forEach(e->{
					e.getAction().getId();
					e.getFilter().getId();
					e.getAction().getParameters().forEach(p->p.getId().getParameterKey());
				});
			}
			events.sort((a,b)->{
				if (a.isEnabled() && !b.isEnabled()) return -1;
				if (!a.isEnabled() && b.isEnabled()) return 1;
				
				if (a.getAction().getId().equals(b.getAction().getId())) {
					return a.getFilter().getId().compareTo(b.getFilter().getId());
				}
				return a.getAction().getId().compareTo(b.getAction().getId());
			});
			Display.getDefault().syncExec(()->{
				if(tblEvents == null || tblEvents.getControl().isDisposed()) return;
				tblEvents.setInput(events);
			});
			return Status.OK_STATUS;
		}
		
	};

	private class NewActionEventDialog extends SmartStyledTitleDialog{
	
		private ComboViewer cmbAction;
		private ComboViewer cmbFilter;
			
		public NewActionEventDialog(Shell parentShell) {
			super(parentShell);
		}

		@Override
		protected void okPressed() {
			
			EActionEvent newEvent = new EActionEvent();
			
			Object action = cmbAction.getStructuredSelection().getFirstElement();
			Object filter = cmbFilter.getStructuredSelection().getFirstElement();
			
			if (action == null || filter == null) return;
			if (!(action instanceof EAction )) return;
			if (!(filter instanceof EFilter )) return;
			
			newEvent.setFilter((EFilter)filter);
			newEvent.setAction((EAction)action);
			newEvent.setEnabled(true);
			
			try(Session session = HibernateManager.openSession()){
				session.beginTransaction();
				try {
					HibernateManager.saveOrMerge(session, newEvent);
					session.getTransaction().commit();
				}catch (Exception ex) {
					session.getTransaction().rollback();
					EventPlugIn.log(Messages.EventsPanel_CreateError + ex.getMessage(), ex);
					return;
				}
			}
			super.okPressed();
		}
		
		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			// create OK and Cancel buttons by default
			Button saveBtn = createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
			saveBtn.setEnabled(false);
		}
		
		@Override
		public Control createDialogArea(Composite parent) {
			parent = (Composite) super.createDialogArea(parent);
			
			Composite main = new Composite(parent, SWT.NONE);
			main.setLayout(new GridLayout());
			main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true ));
			
			Composite header = new Composite(main, SWT.NONE);
			header.setLayout(new GridLayout(2, false));
			header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridLayout)header.getLayout()).marginWidth = 0;
			((GridLayout)header.getLayout()).marginHeight = 0;
			
			Label l = new Label(header, SWT.NONE);
			l.setText(Messages.EventsPanel_FilterLabel2);
			
			cmbFilter = new ComboViewer(header, SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbFilter.setContentProvider(ArrayContentProvider.getInstance());
			cmbFilter.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			cmbFilter.setLabelProvider(new LabelProvider() {
				public String getText(Object element) {
					if (element instanceof EFilter) return ((EFilter) element).getId();
					return super.getText(element);
				}
			});
			cmbFilter.addSelectionChangedListener(e->validate());
			
			l = new Label(header, SWT.NONE);
			l.setText(Messages.EventsPanel_ActionLabel2);
			
			cmbAction = new ComboViewer(header, SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbAction.setContentProvider(ArrayContentProvider.getInstance());
			cmbAction.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			cmbAction.setLabelProvider(new LabelProvider() {
				public String getText(Object element) {
					if (element instanceof EAction) return ((EAction) element).getId();
					return super.getText(element);
				}
			});
			cmbAction.addSelectionChangedListener(e->validate());
			
			loadItems.schedule();
			
			setTitle(Messages.EventsPanel_PanelTitle);
			getShell().setText(Messages.EventsPanel_PanelTitle);
			setMessage(Messages.EventsPanel_Message);
			return parent;
		}
		
		@Override
		public boolean isResizable() {
			return true;
		}
		
		private void validate() {
			Object action = cmbAction.getStructuredSelection().getFirstElement();
			Object filter = cmbFilter.getStructuredSelection().getFirstElement();
			
			if (action == null || filter == null) {
				setError(Messages.EventsPanel_actionfilterrequired);
				return;
			}
			if (!(action instanceof EAction )) {
				setError(Messages.EventsPanel_actionrequired);
				return;
			}
			if (!(filter instanceof EFilter )) {
				setError(Messages.EventsPanel_filterrequired);
				return;
			}
			setErrorMessage(null);
			Button btn = getButton(IDialogConstants.OK_ID);
			if (btn != null) btn.setEnabled(true);
		}
		
		private void setError(String error) {
			setErrorMessage(error);
			Button btn = getButton(IDialogConstants.OK_ID);
			if (btn != null) btn.setEnabled(false);
		}
		
		private Job loadItems = new Job(Messages.EventsPanel_loadingjobname) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				List<EFilter> filters = new ArrayList<>();
				List<EAction> actions = new ArrayList<>();
				
				try(Session session = HibernateManager.openSession()){
					actions.addAll(QueryFactory.buildQuery(session, EAction.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
					filters.addAll(QueryFactory.buildQuery(session, EFilter.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
					
					actions.forEach(a->a.getId());
					filters.forEach(f->{
						f.getId();
						f.getFilterString();
					});
				}
				
				Display.getDefault().syncExec(()->{
					if (cmbFilter.getControl().isDisposed()) return;
					cmbFilter.setInput(filters);
					cmbAction.setInput(actions);
					
					if (!filters.isEmpty()) cmbFilter.setSelection(new StructuredSelection(filters.get(0)));
					if (!actions.isEmpty()) cmbAction.setSelection(new StructuredSelection(actions.get(0)));
				});
				return Status.OK_STATUS;
			}
			
		};
	}
}
