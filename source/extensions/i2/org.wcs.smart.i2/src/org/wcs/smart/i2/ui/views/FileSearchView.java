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
package org.wcs.smart.i2.ui.views;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.i2.FileCharSequence;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.ui.AnimatedGif;
import org.wcs.smart.ui.properties.FilterComposite;

/**
 * View for searching smart text attachments
 * @author Emily
 *
 */
public class FileSearchView {
	
	private static final int MAX_COUNT = 50;

	public static final String ID = "org.wcs.smart.i2.ui.view.filesearch";
	
	private FilterComposite txtSearch;
	private Composite compResults;
	private ScrolledComposite searchResults;
	
	@PostConstruct
	public void createPartControl(Composite parent) {
		
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginHeight = 0;
		((GridLayout)parent.getLayout()).marginWidth = 0;
		parent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		main.setLayout(new GridLayout());
		main.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
//		Label lblHeader = new Label(main, SWT.NONE);
//		lblHeader.setText("File Search");
//		lblHeader.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		Composite search = new Composite(main, SWT.NONE);
		search.setLayout(new GridLayout(2, false));
		((GridLayout)search.getLayout()).marginWidth = 0;
		((GridLayout)search.getLayout()).marginHeight = 0;
		search.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		search.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		Label lblSearch = new Label(search, SWT.NONE);
		lblSearch.setText("Search:");
		lblSearch.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		txtSearch = new FilterComposite(search, SWT.NONE);
		txtSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Button btnSearch = new Button(main, SWT.PUSH);
		btnSearch.setText("Search");
		btnSearch.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, true, false));
		btnSearch.addListener(SWT.Selection, e->doSearch(null));
		
		Label l = new Label(main, SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		searchResults = new ScrolledComposite(main, SWT.V_SCROLL);
		searchResults.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		searchResults.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		compResults = new Composite(searchResults, SWT.NONE);
		compResults.setLayout(new GridLayout());
		compResults.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		compResults.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		searchResults.setContent(compResults);
		int width = searchResults.getBounds().width;
		width = width - searchResults.getVerticalBar().getSize().x;
		compResults.setSize(width, compResults.computeSize(width, SWT.DEFAULT).y);
		
		searchResults.addListener(SWT.Resize, e->{
			int w = searchResults.getBounds().width;
			w = w - searchResults.getVerticalBar().getSize().x;
			compResults.setSize(w, compResults.computeSize(w, SWT.DEFAULT).y);
		});
	}
	
	private List<ISmartAttachment> lastAttachments;
	private boolean isSearching = false;
	private void doSearch(List<ISmartAttachment> attachments){
		if (attachments==null){
			attachments = lastAttachments;
		}
		if (attachments == null || attachments.isEmpty()) return;
		final String searchString = txtSearch.getPatternFilter();
		
		lastAttachments = attachments;
		if (searchString == null || searchString.trim().isEmpty()) return;
		
		
		if (isSearching) return;
//		pService.showPart(part, PartState.ACTIVATE);
		
		isSearching = true;
		
		final Matcher fMatcher = Pattern.compile(searchString).matcher(new String());
		
		
		Job j = new Job("file search job"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				isSearching = true;
				monitor.beginTask("Search attachments", lastAttachments.size());
				MatchCollector collector = new MatchCollector();
				int offset = 200;
				collector.start();
				try{
					for (ISmartAttachment a : lastAttachments){
						int k = 0;
						try{
							CharSequence sequence = new FileCharSequence(a.getAttachmentFile());
							fMatcher.reset(sequence);
							int matchCount = 0;
							int addCount = 0;
							while(fMatcher.find()){
								matchCount++;
								int start= fMatcher.start();
								int end= fMatcher.end();
								if (end != start) { // don't report 0-length matches
									int length = end - start;
									int substart = start - offset;
									if (substart < 0) substart = 0;
									start = start - substart;
									int subend = end + offset;
									if (subend > sequence.length()) subend = sequence.length() - 1;
									
									end = start + length;
									if (matchCount <= MAX_COUNT){
										addCount++;
										SearchResult result = new SearchResult(a, searchString, sequence.subSequence(substart, subend).toString(), start, end);
										collector.addMatch(result);
									}
								}
								if (k++ == 20) {
									if (monitor.isCanceled()) throw new OperationCanceledException("File search cancelled");
									k= 0;
								}
							}
							collector.setMatchCount(a, matchCount, addCount);
						}catch (Exception ex){
							SearchResult errorResult = new SearchResult(a, "ERROR", ex.getMessage(), 0,0);
							collector.addMatch(errorResult);
						}
						monitor.worked(1);
					}
				}finally{
					collector.done();
					isSearching = false;
				}
				monitor.done();
				if (monitor.isCanceled()) return Status.CANCEL_STATUS;
				return Status.OK_STATUS;
			}
			
		};
		
		j.schedule();
		
	}
	
	@Inject
	@Optional
	public void runSearch(@UIEventTopic(IntelEvents.ATTACHMENT_SEARCH) List<ISmartAttachment> attachments) {
	    doSearch(attachments);
	}
	
	
	@Focus
	public void setFocus() {
		txtSearch.setFocus();
	}
	
	public static class FileSearchViewWrapper extends DIViewPart<FileSearchView>{
		public FileSearchViewWrapper() {
			super(FileSearchView.class);
		}
	}
	
	private class MatchCollector{
		
		private boolean isDone = false;
		
		private boolean first = true;
		
		private List<SearchResult> allResults;
		private List<SearchResult> currentResults;
		
		private AnimatedGif lblProgress;
		private HashMap<ISmartAttachment, Composite> attachmentsToComp;
		
		public void start(){
			attachmentsToComp = new HashMap<>();
			allResults = new ArrayList<>();
			currentResults = new ArrayList<>();
			
			Display.getDefault().syncExec(new Runnable() {
				
				@Override
				public void run() {
					for (Control c : compResults.getChildren()){
						c.dispose();
					}
					
					Path p = new Path(AnimatedGif.ICON_PROGRESS);
					try(InputStream is = FileLocator.openStream(Intelligence2PlugIn.getDefault().getBundle(), p, false)){
						lblProgress = new AnimatedGif(compResults,is);
						lblProgress.setBackground(lblProgress.getDisplay().getSystemColor(SWT.COLOR_WHITE));
						lblProgress.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
						((GridData)lblProgress.getLayoutData()).widthHint = lblProgress.getImageSize().x;
						((GridData)lblProgress.getLayoutData()).heightHint = lblProgress.getImageSize().y;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					compResults.layout();
					compResults.setSize(compResults.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				}
			});
		}
		
		
		public void addMatch(SearchResult result){
			if (first) j.schedule();
			first = false;
			synchronized (allResults) {
				allResults.add(result);
				currentResults.add(result);
			}
		}
		
		public void setMatchCount(ISmartAttachment attachment, Integer matchCount, Integer displayedCount){
			Display.getDefault().asyncExec(() ->{
				Composite c = attachmentsToComp.get(attachment);
				if (c == null){
					c = createHeaderComp(compResults, attachment);
				}
				
				Label l = (Label)c.getChildren()[0];
				l.setText(MessageFormat.format("{0} ({1} of {2})", attachment.getFilename(), displayedCount, matchCount));
			});
		
			
		}
		
		public void done(){
			if (first) j.schedule();
			first = false;
			isDone = true;
		}
		
		private Composite createHeaderComp(Composite parent, ISmartAttachment attachment){
			Composite c = new Composite(parent, SWT.NONE);
			attachmentsToComp.put(attachment, c);
			c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			c.setLayout(new GridLayout());
			Label l = new Label(c, SWT.NONE);
			l.setText(attachment.getFilename());
			c.setBackground(c.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			l.setBackground(c.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			
			FontData fd = l.getFont().getFontData()[0];
			fd.setStyle(SWT.BOLD);
			Font f = new Font(l.getDisplay(), fd);
			l.setFont(f);
			l.addListener(SWT.Dispose, e->f.dispose());
			
			l = new Label(c, SWT.SEPARATOR | SWT.HORIZONTAL);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			return c;
		}
		
		
		Job j = new Job("update match results"){
			Runnable run = new Runnable(){

				@Override
				public void run() {
					
					List<SearchResult> internalItems = new ArrayList<>();
					synchronized (allResults) {
						internalItems.addAll(currentResults);
						currentResults.clear();
					}
					
					for (SearchResult i : internalItems){
						
						Composite c = attachmentsToComp.get(i.getAttachment());
						if (c == null) c = createHeaderComp(compResults, i.getAttachment());
						
						StyledText l = new StyledText(c, SWT.WRAP);
						l.setText(i.getFullString());
						l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
						l.setBackground(c.getDisplay().getSystemColor(SWT.COLOR_WHITE));
						l.setEditable(false);
						StyleRange range = new StyleRange();
						range.start = i.getStart();
						range.length = i.getEnd() - i.getStart();
						range.fontStyle = SWT.BOLD;
						range.background = l.getDisplay().getSystemColor(SWT.COLOR_YELLOW);
						if (range.length > 0) l.setStyleRange(range);
						Label ll = new Label(c, SWT.SEPARATOR | SWT.HORIZONTAL);
						ll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
					}
					if (isDone)lblProgress.dispose();
					
					compResults.layout(true);
					int width = searchResults.getBounds().width;
					width = width - searchResults.getVerticalBar().getSize().x;
					compResults.setSize(width, compResults.computeSize(width, SWT.DEFAULT).y);
				}
				
			};
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				while(!isDone){
					Display.getDefault().syncExec(run);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				Display.getDefault().syncExec(run);
				return Status.OK_STATUS;
			}
			
		};
	}
	
	private class SearchResult{
		private ISmartAttachment attachment;
		private String matchString;
		private String fullString;
		
		private int start;
		private int end;
		
		public SearchResult(ISmartAttachment attachment, String matchString, String fullString, int start, int end){
			this.attachment = attachment;
			this.matchString = matchString;
			this.fullString = fullString;
			this.start = start;
			this.end = end;
		}
		
		public int getStart(){
			return this.start;
		}
		
		public int getEnd(){
			return this.end;
		}
		
		public ISmartAttachment getAttachment(){
			return this.attachment;
		}
		public String getFullString(){
			return this.fullString;
		}
	}
	
	
}
