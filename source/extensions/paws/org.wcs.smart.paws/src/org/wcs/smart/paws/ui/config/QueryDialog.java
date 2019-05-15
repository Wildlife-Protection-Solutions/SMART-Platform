/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.ui.config;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.paws.model.PawsQueryClass;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.common.model.ObservationQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.query.ui.querylist.QueryListLabelProvider;
import org.wcs.smart.ui.SmartStyledDialog;
import org.wcs.smart.ui.properties.DialogConstants;

public class QueryDialog extends SmartStyledDialog {

	private TableViewer queryTree;
	private List<PawsQueryClass> selectedItems;
	
	protected QueryDialog(Shell parent) {
		super(parent);
	}
	
	@Override
	public Point getInitialSize() {
		Point pnt = new Point(400,500);
		return pnt;
	}

	@Override
	public Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout());
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		queryTree = new TableViewer(c, SWT.BORDER | SWT.MULTI);
		queryTree.setContentProvider(ArrayContentProvider.getInstance());
		queryTree.setLabelProvider(new QueryListLabelProvider());
		queryTree.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		queryTree.setInput(new Object[]{DialogConstants.LOADING_TEXT});
		queryTree.addDoubleClickListener(e->{
			okPressed();
		});
		
		loadQueries.schedule();
		
		getShell().setText("SMART Queries");
		return parent;
	}
	
	public void okPressed() {
		selectedItems = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			for (Iterator<Object> iterator = queryTree.getStructuredSelection().iterator(); iterator.hasNext();) {
				Object dmObject = (Object) iterator.next();
				PawsQueryClass item = null;
				if (dmObject instanceof QueryEditorInput) {
					QueryEditorInput c = (QueryEditorInput)dmObject;
					
					item = new PawsQueryClass();
					item.setClassification(c.getQueryName().toLowerCase());
					item.setQueryType(c.getType().getKey());
					item.setQueryUuid(c.getUuid());
					Query q = QueryHibernateManager.getInstance().findQuery(session, c.getUuid(), c.getType());
					item.setCachedQuery(q);
				}else {
					MessageDialog.openWarning(getShell(), "Invalid Selection", MessageFormat.format("Selected item {0} cannot be added.  Please select a query.", dmObject.toString()));
					return;
				}			
				if (item != null) selectedItems.add(item);
			}
		}
		super.okPressed();
	}
	
	public List<PawsQueryClass> getSelectedItems(){
		return this.selectedItems;
	}
	
	@Override
	public void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
	}
	
	@Override
	public boolean isResizable() {
		return true;
	}
	
	private Job loadQueries = new Job("loading queries") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			List<QueryEditorInput> allQueries = new ArrayList<>();
			
			try(Session session = HibernateManager.openSession()){
				HashMap<UUID, List<QueryEditorInput>> queries = QueryHibernateManager.getInstance().getQueryProxies(session);
				for (List<QueryEditorInput> all : queries.values()){
					for (QueryEditorInput qi : all){
						if (qi.isShared() &&
								ObservationQuery.class.isAssignableFrom(qi.getType().getHibernateClass()) ){
							allQueries.add(qi);
						}
					}
				
				}
			}	
			allQueries.sort((a,b)->Collator.getInstance().compare(a.getQueryName(), b.getQueryName()));
			Display.getDefault().asyncExec(()->{
				queryTree.setInput(allQueries);
				queryTree.refresh();
			});
			return Status.OK_STATUS;
		}
		
	};
}
