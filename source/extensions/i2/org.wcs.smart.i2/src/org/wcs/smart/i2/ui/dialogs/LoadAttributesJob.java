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
package org.wcs.smart.i2.ui.dialogs;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.IntelAttributeManager;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;

/**
 * Job that can be extended to load attributes and all associated items eagerly.
 * 
 * @author Emily
 *
 */
public abstract class LoadAttributesJob extends Job {

	protected List<IntelAttribute> attributes;
	
	public LoadAttributesJob(){
		super("load entity attributes"); //$NON-NLS-1$
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		attributes = null;
		Session session = HibernateManager.openSession();
		try{
			attributes  = IntelAttributeManager.INSTANCE.getAttributes(session, SmartDB.getCurrentConservationArea());
			for (IntelAttribute ia : attributes){
				ia.getNames().size();
				if (ia.getAttributeList() != null){
					for (IntelAttributeListItem item : ia.getAttributeList()){
						item.getNames().size();
					}
				}
			}
				
		}finally{
			session.close();
		}
			
		afterLoad();
		return Status.OK_STATUS;
	}

	/**
	 * Code executed after attributes are loaded from the database
	 * and the session has been closed
	 */
	public abstract void afterLoad();
	
}
