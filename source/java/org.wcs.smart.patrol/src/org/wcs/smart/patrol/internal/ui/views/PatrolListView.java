/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.patrol.internal.ui.views;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.handlers.RadioState;
import org.eclipse.ui.menus.IMenuService;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.osgi.service.event.Event;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.folder.FolderCreateEditDialog;
import org.wcs.smart.common.folder.FolderTreeDragListener;
import org.wcs.smart.common.folder.FolderTreeUtils;
import org.wcs.smart.common.folder.NoneFolder;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolEventManager.EventType;
import org.wcs.smart.patrol.PatrolEventManager.IPatrolEventListener;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.views.PatrolTreeContentProvider.GroupByType;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolFolder;
import org.wcs.smart.patrol.model.PatrolType;
import org.wcs.smart.patrol.ui.OpenPatrolHandler;
import org.wcs.smart.patrol.ui.PatrolEditor;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.ui.ViewerSelectionListener;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.E3Utils;
import org.wcs.smart.util.UuidUtils;

import jakarta.persistence.Tuple;

/**
 * A viewer where users can view all patrols by a specified filter.
 *   
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class PatrolListView implements IPatrolFilteringView {

	private static final String GB_PREF_KEY = "org.wcs.smart.patrol.list.groupbykey"; //$NON-NLS-1$
	
	public static final String ID = "org.wcs.smart.patrol.ui.PatrolListView"; //$NON-NLS-1$
	
	public static final String GROUP_BY_EVENT = "PATROL/GROUPBY"; //$NON-NLS-1$
	
	private TreeViewer patrolListViewer;
	private PatrolViewFilter filter = PatrolViewFilter.newInstance();
	
	private PatrolTreeContentProvider contentProvider;
	
	private boolean processevent = true;
	
	@Inject private IEclipseContext context;
	@Inject private IMenuService menuService;
	@Inject private MPart localPart;
	@Inject private ESelectionService selService;
	/*
	 * Job that updates the patrol list based on the current filter
	 */
	private Job updateJob = new Job(Messages.PatrolListView_UpdatePatrolJobName) {
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(Messages.PatrolListView_Progress_LoadingPatrols, 1);
			patrolListViewer.getControl().getDisplay().syncExec(new Runnable() {
				@Override
				public void run() {
					patrolListViewer.setInput(DialogConstants.LOADING_TEXT);
					patrolListViewer.refresh();
				}
			});
			
			try(Session s = PatrolHibernateManager.openSession()){
				s.beginTransaction();
				try{
					Query<Tuple> query = filter.buildQuery(s);
					List<Tuple> results = query.list();
					final PatrolEditorInput[] input = new PatrolEditorInput[results.size()];
					int i = 0;
					for (Iterator<Tuple> iterator = results.iterator(); iterator.hasNext();) {
						Tuple data = iterator.next();					
						input[i++] = new PatrolEditorInput((UUID)data.get(0), (String)data.get(1), 
								(PatrolType.Type)data.get(2), (LocalDate)data.get(3), (LocalDate)data.get(4));
					}
					
					monitor.internalWorked(0.5);
					if (patrolListViewer.getControl().isDisposed()) return Status.CANCEL_STATUS;
					patrolListViewer.getControl().getDisplay().asyncExec(new Runnable() {
						@Override
						public void run() {
							patrolListViewer.setInput(input);
							patrolListViewer.refresh();
						}
					});
				}finally{
					s.getTransaction().rollback();
				}
			}
			return Status.OK_STATUS;
		}
	};
	
	/**
	 * listener for patrol change events.
	 */
	private PatrolEventManager.IPatrolEventListener patrolListener = new IPatrolEventListener(){
		@Override
		public void eventFired(int type, Object source) {
			updateContent(1000);
		}
	};

	private IPatrolEventListener saveListener = new IPatrolEventListener() {
		@Override
		public void eventFired(final int attributeChanged, Object source) {
			if (attributeChanged == PatrolEventManager.PATROL_DATES_LEG){
				updateContent(1000);
			}
		}
	};

	/**
	 * Creates a new vies
	 */
	public PatrolListView() {
	}

	@Inject
	@Optional
	public void partActivation(@UIEventTopic(UIEvents.UILifeCycle.BRINGTOTOP) Event event) {
		if (event.getProperty(UIEvents.EventTags.ELEMENT) != localPart) return;
		ShowFieldDataPerspective.enableToolbarItem(ID, context);
	}
	
	private String getPreferenceKey(String part) {
		return part + "." + UuidUtils.uuidToString( SmartDB.getCurrentConservationArea().getUuid() ); //$NON-NLS-1$
	}
	
	@Inject
	private void partActivated(@Optional @UIEventTopic(UIEvents.UILifeCycle.ACTIVATE) Event partEvent, EPartService pService){
		if (!processevent) return;
		try {
			processevent = false;
		
			if (partEvent == null) return;
			MPart activePart = (MPart) partEvent.getProperty(UIEvents.EventTags.ELEMENT);
			Object lpart = E3Utils.getSourceObject(activePart);
			if (lpart instanceof PatrolEditor){
				Patrol p = ((PatrolEditor)lpart).getPatrol();
				PatrolEditorInput pi = new PatrolEditorInput(p);
				patrolListViewer.setSelection(new StructuredSelection(pi));
				
				pService.bringToTop(localPart);
				pService.activate(activePart);
			}
		}finally {
			processevent = true;
		}
	}
	
	@Optional
	@Inject
	private void groupByChanged(@EventTopic(GROUP_BY_EVENT) Object data){
		if (!(data instanceof String)) return;
		contentProvider.setGroupBy((String)data);
		SmartPatrolPlugIn.getDefault().getPreferenceStore().setValue(getPreferenceKey(GB_PREF_KEY), (String)data);
	}
	
	@Optional
	@Inject
	private void dbModified(@EventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object data){
		updateContent(100);
	}
	
	@PreDestroy
	public void dispose() {		
		PatrolEventManager.getInstance().removeListener(EventType.PATROL_ADDED, patrolListener);
		PatrolEventManager.getInstance().removeListener(EventType.PATROL_DELETED, patrolListener);
		PatrolEventManager.getInstance().removeListener(EventType.PATROL_MODIFIED, patrolListener);
		PatrolEventManager.getInstance().removeListener(EventType.PATROL_SAVED, saveListener);
	}

	/**
	 * 
	 * @return the current filter
	 */
	public PatrolViewFilter getFilter() {
		return this.filter;
	}

	@PostConstruct
	public void createPartControl(Composite parent, final MApplication application) {
		localPart.getContext().set(PatrolViewFilter.class, filter);
		
		((FillLayout)parent.getLayout()).marginHeight = 0;
		((FillLayout)parent.getLayout()).marginWidth = 0;
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		main.setLayout(layout);
		
		patrolListViewer = new TreeViewer(main, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.MULTI);
		Control list = patrolListViewer.getControl();
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		list.setBounds(0, 0, 88, 68);
		
		String defaultValue = null;
		Object value = context.get(ECommandService.class).getCommand("org.wcs.smart.patrol.view.groupby").getState(RadioState.STATE_ID).getValue(); //$NON-NLS-1$
		if (value instanceof String) defaultValue = (String)value;
		contentProvider = new PatrolTreeContentProvider(defaultValue);
		
		patrolListViewer.setLabelProvider(new PatrolTreeLabelProvider());
		patrolListViewer.setContentProvider(contentProvider);
		patrolListViewer.setInput(DialogConstants.LOADING_TEXT);
		patrolListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		updateContent();
		
		PatrolEventManager.getInstance().addListener(EventType.PATROL_ADDED, patrolListener);
		PatrolEventManager.getInstance().addListener(EventType.PATROL_DELETED, patrolListener);
		PatrolEventManager.getInstance().addListener(EventType.PATROL_MODIFIED, patrolListener);
		PatrolEventManager.getInstance().addListener(EventType.PATROL_SAVED, saveListener);
		
		patrolListViewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object selection = ((IStructuredSelection)patrolListViewer.getSelection()).getFirstElement();;
				if (!(selection instanceof PatrolEditorInput)){
					return;
				}
				
				PatrolEditorInput p = (PatrolEditorInput)selection;
				if (p != null){
					IEclipseContext localCtx = EclipseContextFactory.create();
					localCtx.set(OpenPatrolHandler.PATROL_PARAM, p); 
					localCtx.setParent(localPart.getContext());
					ContextInjectionFactory.invoke(new OpenPatrolHandler(), Execute.class, localCtx);
				}				
			}
		});
		
		patrolListViewer.addSelectionChangedListener(new ViewerSelectionListener(selService));
		
		/* add right click context menu */
		MenuManager menuManager = new MenuManager();
		menuService.populateContributionManager(menuManager, "popup:org.wcs.smart.patrol.ui.PatrolListView"); //$NON-NLS-1$
		Menu menu = menuManager.createContextMenu(patrolListViewer.getControl());

		MenuItem mnuCollapseAll = new MenuItem(menu, SWT.PUSH);
		mnuCollapseAll.setText(Messages.PatrolListView_CollapseAllOp);
		mnuCollapseAll.addListener(SWT.Selection, x->{
			patrolListViewer.collapseAll();
		});
		
		
		menu.addMenuListener(new MenuListener() {

			MenuItem mnuCreateFolder = null;
			MenuItem mnuEditFolder = null;
			MenuItem mnuDeleteFolder = null;
			MenuItem mnuCollapseAll = null;
			MenuItem mnuExpandAll = null;
			MenuItem sep1 = null;

			@Override
			public void menuShown(MenuEvent e) {

				if (mnuCollapseAll  == null) {
					new MenuItem(menu,  SWT.SEPARATOR);
					
					mnuCollapseAll = new MenuItem(menu, SWT.PUSH);
					mnuCollapseAll.setText(Messages.PatrolListView_CollapseAllOp);
					mnuCollapseAll.addListener(SWT.Selection, x->{
						patrolListViewer.collapseAll();
					});
					
					mnuExpandAll = new MenuItem(menu, SWT.PUSH);
					mnuExpandAll.setText(Messages.PatrolListView_ExpandAllOp);
					mnuExpandAll.addListener(SWT.Selection, x->{
						patrolListViewer.expandAll();
					});
				}
				
				mnuCollapseAll.setEnabled(contentProvider.getGroupBy() != GroupByType.NONE);
				mnuExpandAll.setEnabled(contentProvider.getGroupBy() != GroupByType.NONE);
				
				if (contentProvider.getGroupBy() != PatrolTreeContentProvider.GroupByType.FOLDER) {
					if (mnuCreateFolder != null) mnuCreateFolder.dispose();
					mnuCreateFolder = null;
					if (mnuEditFolder != null) mnuEditFolder.dispose();
					mnuEditFolder = null;
					if (mnuDeleteFolder != null) mnuDeleteFolder.dispose();
					mnuDeleteFolder = null;
					if (sep1 != null) sep1.dispose();
					sep1 = null;
					
					return;
				}else if (mnuCreateFolder == null) {
				
					sep1 = new MenuItem(menu,  SWT.SEPARATOR, menu.getItemCount() - 3);

					mnuCreateFolder = new MenuItem(menu, SWT.PUSH, menu.getItemCount() - 3);
					mnuCreateFolder.setText(Messages.PatrolListView_CreateFolderOp);
					mnuCreateFolder.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
					mnuCreateFolder.addListener(SWT.Selection, x->{
						handleFolderCreate();
					});

					mnuEditFolder = new MenuItem(menu, SWT.PUSH, menu.getItemCount() - 3);
					mnuEditFolder.setText(Messages.PatrolListView_EditFolderOp);
					mnuEditFolder.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
					mnuEditFolder.addListener(SWT.Selection, x->{
						handleFolderEdit();
					});

					mnuDeleteFolder = new MenuItem(menu, SWT.PUSH, menu.getItemCount() - 3);
					mnuDeleteFolder.setText(Messages.PatrolListView_DeleteFolderOp);
					mnuDeleteFolder.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
					mnuDeleteFolder.addListener(SWT.Selection, x->{
						handleFolderDelete();
					});
					
					
				}
				
				ITreeSelection sel = patrolListViewer.getStructuredSelection();
				boolean foldersSelected = !sel.isEmpty();
				for (Iterator<?> it = sel.iterator(); it.hasNext();) {
					Object next = it.next();
					if (!(next instanceof PatrolFolder) || next == NoneFolder.INSTANCE) {
						foldersSelected = false;
						break;
					}
				}
				mnuCreateFolder.setEnabled(contentProvider.getGroupBy() == GroupByType.FOLDER);
				mnuEditFolder.setEnabled(contentProvider.getGroupBy() == GroupByType.FOLDER && sel.size() == 1 && foldersSelected);
				mnuDeleteFolder.setEnabled(contentProvider.getGroupBy() == GroupByType.FOLDER && foldersSelected);
			}
			
			@Override
			public void menuHidden(MenuEvent e) { }
		});
		
		
		patrolListViewer.getControl().setMenu(menu);

		Transfer[] transferTypes = new Transfer[]{LocalSelectionTransfer.getTransfer()};
		patrolListViewer.addDragSupport(DND.DROP_MOVE, transferTypes , new FolderTreeDragListener(patrolListViewer));
		patrolListViewer.addDropSupport(DND.DROP_MOVE, transferTypes, new PatrolFolderTreeDropListener(patrolListViewer, contentProvider));
		
		if (SmartPatrolPlugIn.getDefault().getPreferenceStore().contains(getPreferenceKey(GB_PREF_KEY))) {
			String gb = SmartPatrolPlugIn.getDefault().getPreferenceStore().getString(getPreferenceKey(GB_PREF_KEY));
			contentProvider.setGroupBy(gb);
		}
	}

	protected void handleFolderCreate() {
		PatrolFolder folder = new PatrolFolder();
		folder.setConservationArea(SmartDB.getCurrentConservationArea());
		folder.updateName(SmartDB.getCurrentLanguage(), Messages.PatrolListView_DefaultFolderName);
		if (!SmartDB.getCurrentLanguage().equals(SmartDB.getCurrentConservationArea().getDefaultLanguage())){
			folder.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), Messages.PatrolListView_DefaultFolderName);
		}
		//look for parent folder
		Object el = patrolListViewer.getStructuredSelection().getFirstElement();
		if (el != null && !(el instanceof PatrolFolder)) {
			el = contentProvider.getParent(el);
		}
		if (el instanceof PatrolFolder && el != NoneFolder.INSTANCE) {
			PatrolFolder pFolder = (PatrolFolder)el;
			folder.setParentFolder(pFolder);
			folder.setFolderOrder(getNextFolderOrderIndex(pFolder));
		} else {
			folder.setFolderOrder(getNextFolderOrderIndex(null));
		}
		
		FolderCreateEditDialog dlg = new FolderCreateEditDialog(Display.getCurrent().getActiveShell(), folder);
		if (dlg.open() == Window.OK) {
			contentProvider.applyCurrentGrouping();
		}
	}

	protected void handleFolderEdit() {
		ITreeSelection sel = patrolListViewer.getStructuredSelection();
		if (sel.getFirstElement() instanceof PatrolFolder) {
			PatrolFolder folder = (PatrolFolder) sel.getFirstElement();
			FolderCreateEditDialog dlg = new FolderCreateEditDialog(Display.getCurrent().getActiveShell(), folder);
			if (dlg.open() == Window.OK) {
				contentProvider.applyCurrentGrouping();
			}
		}
	}

	protected void handleFolderDelete() {
		String message = MessageFormat.format(Messages.PatrolListView_DeleteFolderConfirm_Message, Messages.PatrolListView_NoneFolder_Name);
		if (!MessageDialog.openConfirm(Display.getCurrent().getActiveShell(), Messages.PatrolListView_DeleteFolderConfirm_Title, message)) {
			return;
		}
		ITreeSelection sel = patrolListViewer.getStructuredSelection();
		List<?> items = sel.toList();
		List<PatrolFolder> toDel = items.stream().filter(pf -> pf instanceof PatrolFolder && pf != NoneFolder.INSTANCE).map(pf -> (PatrolFolder)pf).collect(Collectors.toList());
		List<PatrolFolder> roots = FolderTreeUtils.getRootFoldersFromImput(contentProvider.getElements(null));
		Job deleteJob = new DeletePatrolFoldersJob(roots, toDel);
		deleteJob.schedule();
		try {
			deleteJob.join();
		} catch (InterruptedException ex) {
			SmartPlugIn.displayError(Messages.PatrolListView_DeleteFoldersJob_Error, ex);
		}
		contentProvider.applyCurrentGrouping();
	}

	private int getNextFolderOrderIndex(PatrolFolder parent) {
		if (parent == null) {
			return FolderTreeUtils.getRootFoldersFromImput(contentProvider.getElements(null)).size();
		}
		return parent.getChildFolders().size();
	}
	
	/**
	 * updates content immediately
	 */
	public void updateContent(){
		updateContent(0);
	}
	/**
	 * Refreshes the list of patrols after delay
	 */
	public void updateContent(int delay){
		updateJob.cancel();
		updateJob.schedule(delay);		
	}
	
	@Focus
	public void setFocus() {
		patrolListViewer.getControl().setFocus();
	}
	
	public static class PatrolListViewWrapper extends DIViewPart<PatrolListView>{
		public PatrolListViewWrapper(){
			super(PatrolListView.class);
		}
	}
}
