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

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.tools.compat.parts.DIViewPart;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
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
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.forms.widgets.Twistie;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.search.attachment.AttachmentSearchEngine;
import org.wcs.smart.i2.search.attachment.IMatchCollector;
import org.wcs.smart.i2.search.attachment.SearchResult;
import org.wcs.smart.i2.ui.AnimatedGif;
import org.wcs.smart.ui.properties.FilterComposite;

/**
 * View for searching smart text attachments
 * @author Emily
 *
 */
public class FileSearchView {

	private static final String SECTION_LABEL_KEY = "LABEL"; //$NON-NLS-1$

	private static final String SECTION_STATE_KEY = "STATE"; //$NON-NLS-1$

	public static final String ID = "org.wcs.smart.i2.ui.view.filesearch"; //$NON-NLS-1$
	
	private Object SEARCHJOB_SYNC = new Object();
	
	private FilterComposite txtSearch;
	private Composite compResults;
	private ScrolledComposite searchResults;
	
	private Label lblFiles;
	
	private List<ISmartAttachment> lastAttachments;
	private IMatchCollector collector = new MatchCollector();
	private Job searchJob = null;
	
	public static void doSearch(IEclipseContext context, List<? extends ISmartAttachment> attachments){
		context.get(EPartService.class).showPart(FileSearchView.ID, PartState.ACTIVATE);
		context.get(IEventBroker.class).send(IntelEvents.ATTACHMENT_SEARCH, attachments);
	}
	
	
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
		search.setLayout(new GridLayout());
		((GridLayout)search.getLayout()).marginWidth = 0;
		((GridLayout)search.getLayout()).marginHeight = 0;
		search.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		search.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		lblFiles = new Label(search, SWT.WRAP);
		lblFiles.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		txtSearch = new FilterComposite(search, SWT.NONE);
		txtSearch.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtSearch.getControl().addListener(SWT.KeyDown, e->{
			if (e.character == SWT.CR){
				runSearch(null);
			}
		});
		
