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

import java.io.File;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.i2.FileCharSequence;
import org.wcs.smart.i2.internal.Messages;

/**
 * Text file searcher
 * 
 * @author Emily
 *
 */
public class TextFileSearcher implements IFileSearcher {

	private static final int TEXT_OFFSET_CHARS = 150;
	
	private static final int MAX_MATCH_COUNT = 50;
	
	@Override
	public void search(String searchString, ISmartAttachment attachment,
			IMatchCollector collector, IProgressMonitor monitor) {
		monitor.beginTask(MessageFormat.format(Messages.TextFileSearcher_TaskName,  attachment.getFilename()), 1);
	
		Matcher fMatcher = Pattern.compile(searchString, Pattern.CASE_INSENSITIVE).matcher(searchString);
		
		int k = 0;
		File file = null;
		if (attachment.getCopyFromLocation() != null){
			file = attachment.getCopyFromLocation();
		}else{
			file = attachment.getAttachmentFile();
		}		
		
		try{
			try(FileCharSequence sequence = new FileCharSequence(file)){
			
				fMatcher.reset(sequence);
				int matchCount = 0;
				int addCount = 0;
				while(fMatcher.find()){
					matchCount++;
					int start= fMatcher.start();
					int end= fMatcher.end();
					if (end != start) { // don't report 0-length matches
						int length = end - start;
						int substart = start - TEXT_OFFSET_CHARS;
						if (substart < 0) substart = 0;
						start = start - substart;
						int subend = end + TEXT_OFFSET_CHARS;
						if (subend > sequence.length()) subend = sequence.length() - 1;
						
						end = start + length;
						if (matchCount <= MAX_MATCH_COUNT){
							addCount++;
							SearchResult result = new SearchResult(attachment, searchString, "..." + sequence.getSubString(substart, subend+1)  + "...", start+3, end+3); //$NON-NLS-1$ //$NON-NLS-2$
							collector.addMatch(result);
						}
					}
					if (k++ == 20) {
						if (monitor.isCanceled()){
							throw new OperationCanceledException(Messages.TextFileSearcher_CanceledMsg);
						}
						k= 0;
					}
				}
				collector.setMatchCount(attachment, matchCount, addCount);
			}
		}catch (Exception ex){
			SearchResult errorResult = new SearchResult(attachment, Messages.TextFileSearcher_ErrorItemName, ex.getMessage(), 0,0);
			collector.addMatch(errorResult);
		}finally{
			monitor.done();
		}
	}

}
