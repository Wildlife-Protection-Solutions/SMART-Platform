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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.i2.Intelligence2PlugIn;

/**
 * Attachment search job.  Only a single search job runs at a time.
 * 
 * @author Emily
 *
 */
public class AttachmentSearchJob extends Job {

	private static ISchedulingRule MUTEX = new ISchedulingRule() {
		
		@Override
		public boolean isConflicting(ISchedulingRule rule) {
			return (rule == MUTEX);
		}
		
		@Override
		public boolean contains(ISchedulingRule rule) {
			return (rule == MUTEX);
		}
	};
	
	private List<ISmartAttachment> attachments;
	private IMatchCollector collector;
	private String searchString;
	
	public AttachmentSearchJob(List<ISmartAttachment> attachments, IMatchCollector collector, String searchString){
		super("Search attachments....");
		
		this.attachments = attachments;
		this.collector = collector;
		this.searchString = searchString;
		
		setRule(MUTEX);
	}
	
	
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		
		List<ISmartAttachment> toSearch = new ArrayList<>();
		toSearch.addAll(attachments);
		
		List<IFileSearcher> searchers = AttachmentSearchEngine.INSTANCE.getFileSearchers();
		collector.start();
		try{
			monitor.beginTask("Searching attachments...", searchers.size() * toSearch.size() * 5);
			for (IFileSearcher searcher : searchers){
				for (ISmartAttachment a : toSearch){
					monitor.subTask(MessageFormat.format("text searching {0}",  a.getFilename()));
					searcher.search(searchString, a, collector, new SubProgressMonitor(monitor, 5));
				}
				if (monitor.isCanceled()){
					collector.cancelled();
					return Status.CANCEL_STATUS;
				}
			}
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog(ex.getMessage(), ex);
			return Status.CANCEL_STATUS;
		}finally{
			collector.done();
		}
		
		return Status.OK_STATUS;
	}

}
