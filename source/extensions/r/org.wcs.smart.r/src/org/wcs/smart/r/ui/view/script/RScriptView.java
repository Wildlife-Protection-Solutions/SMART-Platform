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
package org.wcs.smart.r.ui.view.script;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.osgi.service.event.Event;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.r.RPlugIn;
import org.wcs.smart.r.RScriptManager;
import org.wcs.smart.r.internal.Messages;
import org.wcs.smart.r.model.RQuery;
import org.wcs.smart.r.model.RScript;
import org.wcs.smart.r.ui.OpenRScriptHandler;
import org.wcs.smart.r.ui.RScriptDialog;
import org.wcs.smart.r.ui.RunRScriptHandler;
import org.wcs.smart.r.ui.editor.script.RScriptEditor;
import org.wcs.smart.r.ui.editor.script.RScriptEditorInput;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.E3Utils;
import org.wcs.smart.util.UuidUtils;

/**
 * Simple view for listing R scripts and queries
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class RScriptView {

	public static final String ID = "org.wcs.smart.r.view.rscripts"; //$NON-NLS-1$

	@Inject private IEclipseContext context;
	@Inject private MPart localpart;
	
	private TreeViewer itemViewer;
	private List<UuidItem> queries = new ArrayList<>();
	private List<UuidItem> scripts = new ArrayList<>();
	
	private final static String QUERY_NODE = "RQueries"; //$NON-NLS-1$
	private final static String SCRIPT_NODE = "RScripts"; //$NON-NLS-1$
	
	/**
	 * Default constructor
	 */
	public RScriptView() {
		
	}
	
	@Optional
	@Inject
	private void dbModified(@EventTopic(SmartPlugIn.E4_DATABASE_CHANGED_EVENT) Object data){
		updateContent();
	}

	
	@Optional
	@Inject
	private void rScriptModified(@EventTopic(RScriptManager.R_ALL) Object data){
		updateContent();
	}
	
	@PreDestroy
	public void dispose() {		

	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@PostConstruct
	public void createPartControl(Composite parent) {
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
		
		itemViewer = new TreeViewer(main, SWT.MULTI);
		itemViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		itemViewer.setContentProvider(new ITreeContentProvider() {
			
			@Override
			public boolean hasChildren(Object element) {
				if (element == QUERY_NODE) return true;
				if (element == SCRIPT_NODE) return true;
				return false;
			}
			
			@Override
			public Object getParent(Object element) {
				if (element == QUERY_NODE) return null;
				if (element == SCRIPT_NODE) return null;
				if (element instanceof UuidItem) {
					if (((UuidItem)element).type == Type.QUERY) return QUERY_NODE;
					if (((UuidItem)element).type == Type.SCRIPT) return SCRIPT_NODE;
				}
				return null;
			}
			
			@Override
			public Object[] getElements(Object inputElement) {
				return new Object[] {QUERY_NODE, SCRIPT_NODE};
			}
			
			@Override
			public Object[] getChildren(Object parentElement) {
				if (parentElement == QUERY_NODE) return queries.toArray();
				if (parentElement == SCRIPT_NODE) return scripts.toArray();
				return null;
			}
		});
		
		itemViewer.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element == QUERY_NODE) return Messages.RScriptView_RQueryLabel;
				if (element == SCRIPT_NODE) return Messages.RScriptView_RScirptLabel;
				if (element instanceof UuidItem) return ((UuidItem)element).name;
				return super.getText(element);
			}
			
			public Image getImage(Object element) {
				if (element == QUERY_NODE) return RPlugIn.getDefault().getImageRegistry().get(RPlugIn.ICON_QUERY);
				if (element == SCRIPT_NODE) return RPlugIn.getDefault().getImageRegistry().get(RPlugIn.ICON_R);
				if (element instanceof UuidItem) {
					if  (((UuidItem)element).type == Type.QUERY) return RPlugIn.getDefault().getImageRegistry().get(RPlugIn.ICON_QUERY);
					if  (((UuidItem)element).type == Type.SCRIPT) return RPlugIn.getDefault().getImageRegistry().get(RPlugIn.ICON_R);
				}
				return null;
			}
		});
		
		itemViewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				Object x = itemViewer.getStructuredSelection().getFirstElement();
				if (x instanceof UuidItem) {
					newQuery((UuidItem)x);
				}
				
			}
		});
		
		Menu mnu = new Menu(itemViewer.getControl());
		
		itemViewer.getControl().setMenu(mnu);
		
		mnu.addMenuListener(new MenuListener() {
			@Override
			public void menuShown(MenuEvent e) {
				for (MenuItem mi : mnu.getItems()) mi.dispose();
				
				Object x = itemViewer.getStructuredSelection().getFirstElement();
				if (x instanceof UuidItem && ((UuidItem) x).type == Type.QUERY) {
					MenuItem miOpen= new MenuItem(mnu, SWT.PUSH);
					miOpen.setText(Messages.RScriptView_OpenLabel);
					miOpen.addListener(SWT.Selection, s->newQuery((UuidItem)x));
					
					new MenuItem(mnu, SWT.SEPARATOR);
					
					MenuItem miDelete= new MenuItem(mnu, SWT.PUSH);
					miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
					miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
					miDelete.addListener(SWT.Selection, s->deleteQueries());
					
				}else if (x instanceof UuidItem && ((UuidItem) x).type == Type.SCRIPT) {
					MenuItem miNew = new MenuItem(mnu, SWT.PUSH);
					miNew.setText(Messages.RScriptView_NewQueryLabel);
					miNew.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
					miNew.addListener(SWT.Selection, s->newQuery((UuidItem)x));
					
					new MenuItem(mnu, SWT.SEPARATOR);
					
					MenuItem miNewScript = new MenuItem(mnu, SWT.PUSH);
					miNewScript.setText(Messages.RScriptView_NewRScriptMenuItem);
					miNewScript.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
					miNewScript.addListener(SWT.Selection, s->newScript());
					
					MenuItem miEdit = new MenuItem(mnu, SWT.PUSH);
					miEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
					miEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
					miEdit.addListener(SWT.Selection, s->editScript((UuidItem)x));
					
					new MenuItem(mnu, SWT.SEPARATOR);
					
					MenuItem miDelete = new MenuItem(mnu, SWT.PUSH);
					miDelete.setText(DialogConstants.DELETE_BUTTON_TEXT);
					miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
					miDelete.addListener(SWT.Selection, s->deleteScript((UuidItem)x));
					
					
				}else if (x == SCRIPT_NODE) {
					MenuItem miNewScript = new MenuItem(mnu, SWT.PUSH);
					miNewScript.setText(Messages.RScriptView_NewRScriptMenuItem);
					miNewScript.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ADD_ICON));
					miNewScript.addListener(SWT.Selection, s->newScript());
				}
				
				
			}
			
			@Override
			public void menuHidden(MenuEvent e) {
			}
		});
	
		updateContent();
	}
	
	private void newScript() {
		RScriptDialog dialog = new RScriptDialog(context.get(Shell.class));
		if (dialog.open() == Window.OK) updateContent();
	}
	
	private void editScript(UuidItem item) {
		RScript type = new RScript();
		type.setUuid(item.uuid);
		RScriptDialog dialog = new RScriptDialog(context.get(Shell.class), type);
		if (dialog.open() == Window.OK) updateContent();
	}
	
	private void deleteScript(UuidItem item) {
		RScript s = null;
		try(Session session = HibernateManager.openSession()) {
			s = session.get(RScript.class, item.uuid);
		}
		if (s != null) {
			RScriptManager.INSTANCE.deleteScripts(Collections.singletonList(s), context.get(Shell.class));
		}
		updateContent();
	}
	
	private void deleteQueries() {
		List<UUID> toDelete = new ArrayList<>();
		for (Iterator<?> iterator = itemViewer.getStructuredSelection().iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof UuidItem && ((UuidItem) x).type == Type.QUERY) {
				toDelete.add(((UuidItem)x).uuid);
			}
		}
		if (toDelete.isEmpty()) return;
		
		if (!MessageDialog.openConfirm(itemViewer.getControl().getShell(), Messages.RScriptView_DeleteTitle, 
				MessageFormat.format(Messages.RScriptView_DeleteMssage, toDelete.size()))) {
			return;
		}
		List<RQuery> deleted = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				for (UUID uuid : toDelete) {
					RQuery q = session.get(RQuery.class, uuid);
					if (q != null) {
						session.remove(q);
						deleted.add(q);
					}
				}
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				RPlugIn.displayLog(Messages.RScriptView_DeleteError + ex.getMessage(), ex);
			}
		}
		
		context.get(IEventBroker.class).post(RScriptManager.R_DELETE, deleted);
	}
	
	private void newQuery(UuidItem x) {
		if (x.type == Type.SCRIPT) {
			UUID u = ((UuidItem)x).uuid;
			(new RunRScriptHandler()).execute(null, UuidUtils.uuidToString(u), context);
		}else if (x.type == Type.QUERY) {
			RQuery temp = new RQuery();
			temp.setUuid( ((UuidItem)x).uuid );
			(new OpenRScriptHandler()).execute(temp);
		}
	}

	@Inject
	private void partActivated(@Optional @UIEventTopic(UIEvents.UILifeCycle.ACTIVATE) Event partEvent){
		if (partEvent == null) return;

		MPart activePart = (MPart) partEvent.getProperty(UIEvents.EventTags.ELEMENT);
		Object lpart = E3Utils.getSourceObject(activePart);
		if (lpart instanceof RScriptEditor){
			context.get(EPartService.class).bringToTop(localpart);

			UUID uuid = ((RScriptEditorInput) ((RScriptEditor)lpart).getEditorInput()).getRScript();
			RScript temp = new RScript();
			temp.setUuid(uuid);
					
			itemViewer.setSelection(new StructuredSelection(temp));
		}
	}
	
	
	/**
	 * Refreshes the Plan list
	 */
	public void updateContent(){
		loadData.cancel();
		loadData.schedule();
	}
	
	@Focus
	public void setFocus() {
		itemViewer.getControl().setFocus();
	}

	private Job loadData = new Job(Messages.RScriptView_RefreshJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			try(Session session = HibernateManager.openSession()){
				List<RQuery> qs = QueryFactory.buildQuery(session, RQuery.class,
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
				queries.clear();
				for (RQuery q : qs) {
					queries.add(new UuidItem(Type.QUERY, q.getName(),q.getUuid()));
				}
				
				List<RScript> ss = QueryFactory.buildQuery(session, RScript.class,
						new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list(); //$NON-NLS-1$
				scripts.clear();
				for (RScript s : ss) {
					scripts.add(new UuidItem(Type.SCRIPT, s.getName(),s.getUuid()));
				}
			}
			
			Collections.sort(queries);
			Collections.sort(scripts);
			
			Display.getDefault().syncExec(()->{
				itemViewer.setInput(Collections.emptyList());
				itemViewer.refresh();
				itemViewer.expandAll();
			});
			
			
			return Status.OK_STATUS;
		} 
		
	};
    
	public static class RScriptViewWrapper extends DIViewPart<RScriptView>{
		public RScriptViewWrapper(){
			super(RScriptView.class);
		}
	}
	
	private enum Type{QUERY, SCRIPT};

	private class UuidItem implements Comparable<UuidItem>{
		String name;
		UUID uuid;
		Type type;
		
		public UuidItem(Type type, String name, UUID uuid) {
			this.name = name;
			this.type = type;
			this.uuid = uuid;
		}
		
		@Override
		public int compareTo(UuidItem o) {
			return Collator.getInstance().compare(name,  ((UuidItem)o).name);
		}
	}
}