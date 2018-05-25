package org.wcs.smart.r.ui;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.inject.Named;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.r.model.RScript;
import org.wcs.smart.r.ui.editor.script.RScriptEditor;
import org.wcs.smart.r.ui.editor.script.RScriptEditorInput;
import org.wcs.smart.ui.NamedItemLabelProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.UuidUtils;


public class RunRScriptHandler {

	public static final String SCRIPTUUID_PARAM = "org.wcs.smart.r.scirpt.run.scriptuuid"; //$NON-NLS-1$

	
	public RunRScriptHandler() {
	}

	@Execute
	public void execute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) IStructuredSelection selection,@Optional @Named(SCRIPTUUID_PARAM) String scriptUuid, IEclipseContext context) {	
		Shell activeShell = context.get(Shell.class);
		
		RScript scriptToRun = null;
		if (scriptUuid != null) {
			//search db for script
			UUID sid = UuidUtils.stringToUuid(scriptUuid);
			try (Session session = HibernateManager.openSession()){
				scriptToRun = session.get(RScript.class,sid);
			}
			if (scriptToRun == null) {
				MessageDialog.openError(activeShell, "Error", "R script not found.");
			}
		}
		if (scriptToRun == null) {
			ListRScriptDialog dialog = new ListRScriptDialog(activeShell);
			if (dialog.open() == Window.CANCEL) return;
			scriptToRun = dialog.getSelectedScript();
		}
		
		if (scriptToRun == null) return;
		
		//update menu
		//open R editor with scriptToRun
		RunScriptMenuContribution.addScript(scriptToRun);
//		updateMenu(context);
		//selection = query editor input 
		
		//manage last run list
		
		//org.wcs.smart.r.scripts.runlist
		List<QueryEditorInput> defaultQueries = new ArrayList<>();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object item = iterator.next();
			if (item instanceof QueryEditorInput) {
				defaultQueries.add((QueryEditorInput) item);
			}
			
		}
		RScriptEditorInput input = new RScriptEditorInput(scriptToRun, defaultQueries.isEmpty() ? null : defaultQueries);
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, RScriptEditor.ID);
		} catch (PartInitException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	

	}

	
	// E3
	public static class RunRScriptHandlerWrapper extends AbstractHandler {

		private RunRScriptHandler component;

		public RunRScriptHandlerWrapper() {
			IEclipseContext context = getActiveContext();
			component = ContextInjectionFactory.make(RunRScriptHandler.class, context);
		}

		private static IEclipseContext getActiveContext() {
			IEclipseContext parentContext = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
			return parentContext.getActiveLeaf();
		}
		
		@Override
		public Object execute(ExecutionEvent event) throws ExecutionException {
			//Default di handler does not add parameters into context
			IEclipseContext ctx = getActiveContext();
			ctx.set(SCRIPTUUID_PARAM, event.getParameter(SCRIPTUUID_PARAM));
			return ContextInjectionFactory.invoke(component, Execute.class, ctx);
		}
	}
	
	private class ListRScriptDialog extends TitleAreaDialog{

		private TableViewer cmbScripts;
		private List<RScript> script = null;
	 
		private RScript selection;
		
		private Job loadScript = new Job("load R scripts "){ //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				script = null;
				try(Session session = HibernateManager.openSession()){
					script = QueryFactory.buildQuery(session, RScript.class, new Object[] {"conservationArea", SmartDB.getCurrentConservationArea()}).list();
					script.forEach(c->c.getName()); 
				}
				
				script.sort((a,b)->Collator.getInstance().compare(a.getName().toLowerCase(), b.getName().toLowerCase()));
				
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						if (cmbScripts.getControl().isDisposed()) return;
						cmbScripts.setInput(script);
					}
				});
				return Status.OK_STATUS;
			}
			
		};
		public ListRScriptDialog(Shell parentShell) {
			super(parentShell);
		}

		public void okPressed() {
			Object x = cmbScripts.getStructuredSelection().getFirstElement();
			if (x instanceof RScript) {
				selection = (RScript) x;
				super.okPressed();
			}
		}
		
		public RScript getSelectedScript() {
			return this.selection;
		}
		
		@Override
		protected Control createDialogArea(Composite parent) {
			parent = (Composite) super.createDialogArea(parent);
			parent = new Composite(parent, SWT.NONE);
			parent.setLayout(new GridLayout(2, false));
			parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			cmbScripts = new TableViewer(parent);
			cmbScripts.setContentProvider(ArrayContentProvider.getInstance());
			cmbScripts.setLabelProvider(new NamedItemLabelProvider());
			cmbScripts.setInput(new String[]{DialogConstants.LOADING_TEXT});
			cmbScripts.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			cmbScripts.getControl().setFocus();
			cmbScripts.addDoubleClickListener(new IDoubleClickListener() {
				@Override
				public void doubleClick(DoubleClickEvent event) {
					okPressed();
				}
			});
			cmbScripts.addSelectionChangedListener(evt->getButton(IDialogConstants.OK_ID).setEnabled(true));
			setTitle("R Scripts");
			getShell().setText("R Scripts");
			setMessage("Select the R Script to run");
			
			loadScript.setSystem(true);
			loadScript.schedule();
			
			return parent;
		}
		
		@Override
		protected void createButtonsForButtonBar(Composite parent) {
			Button b = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
			b.setEnabled(false);
			createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, true);
		}
		
		@Override
		public boolean isResizable(){
			return true;
		}
	}
}
