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
package org.wcs.smart.intelligence.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.menus.IMenuService;
import org.hibernate.Query;
import org.hibernate.Session;
import org.osgi.service.event.Event;
import org.wcs.smart.common.filter.StringFilterComposite.StringComparison;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.intelligence.IntelligenceEventManager;
import org.wcs.smart.intelligence.IntelligenceEventManager.EventType;
import org.wcs.smart.intelligence.IntelligenceEventManager.IIntelligenceEventListener;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.intelligence.ui.editor.IntelligenceEditor;
import org.wcs.smart.intelligence.ui.editor.IntelligenceEditorInput;
import org.wcs.smart.intelligence.ui.handlers.OpenIntelligenceHandler;
import org.wcs.smart.ui.SearchTextBox;
import org.wcs.smart.ui.ViewerSelectionListener;
import org.wcs.smart.util.E3Utils;

/**
 * A viewer where users can view all intelligence items.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceListView implements IIntelligenceFilteringView {

	public static final String ID = "org.wcs.smart.intelligence.IntelligenceListView"; //$NON-NLS-1$

	private TableViewer intelligenceListViewer;
	private Job updateJob = new UpdateIntelligenceListIdJob();
	private IntelligenceViewFilter filter = new IntelligenceViewFilter();

	@Inject private MPart part;
	@Inject private IMenuService menuService;
	@Inject private ESelectionService selService;
	
	/**
	 * listener for intelligence change events.
	 */
	private IIntelligenceEventListener intelligenceListener = new IIntelligenceEventListener(){
		@Override
		public void eventFired(int type, Intelligence source) {
			updateContent();
		}
	};
	
	@Inject
	private void partActivated(@Optional @UIEventTopic(UIEvents.UILifeCycle.ACTIVATE) Event partEvent){
		if (partEvent == null) return;
		MPart activePart = (MPart) partEvent.getProperty(UIEvents.EventTags.ELEMENT);
		Object lpart = E3Utils.getSourceObject(activePart);
		if (lpart instanceof IntelligenceEditor){
			intelligenceListViewer.setSelection(new StructuredSelection(((IntelligenceEditor)lpart).getEditorInput()));
		}
	}
	
	@PreDestroy
	public void dispose() {		
		IntelligenceEventManager.getInstance().removeListener(EventType.INTELLIGENCE_ADDED, intelligenceListener);
		IntelligenceEventManager.getInstance().removeListener(EventType.INTELLIGENCE_MODIFIED, intelligenceListener);
		IntelligenceEventManager.getInstance().removeListener(EventType.INTELLIGENCE_DELETED, intelligenceListener);
	}
	

	@PostConstruct
	public void createPartControl(Composite parent) {
		((FillLayout)parent.getLayout()).marginHeight = 0;
		((FillLayout)parent.getLayout()).marginWidth = 0;
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		GridLayout layout = new GridLayout(1, false);
		main.setLayout(layout);
		main.setBackground(main.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		final SearchTextBox searchBox = new SearchTextBox(main, SWT.NONE){
			@Override
			protected Composite getFocusControl(){
				return intelligenceListViewer.getTable();
			}
		};
		searchBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		searchBox.setRefreshJob(new Job(Messages.IntelligenceListView_SearchJobName){
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (searchBox == null || searchBox.isDisposed()) return Status.CANCEL_STATUS;
				searchBox.getDisplay().syncExec(new Runnable(){
					@Override
					public void run() {
						String txt = searchBox.getFilterString();
						if (txt == null){
							getFilter().setNameFilter(null,  null);
						}else{
							getFilter().setNameFilter(StringComparison.CONTAINS, txt);
						}
					}});				
				updateContent();
				return Status.OK_STATUS;
			}});
		intelligenceListViewer = new TableViewer(main, SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
		Table list = intelligenceListViewer.getTable();
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		list.setBounds(0, 0, 88, 68);
		
		intelligenceListViewer.setLabelProvider(new IntelligenceEditorInputLabelProvider());
		intelligenceListViewer.setContentProvider(ArrayContentProvider.getInstance());
		intelligenceListViewer.setInput(new Object[]{Messages.IntelligenceListView_Loading_Label});
		intelligenceListViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		updateContent();

		IntelligenceEventManager.getInstance().addListener(EventType.INTELLIGENCE_ADDED, intelligenceListener);
		IntelligenceEventManager.getInstance().addListener(EventType.INTELLIGENCE_MODIFIED, intelligenceListener);
		IntelligenceEventManager.getInstance().addListener(EventType.INTELLIGENCE_DELETED, intelligenceListener);
		
		intelligenceListViewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				IntelligenceEditorInput input = (IntelligenceEditorInput)((IStructuredSelection)intelligenceListViewer.getSelection()).getFirstElement();
				if (input == null) return;
				
				IEclipseContext localCtx = EclipseContextFactory.create();
				localCtx.set(OpenIntelligenceHandler.INTELLUUID_PARAM, input.getUuid());
				localCtx.setParent(part.getContext());
				ContextInjectionFactory.invoke(new OpenIntelligenceHandler(), Execute.class, localCtx);
			}
		});
		intelligenceListViewer.addSelectionChangedListener(new ViewerSelectionListener(selService));
		
		/* add right click context menu */
		MenuManager menuManager = new MenuManager();
		menuService.populateContributionManager(menuManager, "popup:org.wcs.smart.intelligence.IntelligenceListView"); //$NON-NLS-1$
		Menu menu = menuManager.createContextMenu(intelligenceListViewer.getControl());
		intelligenceListViewer.getControl().setMenu(menu);	
	}

	/**
	 * Refreshes the intelligence list
	 */
	public void updateContent(){
		updateJob.cancel();
		updateJob.schedule();		
	}
	
	@Focus
	public void setFocus() {
		intelligenceListViewer.getControl().setFocus();

	}

	public IntelligenceViewFilter getFilter() {
		return filter;
	}
	
    /**
     * Label provider for intelligence editor input objects.
     * 
     * @author elitvin
     *
     */
	private class IntelligenceEditorInputLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof IntelligenceEditorInput){
				IntelligenceEditorInput i = (IntelligenceEditorInput)element;
				return Intelligence.generateLabel(i.getName(), i.getReceivedDate());
			}
			return super.getText(element);
		}
		
	}

    /**
     * Job is used to fill list viewer with data
     * 
     * @author elitvin
     *
     */
    private class UpdateIntelligenceListIdJob extends Job {

    	public UpdateIntelligenceListIdJob() {
			super(Messages.IntelligenceListView_UpdateJob_Title);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			monitor.beginTask(Messages.IntelligenceListView_UpdateJob_LoadTask_Name, 1);
			
			List<?> result = loadIntelligences();
			monitor.internalWorked(0.7);
			
			//convert loaded data to List<IntelligenceEditorInput>
			final List<IntelligenceEditorInput> inputData = new ArrayList<IntelligenceEditorInput>();
			for (Object obj : result) {
				Object[] data = (Object[]) obj;
				inputData.add(new IntelligenceEditorInput((UUID)data[0], (String)data[1], (Date)data[2]));
			}
			monitor.internalWorked(0.8);
			
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					intelligenceListViewer.setInput(inputData);
					intelligenceListViewer.refresh();
				}
			});
			return Status.OK_STATUS;
		}
		
		private List<?> loadIntelligences() {
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try {
				Query query = IntelligenceListView.this.getFilter().buildQuery(session);
				List<?> list = query.list();
				return list;
			} finally {
				session.getTransaction().rollback();
				session.close();
			}
		}
   	
    }

    
	public static class IntelligenceListViewWrapper extends DIViewPart<IntelligenceListView>{
		public IntelligenceListViewWrapper(){
			super(IntelligenceListView.class);
		}
	}
}
