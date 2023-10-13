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
package org.wcs.smart.icon.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ui.Thumbnail;


/**
 * Table for displaying a collection of icon files
 * 
 * @author Emily
 *
 */
public class IconTable extends Composite implements Listener {

	private ThumbnailComposite thumb;
	
	private ScrolledForm infoSection;
	
	private int currentIndex = 0;
	private int pageSize = 5;
	private int thumbnailSize = 50;
	
	private List<IconFile> attachments;
	
	private Job loadImagesJob = new Job("loading images") { //$NON-NLS-1$
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (currentIndex == 0) {
				//starting at beginning - clear any existing thumbs 
				Display.getDefault().syncExec(()->{
					if (thumb != null) thumb.dispose();
					thumb = new ThumbnailComposite(infoSection.getBody());
					thumb.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
					thumb.setBackground(getBackground());
					infoSection.setOrigin(0, 0);	
				});
				
			}
			while(true){
				//create spaces for remaining images
				if (attachments == null) return Status.CANCEL_STATUS;
				if (monitor.isCanceled()) return Status.CANCEL_STATUS;
				monitor.setTaskName(currentIndex + "/" + attachments.size()); //$NON-NLS-1$
				if (currentIndex >= attachments.size()) {
					break;
				}
				List<IconFile> items = attachments.subList(currentIndex, Math.min(currentIndex + pageSize, attachments.size()));
				currentIndex += pageSize;
				
				final boolean[] needmore = new boolean[] {false};
				
				Display.getDefault().syncExec(()->{
					thumb.addFiles(items);
					thumb.createThumbs();
					if (getParent().isDisposed()) return;
					getParent().layout(true, true);
					needmore[0] = needsToLoad();
					
					resize();
				});
				if (monitor.isCanceled()) return Status.CANCEL_STATUS;
				if (!needmore[0]) break;	
			}
			
			
			return Status.OK_STATUS;
		}		
	};
	
	
	
	public IconTable(Composite parent, int style){
		super(parent, style);
		
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		

		infoSection = new ScrolledForm(this, SWT.V_SCROLL);
		infoSection.setExpandHorizontal(true);
		infoSection.setExpandVertical(true);
		infoSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		infoSection.addListener(SWT.Resize, this);
		
		GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = gl.marginHeight = 0;
		gl.horizontalSpacing = 0;
		infoSection.getBody().setLayout(gl);
		
		//ensure we manually dispose of images
		//as we reuse them if we can
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (thumb == null) return;
				if (thumb.thumbs == null) return;
				for (IconTable.ThumbnailComposite.ThumbInfo t : thumb.thumbs){
					if (t.thumb != null) t.thumb.disposeImage();
				}
			}
		});
		infoSection.getVerticalBar().addListener(SWT.Selection, e->{
			if (thumb == null) return;
			scheduleLoadJob();
		});
	}

	
	/**
	 * Set the results to display 
	 * 
	 * @param resultSet
	 */
	public void setAttachments(List<IconFile> attachments) {
		loadImagesJob.cancel();
		Display.getDefault().syncExec(()->{
			this.attachments = attachments;
			currentIndex = 0;
			loadImagesJob.schedule();
		});
	}
	
	@Override
	public void handleEvent(Event event) {
		if (event == null || event.type == SWT.Resize){
			if (infoSection == null || infoSection.isDisposed()) return;
			if (thumb != null){
				scheduleLoadJob();
				resize();
			}
		}
	}
	
	/*
	 * schedule load images job if job is not already running
	 * 
	 */
	private void scheduleLoadJob() {
		if (needsToLoad()) {
			if (loadImagesJob.getState() != Job.RUNNING) loadImagesJob.schedule();
		}
	}
	
	/*
	 * determine if we have more images to load
	 */
	private boolean needsToLoad() {
		if (attachments == null) return false;
		if (currentIndex > attachments.size()) return false;
		int y = infoSection.getSize().y + infoSection.getVerticalBar().getSelection();
		int columns = infoSection.getClientArea().width / (thumbnailSize + ((RowLayout)thumb.getLayout()).spacing);
		int rows = y  / (thumbnailSize + ((RowLayout)thumb.getLayout()).spacing) + 1;
		return columns * rows > currentIndex;
	}
	
	private void resize() {
		Rectangle r = infoSection.getClientArea();
		
		Point p = thumb.computeSize(r.width, SWT.DEFAULT);
		
		int columns = infoSection.getClientArea().width / (thumbnailSize + ((RowLayout)thumb.getLayout()).spacing);
		int rows = attachments.size() / columns + 1;
		int height = rows * ( thumbnailSize + ((RowLayout)thumb.getLayout()).spacing) + ((RowLayout)thumb.getLayout()).spacing;
		p.y = height;
		thumb.setSize(p);
	    infoSection.setMinSize(p);
	}

	
	/*
	 * A composite for thumbnails
	 */
	private class ThumbnailComposite extends Composite  {
		private List<ThumbInfo> thumbs;
		
		public ThumbnailComposite(Composite parent){
			super(parent, SWT.NONE);
			setLayout(new RowLayout());
			((RowLayout)getLayout()).wrap = true;
		}

		
		public void addFiles(List<IconFile> fileNames) {
			if (thumbs == null) thumbs = new ArrayList<IconTable.ThumbnailComposite.ThumbInfo>();
			for (IconFile a : fileNames)thumbs.add(new ThumbInfo(a));
		}
		
		private void createThumbs(){
			if (thumbs == null) return;
			if (isDisposed()) return;
			
			for (ThumbInfo t : thumbs){
				if (t.thumbGui != null) continue;
				Composite parent = new Composite(this, SWT.NONE);
				parent.setLayoutData(new RowData(thumbnailSize, thumbnailSize));
				if (t.thumb == null) t.createThumb();
				Composite thumbNameComp = t.thumb.createThumbnail(parent, 0, SWT.NONE, false);
				thumbNameComp.setToolTipText(t.file.getIcon().getName());
				thumbNameComp.setData(t);
				thumbNameComp.setBackground(getDisplay().getSystemColor(SWT.COLOR_WHITE));
				
				t.thumbGui = thumbNameComp;
			}
		}
		
		private class ThumbInfo { 
			IconFile file;
			Thumbnail thumb;
			Composite thumbGui;
			
			public ThumbInfo(IconFile file){
				this.file = file;
			}
			
			public void createThumb(){
				if (thumb == null){
					thumb = new Thumbnail(file, thumbnailSize, false);
					thumb.setImageName(file.getIcon().getName());
					
				}
			}
		}
	}
}
