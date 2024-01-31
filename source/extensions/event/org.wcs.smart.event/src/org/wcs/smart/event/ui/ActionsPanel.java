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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.event.ActionExecutorManager;
import org.wcs.smart.event.ActionTypeManagerInternal;
import org.wcs.smart.event.EventPlugIn;
import org.wcs.smart.event.internal.Messages;
import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.EActionEvent;
import org.wcs.smart.event.model.EActionParameterValue;
import org.wcs.smart.event.model.IActionParameter;
import org.wcs.smart.event.model.IActionType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Configure events actions panel
 * 
 * @author Emily
 *
 */
public class ActionsPanel extends Composite {

	private TableViewer lstActions;
	private Composite rightPart;
	private List<Listener> modifiedListeners = new ArrayList<>();
	
	public ActionsPanel(Composite parent, int style) {
		super(parent, style);
		createComposite();
	}
	
	private void createComposite() {
		setLayout(new GridLayout());
		
		Label l = new Label(this, SWT.NONE);
		l.setText(Messages.ActionsPanel_Info);
		
		SashForm parts = new SashForm(this,  SWT.NONE);
		parts.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite leftPart = new Composite(parts, SWT.BORDER);
		leftPart.setLayout(new GridLayout());
		((GridLayout)leftPart.getLayout()).marginWidth = 0;
		((GridLayout)leftPart.getLayout()).marginHeight = 0;
		
		lstActions = new TableViewer(leftPart, SWT.V_SCROLL | SWT.NONE | SWT.FULL_SELECTION | SWT.MULTI);
		lstActions.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		lstActions.setContentProvider(ArrayContentProvider.getInstance());
		lstActions.getTable().setHeaderVisible(true);

		TableViewerColumn column1 = new TableViewerColumn(lstActions, SWT.NONE);
		column1.getColumn().setText(Messages.ActionsPanel_ActionColumnName);
		column1.getColumn().setWidth(200);
		
		column1.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof EAction) return ((EAction) element).getId();
				return super.getText(element);
			}
			public Image getImage(Object element) {
				if (element instanceof EAction) return EventPlugIn.getDefault().getImageRegistry().get(EventPlugIn.ICON_ACTION);
				return null;
			}
		});
		
		TableViewerColumn column2 = new TableViewerColumn(lstActions, SWT.NONE);
		column2.getColumn().setText(Messages.ActionsPanel_TypeColumName);
		column2.getColumn().setWidth(200);
		
		column2.setLabelProvider(new ColumnLabelProvider() {
			public String getText(Object element) {
				if (element instanceof EAction) {
					EAction action = (EAction)element;
					IActionType type = ActionTypeManagerInternal.INSTANCE.getActionType(action.getActionTypeKey());
					if (type == null) return Messages.ActionsPanel_UnknownType;
					return type.getName(Locale.getDefault());
				}
				return super.getText(element);
			}
		});
		
		lstActions.setInput(new String[] {DialogConstants.LOADING_TEXT});
		lstActions.addDoubleClickListener(e->editAction());
		
		Menu actionMenu = new Menu(lstActions.getControl());
		
		MenuItem addMnu = new MenuItem(actionMenu, SWT.PUSH);
		addMnu.setText(DialogConstants.ADD_BUTTON_TEXT);
		addMnu.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		addMnu.addListener(SWT.Selection, e->addAction());
		
		MenuItem editMnu = new MenuItem(actionMenu, SWT.PUSH);
		editMnu.setText(DialogConstants.EDIT_BUTTON_TEXT);
		editMnu.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		editMnu.addListener(SWT.Selection, e->editAction());
		
		MenuItem deleteMnu = new MenuItem(actionMenu, SWT.PUSH);
		deleteMnu.setText(DialogConstants.DELETE_BUTTON_TEXT);
		deleteMnu.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteMnu.addListener(SWT.Selection, e->deleteAction());
		
		lstActions.getControl().setMenu(actionMenu);
		
		editMnu.setEnabled(false);
		deleteMnu.setEnabled(false);
		
		lstActions.addSelectionChangedListener(e->{
			Object x = lstActions.getStructuredSelection().getFirstElement();
			boolean isSelected = (x != null && x instanceof EAction);
			editMnu.setEnabled(isSelected);
			deleteMnu.setEnabled(isSelected);
			updateDetails();
		});
		
		
		Composite rightPartOuter = new Composite(parts, SWT.BORDER);
		rightPartOuter.setLayout(new GridLayout());
		((GridLayout)rightPartOuter.getLayout()).marginWidth = 0;
		((GridLayout)rightPartOuter.getLayout()).marginHeight = 0;
		
		rightPart = new Composite(rightPartOuter, SWT.NONE);
		rightPart.setLayout(new GridLayout());
		rightPart.setBackground(parts.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		rightPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		parts.setWeights(new int[] {4, 4});
		
		updateDetails();

		loadActionsJob.schedule();
	}
	
	public void refresh() {
		loadActionsJob.schedule();
	}
	
	public void addListener(Listener listener) {
		modifiedListeners.add(listener);
	}
	
	private void fireEvents() {
		for (Listener l : modifiedListeners) {
			l.handleEvent(null);
		}
	}
	
	private void addAction() {
		NewActionDialog dialog = new NewActionDialog(getShell());
		if (dialog.open() ==  NewActionDialog.OK) {
			loadActionsJob.schedule();
			fireEvents();
		}
	}
	
	public void editAction() {
		Object x = lstActions.getStructuredSelection().getFirstElement();
		if (!(x instanceof EAction)) return;
		EAction toUpdate = (EAction)x;
		
		NewActionDialog dialog = new NewActionDialog(getShell(), toUpdate);
		if (dialog.open() == NewActionDialog.OK) {
			loadActionsJob.schedule();
			fireEvents();
		}
	}
	
	public void deleteAction() {
		List<EAction> toDelete = new ArrayList<>();
		for (Iterator<?> iterator = lstActions.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object eAction = (Object) iterator.next();
			if (eAction instanceof EAction) toDelete.add((EAction)eAction);
		}
		if (toDelete.isEmpty()) return;
		
		if (!MessageDialog.openQuestion(getShell(), Messages.ActionsPanel_DeleteTitle, MessageFormat.format(Messages.ActionsPanel_DeleteMst, toDelete.size() ))){
			return;
		}
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (EAction e : toDelete) {
					List<EActionEvent> events = QueryFactory.buildQuery(session, EActionEvent.class, new Object[] {"action", e}).list(); //$NON-NLS-1$
					events.forEach(ae->session.remove(ae));
					session.remove(e);
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				EventPlugIn.displayLog(Messages.ActionsPanel_DeleteError + ex.getMessage(), ex);
				toDelete.clear();
			}
		}
		((List<?>)lstActions.getInput()).removeAll(toDelete);
		lstActions.refresh();
		lstActions.setSelection(null);
		fireEvents();
	}
	
	private void createToolbar(Composite parent, boolean hasSelection) {
		ToolBar tb = new ToolBar(parent,  SWT.FLAT);
		tb.setBackground(parent.getBackground());
		
		ToolItem addItem = new ToolItem(tb, SWT.PUSH);
		addItem.setToolTipText(Messages.ActionsPanel_addTooltip);
		addItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
		addItem.addListener(SWT.Selection, e->addAction());
		
		ToolItem editItem = new ToolItem(tb, SWT.PUSH);
		editItem.setToolTipText(Messages.ActionsPanel_editTooltip);
		editItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		editItem.addListener(SWT.Selection, e->editAction());
		editItem.setEnabled(hasSelection);
		
		ToolItem deleteItem = new ToolItem(tb, SWT.PUSH);
		deleteItem.setToolTipText(Messages.ActionsPanel_deleteTooltip);
		deleteItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
		deleteItem.addListener(SWT.Selection, e->deleteAction());
		deleteItem.setEnabled(hasSelection);
	}
	
	private void updateDetails() {
		for (Control k : rightPart.getChildren()) k.dispose();
		Object element = lstActions.getStructuredSelection().getFirstElement();
		if (!(element instanceof EAction)) {
			
			Composite headerPart = new Composite(rightPart, SWT.NONE);
			headerPart.setLayout(new GridLayout(2, false));
			headerPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			headerPart.setBackground(rightPart.getBackground());		
			((GridLayout)headerPart.getLayout()).marginWidth = 0;
			((GridLayout)headerPart.getLayout()).marginHeight = 0;
			
			Label l = new Label(headerPart, SWT.NONE);
			l.setBackground(rightPart.getBackground());
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			FontData fd = l.getFont().getFontData()[0];
			fd.setStyle(SWT.BOLD);
			fd.setHeight(fd.getHeight() + 1);
			Font boldFont = new Font(l.getDisplay(), fd);
			l.addListener(SWT.Dispose, e->boldFont.dispose());
			l.setFont(boldFont);
			l.setBackground(rightPart.getBackground());
			
			createToolbar(headerPart, false);
			
			rightPart.layout(true);
			return;
		}
		EAction action = (EAction) element;
		IActionType actionType = ActionTypeManagerInternal.INSTANCE.getActionType(action.getActionTypeKey());
		
		Composite headerPart = new Composite(rightPart, SWT.NONE);
		headerPart.setLayout(new GridLayout(2, false));
		headerPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		headerPart.setBackground(rightPart.getBackground());		
		((GridLayout)headerPart.getLayout()).marginWidth = 0;
		((GridLayout)headerPart.getLayout()).marginHeight = 0;
		
		Label l = new Label(headerPart, SWT.NONE);
		l.setText(action.getId());
		l.setToolTipText(action.getId());
		l.setBackground(rightPart.getBackground());
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight() + 1);
		Font boldFont = new Font(l.getDisplay(), fd);
		l.addListener(SWT.Dispose, e->boldFont.dispose());
		l.setFont(boldFont);
		l.setBackground(rightPart.getBackground());
		
		createToolbar(headerPart, true);
		
		l = new Label(rightPart, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		ScrolledComposite scroll = new ScrolledComposite(rightPart, SWT.V_SCROLL );
		scroll.setBackground(rightPart.getBackground());
		scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite content = new Composite(scroll, SWT.NONE);
		content.setBackground(rightPart.getBackground());
		content.setLayout(new GridLayout());
		content.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		scroll.setContent(content);
		
		l = new Label(content, SWT.NONE);
		l.setText(Messages.ActionsPanel_TypeLabel);
		l.setBackground(rightPart.getBackground());
		fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		Font boldFont2 = new Font(l.getDisplay(), fd);
		l.addListener(SWT.Dispose, e->boldFont2.dispose());
		l.setFont(boldFont2);
		
		l = new Label(content, SWT.NONE);
		l.setBackground(rightPart.getBackground());
		if (actionType == null) {
			l.setText(Messages.ActionsPanel_TypeNotFoundError);
		}else {
			l.setText(actionType.getName(Locale.getDefault()));
		}
		
		l = new Label(content, SWT.NONE);
		
		l = new Label(content, SWT.NONE);
		l.setText(Messages.ActionsPanel_ParameterLabel);
		l.setBackground(rightPart.getBackground());
		l.setFont(boldFont2);
		
		if (actionType != null) {
			for (IActionParameter p : actionType.getActionParameters()) {
				String paramValue = ""; //$NON-NLS-1$
				for (EActionParameterValue pvalue : action.getParameters()) {
					if (pvalue.getId().getParameterKey().equalsIgnoreCase(p.getKey())) {
						paramValue = pvalue.getParameterValue();
						break;
					}
				}
				l = new Label(content, SWT.WRAP);
				l.setText(MessageFormat.format("{0}: {1}", p.getName(Locale.getDefault()), paramValue)); //$NON-NLS-1$
				l.setBackground(rightPart.getBackground());
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			}
		}
		rightPart.layout(true);
		content.setSize(content.computeSize(scroll.getSize().x-20, SWT.DEFAULT));
		scroll.addListener(SWT.Resize, e->{
			content.setSize(content.computeSize(scroll.getSize().x-20, SWT.DEFAULT));	
		});
		
	}

	private Job loadActionsJob = new Job(Messages.ActionsPanel_LoadingJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<Object> selectedItems = new ArrayList<>();
			Display.getDefault().syncExec(()->{
				for (Iterator<?> iterator = lstActions.getStructuredSelection().iterator(); iterator.hasNext();) {
					Object object = (Object) iterator.next();
					selectedItems.add(object);
				}
			});
			
			List<EAction> actions = new ArrayList<>();
			try(Session session = HibernateManager.openSession()){
				actions.addAll(QueryFactory.buildQuery(session, EAction.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list()); //$NON-NLS-1$
				actions.forEach(e->e.getParameters().forEach(pp->pp.getParameterValue()));
			}
			actions.sort((a,b)->a.getId().compareTo(b.getId()));
			Display.getDefault().syncExec(()->{
				if (lstActions.getControl().isDisposed()) return;
				lstActions.setInput(actions);
				lstActions.setSelection(new StructuredSelection(selectedItems));
			});
			return Status.OK_STATUS;
		}
		
	};
}
