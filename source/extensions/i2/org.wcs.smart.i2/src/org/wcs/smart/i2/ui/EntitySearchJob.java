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
package org.wcs.smart.i2.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.search.BasicEntitySearch;
import org.wcs.smart.i2.search.IIntelEntitySearch;
import org.wcs.smart.i2.search.IntelSearchResult;

/**
 * Entity search job.
 * 
 * @author Emily
 *
 */
public abstract class EntitySearchJob extends Job{

	private IIntelEntitySearch search = new BasicEntitySearch(null);
	
	public EntitySearchJob() {
		super("Entity Search");
	}

	public void setSearch(IIntelEntitySearch search){
		this.search = search;
	}
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		monitor.beginTask("Entity Search", 3);
		beforeSearch(new SubProgressMonitor(monitor, 1));
		
		IntelSearchResult entities = null; 
		Session s = HibernateManager.openSession();
		try{
			entities = search.doSearch(s, new SubProgressMonitor(monitor, 1));
		}catch (Exception ex){
			Intelligence2PlugIn.log(ex.getMessage(), ex);
			onError(ex);
			return Status.OK_STATUS;
		}finally{
			s.close();
		}
		if (monitor.isCanceled()) return Status.CANCEL_STATUS;
		
		afterSearch(entities, new SubProgressMonitor(monitor, 1));
		
		return Status.OK_STATUS;
	}

	
	public abstract void onError(Exception ex);
	
	public abstract void afterSearch(IntelSearchResult entities, IProgressMonitor monitor);
	
	public abstract void beforeSearch(IProgressMonitor monitor);
}
