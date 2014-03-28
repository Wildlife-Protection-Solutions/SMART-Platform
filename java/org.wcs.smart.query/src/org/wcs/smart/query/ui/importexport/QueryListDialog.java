package org.wcs.smart.query.ui.importexport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.query.ui.querylist.QueryListContentProvider;
import org.wcs.smart.query.ui.querylist.QueryListLabelProvider;
import org.wcs.smart.query.ui.querylist.SavedQueryTree;

public class QueryListDialog extends TitleAreaDialog{

	private TreeViewer queryTree;
	
	private List<QueryEditorInput> queries;
	
	private Job loadQueriesJob = new Job("loading queries") { //$NON-NLS-1$
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final HashMap<Integer, Object> data = new HashMap<Integer, Object>();
			data.put(QueryListContentProvider.QUERY_KEY, SavedQueryTree
					.getInstance().getQueries());
			data.put(QueryListContentProvider.FOLDER_KEY, SavedQueryTree
					.getInstance().getFolders());

			
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					queryTree.setInput(data);
					
				}
			});

			return Status.OK_STATUS;
		}

	};
	
	public QueryListDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	public Point getInitialSize(){
		Point p = super.getInitialSize();
		return new Point(p.x, Math.max(p.y*2, 500));
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	/**
	 * Create contents of the dialog.
	 */
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite) super.createDialogArea(parent);
		
		queryTree = new TreeViewer(composite, SWT.BORDER | SWT.MULTI);
		queryTree.setLabelProvider(new QueryListLabelProvider());
		queryTree.setContentProvider(new QueryListContentProvider(true));
		queryTree.setAutoExpandLevel(2);
		queryTree.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		queryTree.setInput(Messages.QueryListDialog_LoadingLabel);
		
		loadQueriesJob.setSystem(true);
		loadQueriesJob.schedule();

		setMessage(Messages.QueryListDialog_Message);
		setTitle(Messages.QueryListDialog_Title);
		getShell().setText(Messages.QueryListDialog_Title);
		return parent;
		
	}
		
	@Override
	public void okPressed(){
		IStructuredSelection sel = (IStructuredSelection)queryTree.getSelection();
		queries = new ArrayList<QueryEditorInput>();
		for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
			Object obj = (Object) iterator.next();
			if (obj instanceof Query){
				queries.add(new QueryEditorInput((Query)obj));
			}else if (obj instanceof QueryEditorInput){
				queries.add((QueryEditorInput)obj);
			}
		}
		super.okPressed();
	}
	
	public List<QueryEditorInput> getSelectedQueries(){
		return this.queries;
	}

}
