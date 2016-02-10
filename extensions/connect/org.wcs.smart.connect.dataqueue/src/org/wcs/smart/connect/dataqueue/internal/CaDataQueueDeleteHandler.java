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
package org.wcs.smart.connect.dataqueue.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.DeleteConservationAreaHandler;
import org.wcs.smart.ca.ICaDeleteHandler;
import org.wcs.smart.connect.dataqueue.internal.process.DataQueueManager;

/**
 * Delete handler for deleting smart connect information attached to
 * conservation area.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class CaDataQueueDeleteHandler implements ICaDeleteHandler {

	/**
	 * To be executed before the conservation area and patrol is deleted
	 */
	public static final int EXECUTE_ORDER = DeleteConservationAreaHandler.EXECUTE_ORDER + 1;

	public CaDataQueueDeleteHandler() {
	}

	@Override
	public void beforeDelete(ConservationArea ca, Session session,
			IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.CaDataQueueDeleteHandler_taskMessage);
		//delete data queue items
		DataQueueManager.INSTANCE.deleteDataQueue(ca, session);
		
		//delete any data queue options
		DataQueueManager.INSTANCE.deleteDataQueueOptions(ca, session);
	}

}
