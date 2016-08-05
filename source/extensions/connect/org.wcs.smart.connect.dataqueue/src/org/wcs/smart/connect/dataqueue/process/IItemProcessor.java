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
package org.wcs.smart.connect.dataqueue.process;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.connect.dataqueue.model.DataQueueItem;
import org.wcs.smart.connect.dataqueue.model.LocalDataQueueItem;

/**
 * Processor for processing data queue items.
 * 
 * @author Emily
 *
 */
public interface IItemProcessor {

	public static final String EXTENSION_ID = "org.wcs.smart.connect.dataqueue.processor"; //$NON-NLS-1$
	
	/**
	 * 
	 * @param type
	 * @return <code>true</code> if the processor can process the provided type.
	 */
	public boolean canProcess(DataQueueItem.Type type);
	
	/**
	 * Processes the item and returns associated status.  Throws an exception
	 * if an error occurred while processing.
	 * 
	 * @param item 
	 * @param monitor
	 * @return ProcessingStatus; if processing status is requeue then the item is requeued on the server
	 * to be processed again later
	 * @throws Exception
	 */
	public ProcessingStatus process(DataQueueItem item, IProgressMonitor monitor) throws Exception;
	
	/**
	 * Processing Status 
	 * @author Emily
	 *
	 */
	class ProcessingStatus{
		LocalDataQueueItem.Status status;
		String message;
		
		public ProcessingStatus(LocalDataQueueItem.Status status, String message){
			this.status = status;
			this.message = message;
		}
		
		public LocalDataQueueItem.Status getStatus(){
			return this.status;
		}
		
		public String getMessage(){
			return this.message;
		}
		
		public void appendToMessage(String append){
			this.message = message + " " + append; //$NON-NLS-1$
		}
	}
}
