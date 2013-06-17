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
package org.wcs.smart.query.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.IAreaModifiedListener;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.SimpleQuery;
import org.wcs.smart.query.parser.filter.IFilter;
import org.wcs.smart.query.parser.internal.filter.AreaFilter;

/**
 * An area change listener for query editors that edit simple queries.
 * <p>This listener regenerates filter drop items and refreshes the query</p>
 * @author Emily
 *
 */
public class SimpleQueryAreaChangeListener implements IAreaModifiedListener{

	private IQueryEditor queryEditor = null;
	
	public SimpleQueryAreaChangeListener(IQueryEditor queryEditor){
		this.queryEditor = queryEditor;
	}
	
	private SimpleQuery getQuery(){
		return (SimpleQuery) queryEditor.getQuery();
	}
	@Override
	public void areasUpdated(AreaType type) {
		if (getQuery() == null){
			return;
		}
		if (getQuery().getFilter() == null){
			return;
		}
		List<IFilter> filters = new ArrayList<IFilter>();
		filters.add(getQuery().getFilter());
		boolean foundArea = false;
		while(filters.size() > 0){
			IFilter filter = filters.remove(0);
			if (filter instanceof AreaFilter){
				foundArea = true;
				break;
			}
			if (filter.getChildren() != null){
				filters.addAll(filter.getChildren());
			}
		}
		if (!foundArea){
			return;
		}
		//running it its own job so it has its own hibernate session
		//and does not interfere with other sessions.
		Job j = new Job("update drop items") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				final Session session = HibernateManager.openSession();
				try{
					Display.getDefault().syncExec(new Runnable(){
						@Override
						public void run() {
							try{
								getQuery().generateDropItems(session);
							}catch (Exception ex){
								QueryPlugIn.log(ex.getMessage(), ex);
							}
						}});
				}finally{
					session.close();
				}
				return Status.OK_STATUS;
			}
		};
		j.setSystem(true);
		j.schedule();
		try {
			j.join();
		} catch (InterruptedException e) {
			QueryPlugIn.log(e.getMessage(), e);
		}
		
		QueryEventManager.getInstance().fireQueryRefreshListeners(getQuery());
	}
};
