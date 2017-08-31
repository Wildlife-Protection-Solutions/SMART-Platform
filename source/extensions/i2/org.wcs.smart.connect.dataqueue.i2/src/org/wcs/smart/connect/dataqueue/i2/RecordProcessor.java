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
package org.wcs.smart.connect.dataqueue.i2;

import java.nio.file.Path;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.connect.dataqueue.i2.internal.Messages;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;
import org.wcs.smart.connect.dataqueue.process.IItemProcessor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.xml.RecordXmlImporter;

/**
 * Independent incident data queue processor for importing
 * incidents.
 * 
 * @author Emily
 *
 */
public class RecordProcessor implements IItemProcessor {

	public static final String RECORD_XML = "I2_RECORD_XML"; //$NON-NLS-1$

	public RecordProcessor() {
	}

	@Override
	public boolean canProcess(String type) {
		return type.toUpperCase().equals(RECORD_XML);
	}

	@Override
	public ProcessingStatus process(DataQueueItem item, IProgressMonitor monitor)
			throws Exception {
		LocalDataQueueItem litem = (LocalDataQueueItem)item;
		Path file = litem.getFullFilePath();
		
		IEventBroker broker = SmartPlugIn.getDefault().getWorkbench().getService(IEclipseContext.class).get(IEventBroker.class);
		try(Session session = HibernateManager.openSession()){
			RecordXmlImporter importer = new RecordXmlImporter(session);
			
			importer.importRecord(file, monitor);
			Object[] results = importer.finishSingleTransaction(broker);
			if (results == null) {
				return new ProcessingStatus(LocalDataQueueItem.Status.ERROR, Messages.RecordProcessor_cancelled);		
			}else {
				int value = (int) results[0];
				if (value == 0) {
					return new ProcessingStatus(LocalDataQueueItem.Status.COMPLETE_WARN, (String)results[1]);	
				}else if (value == 1) {
					return new ProcessingStatus(LocalDataQueueItem.Status.COMPLETE, (String)results[1]);
				}else {
					return new ProcessingStatus(LocalDataQueueItem.Status.COMPLETE_WARN, Messages.RecordProcessor_unknowncode + (String)results[1]);
				}
			}
		}

		
	}

}