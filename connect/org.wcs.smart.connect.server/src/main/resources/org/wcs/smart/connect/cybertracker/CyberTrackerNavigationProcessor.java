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
package org.wcs.smart.connect.cybertracker;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.wcs.smart.connect.model.CyberTrackerNavigationLayer;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.model.WorkItem.Status;
import org.wcs.smart.connect.model.WorkItem.Type;
import org.wcs.smart.connect.uploader.IUploadItemProcessor;
import org.wcs.smart.hibernate.QueryFactory;

/**
 * Processor for cybertracker packages.
 * 
 * @author Emily
 *
 */
public class CyberTrackerNavigationProcessor implements IUploadItemProcessor {

	private final Logger logger = Logger.getLogger(CyberTrackerNavigationProcessor.class.getName());
	
	@Override
	public Type getSupportedType() {
		return Type.UP_NAVIGATION;
	}

	@Override
	public void processItem(WorkItem item, Session session) {
		//update the status of the data queue item to processing
		try{
			session.beginTransaction();
					
			//cybertracker package
			CyberTrackerNavigationLayer ctPackage = QueryFactory.buildQuery(session, CyberTrackerNavigationLayer.class, 
						"workItem", item.getUuid()).uniqueResult(); //$NON-NLS-1$
			
			//package can now be downloaded
			ctPackage.setStatus(CyberTrackerNavigationLayer.Status.READY);	
			item.setStatus(Status.COMPLETE);
			
			session.saveOrUpdate(item);
			
			session.getTransaction().commit();
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
			session.getTransaction().rollback();
					
			session.beginTransaction();
			item.setStatus(org.wcs.smart.connect.model.WorkItem.Status.ERROR);
			item.setMessage("Error uploading cybertracker package: " + ex.getMessage()); //$NON-NLS-1$
			session.getTransaction().commit();
		}
	}

}
