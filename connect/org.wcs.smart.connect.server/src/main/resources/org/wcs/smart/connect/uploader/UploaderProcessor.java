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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.wcs.smart.connect.model.UploadItem;

/**
 * Thread for running processing tasks for processing uploaded items.
 * 
 * @author Emily
 *
 */
public class UploaderProcessor implements Runnable {

	private final Logger logger = Logger.getLogger(UploaderProcessor.class.getName());
	
	private UploadItem item;
	private Session session;
	private SessionFactory factory;
	
	public UploaderProcessor(UploadItem item, SessionFactory factory){
		this.item = item;
		this.factory = factory;
	}
	@Override
	public void run() {
		session = factory.openSession();
		try{
			ItemProcessManager.INSTANCE.processItem(item, session);
		}catch (Exception ex){
			logger.log(Level.SEVERE, ex.getMessage(), ex);
		}finally{
			session.close();
		}
		
	}

}
