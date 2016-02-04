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
package org.wcs.smart.connect.dataqueue.internal.process;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.connect.dataqueue.ConnectDataQueuePlugin;

/**
 * Class to represent the status of the auto data queue processor.
 * @author Emily
 *
 */
public class AutoProcessingStatus {
	
	public enum Status{
		PROCESSING(ConnectDataQueuePlugin.DQ_PROCESSING_ICON),
		ERROR(ConnectDataQueuePlugin.DQ_ERROR_ICON),
		WARNING(ConnectDataQueuePlugin.DQ_WARN_ICON),
		INACTIVE(ConnectDataQueuePlugin.DQ_INACTIVE_ICON),
		OK(ConnectDataQueuePlugin.DQ_OK_ICON);
		
		private String imageKey;
		Status(String imageKey){
			this.imageKey = imageKey;
		}
		public Image getImage(){
			return ConnectDataQueuePlugin.getDefault().getImageRegistry().get(imageKey);
		}
	}
	
	private Status status;
	private String message;
	
	public AutoProcessingStatus(){
		this.message = null;
		this.status = Status.OK;
	}
	
	public void updateStatus(Status status, String message){
		this.status = status;
		this.message = message;
	}
	
	public String getMessage(){
		return this.message;
	}
	public Status getStatus(){
		return this.status;
	}
}
