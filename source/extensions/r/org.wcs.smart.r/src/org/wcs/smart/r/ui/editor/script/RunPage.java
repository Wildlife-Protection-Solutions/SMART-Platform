package org.wcs.smart.r.ui.editor.script;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.r.engine.REngine;
import org.wcs.smart.r.model.RScript;
import org.wcs.smart.r.model.RScriptParameter;

public class RunPage extends EditorPart {

	private FormToolkit toolkit;
	
	private Form mainForm; 
	private Text txtParameters;
	
	private QueryListComposite queryList;
	private RScriptEditor parent;
	
	public RunPage(RScriptEditor parent) {
		this.parent = parent;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		toolkit.dispose();
	}
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	private void executeScript() {
		parent.showResults();
		
		REngine engine = new REngine(queryList.getQueries(),txtParameters.getText(),parent.createPage2OutputStream());
		engine.execute();
		
	}
	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		
		mainForm = toolkit.createForm(parent);
		mainForm.getBody().setLayout(new GridLayout());
//		mainForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite main = toolkit.createComposite(mainForm.getBody(), SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Label l = toolkit.createLabel(main, "R Script Parameters:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		txtParameters = toolkit.createText(main, "", SWT.V_SCROLL | SWT.WRAP);
		txtParameters.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtParameters.getLayoutData()).heightHint = 70;

//		l = toolkit.createLabel(main, "Queries:");
		l = toolkit.createLabel(main, "");
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		queryList = new QueryListComposite(main);
		queryList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		DropTarget dtarget = new DropTarget(queryList, DND.DROP_MOVE);
		dtarget.setTransfer(new Transfer[] { LocalSelectionTransfer.getTransfer() });
		dtarget.addDropListener(new DropTargetAdapter() {

			@Override
			public void dragEnter(DropTargetEvent event) {

				
			}

			public void dragLeave(DropTargetEvent event) {

			}
			
			@Override
			public void dragOperationChanged(DropTargetEvent event) {
			}

			@Override
			public void dragOver(DropTargetEvent event) {
				
			}

			@Override
			public void dropAccept(DropTargetEvent event) {
			}

			@Override
			public void drop(DropTargetEvent event) {
				if (event.detail == DND.DROP_NONE ){
					return;
				}
				StructuredSelection selection = (StructuredSelection) LocalSelectionTransfer.getTransfer().getSelection();
				if (selection == null) return;
				
				List<Query> q = new ArrayList<>();
				try(Session s = HibernateManager.openSession()){
					for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
						Object x = iterator.next();
						if (x instanceof QueryEditorInput) {
							QueryEditorInput dragItem = (QueryEditorInput) x;
							Query temp = QueryHibernateManager.getInstance().findQuery(s, dragItem.getUuid(), dragItem.getType());
							if (temp != null) {
								q.add(temp);
							}	
						}
					}
				}
				if (q != null) queryList.addQueries(q);
			}

		
		});
		
		if (((RScriptEditorInput)getEditorInput()).getDefaultQueries() != null) {
			List<Query> toAdd = new ArrayList<>();
			try(Session s = HibernateManager.openSession()){
				for (QueryEditorInput q : ((RScriptEditorInput)getEditorInput()).getDefaultQueries()) {
					Query t = QueryHibernateManager.getInstance().findQuery(s, q.getUuid(), q.getType());
					if (t != null) toAdd.add(t);
				}
			}
			queryList.addQueries(toAdd);
		}
		
		Button btnRun = new Button(main, SWT.PUSH);
		btnRun.setText("Run Script");
		btnRun.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, 3, 1));
		btnRun.addListener(SWT.Selection, e-> executeScript());
	}
	

	public void update() {
		RScript script = parent.getScript();
		mainForm.setText("R Script: " + script.getName());
		boolean found = false;
		if (script.getParameters() != null) {
			for (RScriptParameter p : script.getParameters()) {
				if (p.getKey().equalsIgnoreCase(RScriptParameter.R_PARAMETER)) {
					txtParameters.setText(p.getValue());
					found = true;
					break;
				}
			}
		}
		if (!found && script.getDefaultParameters() != null) {
			txtParameters.setText(script.getDefaultParameters());
		}
	}
	
	@Override
	public void setFocus() {
		txtParameters.setFocus();
	}

}
