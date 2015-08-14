/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.uploader;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.connect.model.UploadItem;
import org.wcs.smart.connect.model.UploadItem.Status;

/**
 * Manages the processing of uploaded items.
 * 
 * @author Emily
 *
 */
public enum ItemProcessManager {

	INSTANCE;
	
	private static List<IUploadItemProcessor> processors = new ArrayList<IUploadItemProcessor>();
	static{
		processors.add(new LoadCaProcessor());
	};
	
	
	public void processItem(UploadItem item, Session session) throws Exception{
		
		//update status
		session.beginTransaction();
		session.update(item);
		item.setStatus(Status.PROCESSING);
		session.getTransaction().commit();
		
		IUploadItemProcessor processor = findProcessor(item);
		if (processor == null){
			item.setStatus(Status.ERROR);
			item.setMessage(MessageFormat.format("No processor found for the file type {0}", item.getType().toString()));
		}
		processor.processItem(item, session);
	}
	
	private IUploadItemProcessor findProcessor(UploadItem item){
		for (IUploadItemProcessor p : processors){
			if (p.getSupportedType().equals(item.getType())){
				return p;
			}
		}
		return null;
	}
}
