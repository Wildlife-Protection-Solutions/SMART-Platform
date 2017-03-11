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
package org.wcs.smart.i2.search;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntitySearch;

import com.ibm.icu.text.Collator;

/**
 * Job for loading searches saved in the database
 * 
 * @author Emily
 *
 */
public abstract class LoadSavedSearches extends Job{

	public LoadSavedSearches() {
		super(Messages.LoadSavedSearches_jobname);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		beforeSearch();
		List<SearchProxy> searches = new ArrayList<SearchProxy>();
		Session session = HibernateManager.openSession();
		try{
			List<IntelEntitySearch> objects = session.createCriteria(IntelEntitySearch.class)
			.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
			.list();
			
			objects.forEach(o -> searches.add(new SearchProxy(o.getUuid(), o.getName())));
			
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(Messages.LoadSavedSearches_errorMsg + ex.getMessage() , ex);
			return Status.OK_STATUS;
		}finally{	
			session.close();
		}
		searches.sort((a,b)->Collator.getInstance().compare(a.getName().toLowerCase(), b.getName().toLowerCase()));
		searchesLoaded(searches);
		
		return Status.OK_STATUS;
	}

	/**
	 * Executed before the search is performed
	 */
	protected void beforeSearch(){ }
	
	protected abstract void searchesLoaded(List<SearchProxy> queries);

}
