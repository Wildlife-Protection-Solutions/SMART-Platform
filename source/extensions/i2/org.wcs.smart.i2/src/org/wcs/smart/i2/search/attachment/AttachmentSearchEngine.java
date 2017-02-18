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
package org.wcs.smart.i2.search.attachment;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.jobs.Job;
import org.wcs.smart.common.attachment.ISmartAttachment;

/**
 * SMART Attachments search engine.  Manages attachment searches, enusring only a 
 * single search occurs at a time
 * 
 * @author Emily
 *
 */
public enum AttachmentSearchEngine {
	INSTANCE;

	
	public List<IFileSearcher> getFileSearchers(){
		return Collections.singletonList(new TextFileSearcher());
	}
	
	//TODO: only have on search job running at a time
	/**
	 * Creates and starts a new search job.  
	 * @param toSearch
	 * @param collector
	 * @param searchString
	 * @return
	 */
	public Job searchAttachments(List<ISmartAttachment> toSearch, IMatchCollector collector, String searchString){
		AttachmentSearchJob job = new AttachmentSearchJob(toSearch, collector, searchString);
		job.schedule();
		return job;
		
	}
}