		Button btnSearch = new Button(search, SWT.PUSH);
		btnSearch.setText(Messages.FileSearchView_SerachBtn);
		btnSearch.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
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
	

	
	private void doSearch(List<ISmartAttachment> attachments){
		if (attachments == null){
			attachments = lastAttachments;
		}
		if (attachments == null || attachments.isEmpty()) return;
		lastAttachments = attachments;
		
		StringBuilder files = new StringBuilder();
		files.append(MessageFormat.format(Messages.FileSearchView_FilesLabel, attachments.size()));
		files.append(" "); //$NON-NLS-1$
		for (ISmartAttachment m : attachments){
			files.append(m.getFilename() + ", "); //$NON-NLS-1$
		}
		files.delete(files.length() - 2, files.length());
		lblFiles.setText(files.toString());
		((GridData)lblFiles.getLayoutData()).widthHint = lblFiles.getParent().getBounds().width;
		lblFiles.getParent().layout(true);
		final String searchString = txtSearch.getPatternFilter();
		if (searchString == null || searchString.trim().isEmpty()) return;
		
		synchronized (SEARCHJOB_SYNC) {
			if (searchJob != null){
				searchJob.cancel();
				searchJob = null;
			}
			searchJob = AttachmentSearchEngine.INSTANCE.searchAttachments(attachments, collector, searchString);
		}
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
	
	
	private Composite progressComposite;
	private void startSearch(){
		attachmentsToComp = new HashMap<ISmartAttachment, Composite>();
		//dispose of existing match results
		for (Control c : compResults.getChildren()){
			c.dispose();
		}
		//show animation icon
		progressComposite = new Composite(compResults, SWT.NONE);
		progressComposite.setLayout(new GridLayout(2, false));
		progressComposite.setBackground(progressComposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		((GridLayout)progressComposite.getLayout()).marginWidth = 0;
		((GridLayout)progressComposite.getLayout()).marginHeight = 0;
		
		Path p = new Path(AnimatedGif.ICON_PROGRESS);
		try(InputStream is = FileLocator.openStream(Intelligence2PlugIn.getDefault().getBundle(), p, false)){
			AnimatedGif lblProgress = new AnimatedGif(progressComposite,is);
			lblProgress.setBackground(lblProgress.getDisplay().getSystemColor(SWT.COLOR_WHITE));
			lblProgress.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			((GridData)lblProgress.getLayoutData()).widthHint = lblProgress.getImageSize().x;
			((GridData)lblProgress.getLayoutData()).heightHint = lblProgress.getImageSize().y;
		} catch (IOException e) {
		}
		
		Hyperlink h = new Hyperlink(progressComposite, SWT.NONE);
		h.setText(Messages.FileSearchView_CancelLink);
		h.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		h.setForeground(h.getDisplay().getSystemColor(SWT.COLOR_LINK_FOREGROUND));
		h.setBackground(h.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		h.addHyperlinkListener(new HyperlinkAdapter(){
			public void linkActivated(HyperlinkEvent e) {
				synchronized (SEARCHJOB_SYNC) {
					if (searchJob != null){
						h.setEnabled(false);
						searchJob.cancel();
					}
				}
			}
			
		});
		layoutResults();
	}
	
	private HashMap<ISmartAttachment, Composite> attachmentsToComp;
	
	private Composite createFileHeaderComp(Composite parent, ISmartAttachment attachment){
		final Composite c = new Composite(parent, SWT.NONE);
		attachmentsToComp.put(attachment, c);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		c.setLayout(new GridLayout());
		
		Composite header = new Composite(c, SWT.NONE);
		header.setLayout(new GridLayout(2, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		
		Twistie img = new Twistie(header, SWT.NONE);
		img.setExpanded(true);
		
		final Label l = new Label(header, SWT.NONE);
		l.setData(SECTION_STATE_KEY, true);
		l.setText(attachment.getFilename());
		
		img.setBackground(c.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		header.setBackground(c.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		c.setBackground(c.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		l.setBackground(c.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		Font f = new Font(l.getDisplay(), fd);
		l.setFont(f);
		l.addListener(SWT.Dispose, e->f.dispose());
		
		Label sep = new Label(c, SWT.SEPARATOR | SWT.HORIZONTAL);
		sep.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Listener clickListener = e-> {
				Boolean state = (Boolean)l.getData(SECTION_STATE_KEY);
				state = !state;
				img.setExpanded(state);
				for (Control kid: c.getChildren()){
					if (kid != header && kid != sep){
						kid.setVisible(state);
						((GridData)kid.getLayoutData()).exclude = !state;
					}
				}
				c.pack();
				l.setData(SECTION_STATE_KEY, state);
				layoutResults();
		};
		l.addListener(SWT.MouseUp, clickListener);
		l.setCursor(l.getDisplay().getSystemCursor(SWT.CURSOR_HAND));
		img.addListener(SWT.MouseUp, clickListener);
			
		c.setData(SECTION_LABEL_KEY, l);
		return c;
	}
	
	private void addSearchResults(List<SearchResult> results){
		for (SearchResult i : results){
			
			Composite c = attachmentsToComp.get(i.getAttachment());
			if (c == null) c = createFileHeaderComp(compResults, i.getAttachment());
			
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
		layoutResults();
	}
	
	private void setSearchCount(ISmartAttachment attachment, Integer matchCount, Integer displayCount){
		Composite c = attachmentsToComp.get(attachment);
		if (c == null){
			c = createFileHeaderComp(compResults, attachment);
		}
		Label l = (Label)c.getData(SECTION_LABEL_KEY);
		l.setText(MessageFormat.format(Messages.FileSearchView_SearchResults_Filename_displaycnt_matchCnt, attachment.getFilename(), displayCount, matchCount));
		l.getParent().layout(true);
		
		layoutResults();
	}
	
	private void addCancelDetails(){
		Composite cancelComposite = new Composite(compResults, SWT.NONE);
		cancelComposite.setLayout(new GridLayout(2, false));
		((GridLayout)cancelComposite.getLayout()).marginWidth = 0;
		((GridLayout)cancelComposite.getLayout()).marginHeight = 0;
		cancelComposite.setBackground(compResults.getBackground());
		Label l = new Label(cancelComposite, SWT.NONE);
		l.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK));
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		l.setBackground(compResults.getBackground());
		l = new Label(cancelComposite, SWT.NONE);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		l.setText(Messages.FileSearchView_CancelledLabel);
		l.setBackground(compResults.getBackground());
		cancelComposite.moveAbove(compResults.getChildren()[0]);
	}
	
	
	private void finishSearch(){
		progressComposite.dispose();
		layoutResults();
		synchronized (SEARCHJOB_SYNC) {
			searchJob = null;
		}
	}
	
	private void layoutResults(){
		compResults.layout();
		int width = searchResults.getBounds().width;
		width = width - searchResults.getVerticalBar().getSize().x;
		compResults.setSize(width, compResults.computeSize(width, SWT.DEFAULT).y);
	}
	
	
	
	private class MatchCollector implements  IMatchCollector{

		private List<SearchResult> allResults;
		private List<SearchResult> displayResults;
		
		private Job updateUiJob = new Job(Messages.FileSearchView_updateUiJob){		
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				while(!monitor.isCanceled()){
					List<SearchResult> internalItems = new ArrayList<>();
					synchronized (displayResults) {
						internalItems.addAll(displayResults);
						displayResults.clear();
					}
					if (!internalItems.isEmpty()){
						runDisplay(()->addSearchResults(internalItems));
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						Intelligence2PlugIn.log(e.getMessage(), e);
					}
				}
				return Status.OK_STATUS;
			}
		};
		
		public MatchCollector(){
			updateUiJob.setSystem(true);
		}
		
		@Override
		public void start() {
			updateUiJob.schedule();
			allResults = new ArrayList<>();
			displayResults = new ArrayList<>();
			runDisplay(()->startSearch());
		}

		@Override
		public void done() {
			updateUiJob.cancel();
			List<SearchResult> copy = new ArrayList<>();
			synchronized (displayResults) {
				copy.addAll(displayResults);
				displayResults.clear();
			}
			
			if (!copy.isEmpty()) runDisplay(()->addSearchResults(copy));
			runDisplay(()->finishSearch());
		}

		@Override
		public void cancelled() {
			done();
			runDisplay(()->addCancelDetails());			
		}

		@Override
		public void addMatch(SearchResult result) {
			allResults.add(result);
			synchronized (displayResults) {
				displayResults.add(result);
			}
			
		}

		@Override
		public void setMatchCount(ISmartAttachment attachment, Integer matchCount, Integer displayCount) {
			runDisplay(()->setSearchCount(attachment, matchCount, displayCount));
		}
		
		private void runDisplay(Runnable r){
			Display.getDefault().asyncExec(r);
		}
	}
	
}
