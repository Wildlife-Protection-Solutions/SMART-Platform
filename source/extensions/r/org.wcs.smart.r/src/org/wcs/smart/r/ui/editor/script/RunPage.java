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
package org.wcs.smart.r.ui.editor.script;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
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
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.r.engine.QueryConfiguration;
import org.wcs.smart.r.engine.REngine;
import org.wcs.smart.r.internal.Messages;
import org.wcs.smart.r.model.RQuery;
import org.wcs.smart.ui.TranslateSimpleListItemDialog;
import org.wcs.smart.util.UuidUtils;

/**
 * Run page of R script editor
 * @author Emily
 *
 */
public class RunPage extends EditorPart {

	private FormToolkit toolkit;
	
	private Form mainForm; 
	private Text txtParameters;
	private Label txtScriptName;
	
	private QueryListComposite queryList;
	private RScriptEditor parent;
	private HeaderComposite header;
	
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

	void updateQuery(RQuery query) {
		query.setConfiguration(QueryConfiguration.toConfigurationString(txtParameters.getText(), queryList.getQueries()));
	}
	
	private void parse(RQuery query) throws ParseException {
		if (query.getConfiguration() == null || query.getConfiguration().isEmpty()) return;
		JSONParser jsonParser = new JSONParser();
		JSONObject items = (JSONObject) jsonParser.parse(query.getConfiguration());
		
		String params = (String)items.get(RQuery.PARAM_JSONKEY);
		txtParameters.setText(params);
		JSONArray queries = (JSONArray)items.get(RQuery.QUERY_JSONKEY);

		for (Object q : queries.toArray()) {
			JSONObject item = (JSONObject)q;
			
			String typeKey = (String) item.get(RQuery.QTYPE_JSONKEY);
			UUID qUuid = UuidUtils.stringToUuid((String)item.get(RQuery.QUUID_JSONKEY));
			String eFormat = (String) item.get(RQuery.QEXPORT_JSONKEY);
			String dates = (String)item.get(RQuery.QDATE_JSON_KEY);
			
			IQueryType qType = QueryTypeManager.INSTANCE.findQueryType(typeKey);
			DateFilter dFilter = null;
			try {
				dFilter = DateFilter.fromString(dates, qType.getDateFilterOptions());
			}catch (Exception ex) {
				ex.printStackTrace();
			}
			
			Query dbquery = null;
			try(Session session = HibernateManager.openSession()){
				dbquery = QueryHibernateManager.getInstance().findQuery(session, qUuid, qType);
			}
			if (dbquery != null) queryList.addQuery(dbquery, dFilter, eFormat, false);
		}
		queryList.updateList();
		
	}
	void executeScript() {
		parent.showResults();
		REngine engine = new REngine(parent.getQuery().getScript(), queryList.getQueries(),txtParameters.getText(),parent.createPage2OutputStream());
		engine.execute();
		
	}
	
	private void translate() {
		TranslateSimpleListItemDialog dialog = new TranslateSimpleListItemDialog(mainForm.getShell(), RunPage.this.parent.getQuery());
		if (dialog.open() == Window.OK) {
			RunPage.this.parent.updateName( RunPage.this.parent.getQuery().getName() );
			header.setText(RunPage.this.parent.getQuery().getName());
		}
	}
	
	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		
		mainForm = toolkit.createForm(parent);
		mainForm.getBody().setLayout(new GridLayout());
		
		Composite top = toolkit.createComposite(mainForm.getBody(), SWT.NONE);
		top.setLayout(new GridLayout(2, false));
		top.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)top.getLayout()).marginWidth = 0;
		((GridLayout)top.getLayout()).marginHeight = 0;
		
		header = new HeaderComposite(top, toolkit, mainForm.getFont(), mainForm.getForeground()) {};
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		header.addListener(SWT.Selection, e->{
			RunPage.this.parent.updateName(e.text);
		});
		
		Composite main = toolkit.createComposite(mainForm.getBody(), SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite scriptName = toolkit.createComposite(main, SWT.NONE);
		scriptName.setLayout(new GridLayout(3, false));
		scriptName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false,false));
		((GridLayout)scriptName.getLayout()).marginWidth = 0;
		((GridLayout)scriptName.getLayout()).marginHeight = 0;
		

		Label l = toolkit.createLabel(scriptName, Messages.RunPage_RScriptLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		txtScriptName = toolkit.createLabel(scriptName, ""); //$NON-NLS-1$
		txtScriptName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Hyperlink translate = toolkit.createHyperlink(scriptName, Messages.HeaderComposite_translateLink, SWT.NONE);
		translate.addHyperlinkListener(new IHyperlinkListener() {
			@Override
			public void linkExited(HyperlinkEvent e) { }
			@Override
			public void linkEntered(HyperlinkEvent e) { }
			
			@Override
			public void linkActivated(HyperlinkEvent e) {
				translate();
			}
		});
		translate.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false));
		
		l = toolkit.createLabel(main, Messages.RunPage_Parameters);
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		txtParameters = toolkit.createText(main, "", SWT.V_SCROLL | SWT.WRAP); //$NON-NLS-1$
		txtParameters.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtParameters.getLayoutData()).heightHint = 50;
		txtParameters.addListener(SWT.Modify, e->RunPage.this.parent.setDirty(true));
		l = toolkit.createLabel(main, ""); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		queryList = new QueryListComposite(main);
		queryList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		queryList.addListener(SWT.Selection, e->RunPage.this.parent.setDirty(true));
		
		DropTarget dtarget = new DropTarget(queryList, DND.DROP_MOVE);
		dtarget.setTransfer(new Transfer[] { LocalSelectionTransfer.getTransfer() });
		dtarget.addDropListener(new DropTargetAdapter() {
			@Override
			public void dragEnter(DropTargetEvent event) {}
			@Override
			public void dragLeave(DropTargetEvent event) {}
			@Override
			public void dragOperationChanged(DropTargetEvent event) {}
			@Override
			public void dragOver(DropTargetEvent event) {}
			@Override
			public void dropAccept(DropTargetEvent event) {}
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
		btnRun.setText(Messages.RunPage_RunButton);
		btnRun.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, 3, 1));
		btnRun.addListener(SWT.Selection, e-> executeScript());
	}
	

	public void update() {
		txtScriptName.setText(parent.getQuery().getScript().getName());
		header.setText(parent.getQuery().getName());
		try {
			parse(parent.getQuery());
		} catch (ParseException e) {
			e.printStackTrace();
		}		
	}
	
	@Override
	public void setFocus() {
		txtParameters.setFocus();
	}

}
