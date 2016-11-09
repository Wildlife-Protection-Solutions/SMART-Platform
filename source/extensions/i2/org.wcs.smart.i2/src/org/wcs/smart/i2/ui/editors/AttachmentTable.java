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
package org.wcs.smart.i2.ui.editors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelEntityAttachment;
import org.wcs.smart.i2.ui.I2SwtUtils;
import org.wcs.smart.ui.Thumbnail;

import com.ibm.icu.text.DateFormat;

/**
 * Table for displaying list of attachments.
 * '
 * @author Emily
 *
 */
public class AttachmentTable extends Composite implements Listener {

	private ThumbnailComposite thumb;
	private List<ISmartAttachment> attachments;
	private FormToolkit toolkit;
	
	private ScrolledForm infoSection;
	
	private Menu thumbMenu;
	
	public AttachmentTable(Composite parent, FormToolkit toolkit, IMenuCreator thumbsMenu, int style){
		super(parent, style);
		this.toolkit = toolkit;
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		infoSection = toolkit.createScrolledForm(this);
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
				if (thumb != null && thumb.thumbs != null){
					for (org.wcs.smart.i2.ui.editors.AttachmentTable.ThumbnailComposite.ThumbInfo t : thumb.thumbs){
						if (t.thumb != null) t.thumb.disposeImage();
					}
				}
			}
		});
		if (thumbsMenu != null) thumbMenu = thumbsMenu.createMenu(this); 
	}
	
	public AttachmentTable(Composite parent, FormToolkit toolkit, IMenuCreator thumbsMenu){
		this(parent, toolkit, thumbsMenu, SWT.NONE);
	}
	
	
	@Override
	public void handleEvent(Event event) {
		if (event == null || event.type == SWT.Resize){
			if (infoSection.isDisposed()) return;
			if (thumb != null){
				int mainWidth = infoSection.getClientArea().width - infoSection.getVerticalBar().getSize().x;
				int width = infoSection.getSize().x - infoSection.getVerticalBar().getSize().x;	
				int cols = (int)Math.floor(width / 100.0);
				if (cols < 1) cols = 1;
				thumb.updateLayout(cols);
				((GridData)thumb.getLayoutData()).widthHint = mainWidth;
				thumb.layout(true);
				infoSection.getBody().layout(true);
				infoSection.reflow(true);
			}
		}
	
	}
	
	public void setAttachments(List<ISmartAttachment> attachments){
		this.attachments = attachments;
		redraw.schedule();
	}
	
	
	
	private Job redraw = new Job("redraw thumbnails"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					if (thumb == null){
						thumb = new ThumbnailComposite(AttachmentTable.this.infoSection.getBody());
						infoSection.setContent(thumb);
						toolkit.adapt(thumb);
					}else{
						for (Control c : thumb.getChildren()){
							c.setMenu(null);
							c.dispose();
						}
					}
				}
				
			});
			
			if (attachments != null && !attachments.isEmpty()){
				thumb.setFiles(attachments);
				thumb.initThumbs();
				
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						thumb.createThumbs();
						handleEvent(null);
					}
					
				});
			}
			return Status.OK_STATUS;
		}
		
	};
	
	public List<IntelAttachment> getSelection(){
		List<IntelAttachment> sel = new ArrayList<IntelAttachment>();
		for (ThumbnailComposite.ThumbInfo t : thumb.thumbs){
			if (t.isSelected) sel.add((IntelAttachment) t.file);
		}
		return sel;
	}
	
	/*
	 * A composite for thumbnails
	 */
	private class ThumbnailComposite extends Composite {
		private List<ThumbInfo> thumbs;
		
		public ThumbnailComposite(Composite parent){
			super(parent, SWT.NONE);
			GridLayout gl = new GridLayout();
			gl.marginWidth = gl.marginHeight = 0;
			setLayout(gl);
			
			setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		}

		public void updateLayout(int numCols){
			GridLayout gl = new GridLayout(numCols, false);
			gl.marginWidth = gl.marginHeight = 0;
			setLayout(gl);
		}
		
		public void setFiles(List<ISmartAttachment> fileNames){
			if (thumbs == null){
				thumbs = new ArrayList<AttachmentTable.ThumbnailComposite.ThumbInfo>();
				for (ISmartAttachment a : fileNames){
					thumbs.add(new ThumbInfo(a));
				}
			}else{
				//let's merge file
				List<ThumbInfo> old = thumbs;
				thumbs = new ArrayList<AttachmentTable.ThumbnailComposite.ThumbInfo>();
				
				for (ISmartAttachment file : fileNames){
					ThumbInfo found = null;
					for (ThumbInfo o : old){
						if (o.file.equals(file)){
							found = o;
							break;
						}
					}
					if (found != null){
						old.remove(found);
						thumbs.add(found);
					}else{
						thumbs.add(new ThumbInfo(file));
					}
				}
				for (ThumbInfo i : old){
					if (i.thumb != null){
						//dispose of unused images
						i.thumb.disposeImage();
					}
				}
			}
		}
		
		public void initThumbs(){
			if (thumbs == null) return;
			for (ThumbInfo info : thumbs){
				info.createThumb();
			}
		}
		
		public void createThumbs(){
			if (thumbs == null) return;
			if (isDisposed()) return;
			for (ThumbInfo t : thumbs){
				Composite parent = toolkit.createComposite(this, SWT.NONE);
				parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

				Composite thumbNameComp = t.thumb.createThumbnail(parent, SWT.NONE);
				if (thumbMenu != null) thumbNameComp.setMenu(thumbMenu);
				thumbNameComp.setToolTipText(t.tooltip);
				thumbNameComp.setData(t);
				thumbNameComp.addDisposeListener(new DisposeListener() {
					
					@Override
					public void widgetDisposed(DisposeEvent e) {
						thumbNameComp.setMenu(null);
					}
				});
				Listener l = new Listener(){
					@Override
					public void handleEvent(Event event) {
						if (event.type == SWT.MouseEnter){
							t.isMouseIn = true;
							thumbNameComp.redraw();
						}else if (event.type == SWT.MouseDown){
							for (ThumbInfo kid : thumbs){
								kid.isSelected = false;
							}
							t.isSelected = true;
						}else if (event.type == SWT.MouseExit){
							t.isMouseIn = false;
							thumbNameComp.redraw();
						}else if (event.type == SWT.Paint){
							if (t.isMouseIn){
								event.gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
								event.gc.setLineWidth(4);
								event.gc.drawRectangle(0, 0, thumbNameComp.getClientArea().width, thumbNameComp.getClientArea().height);
							}else{
								event.gc.setForeground(toolkit.getColors().getBorderColor());
								event.gc.setLineWidth(2);
								event.gc.drawRectangle(0, 0, thumbNameComp.getClientArea().width, thumbNameComp.getClientArea().height);
							}
						}	
					}
				};
				I2SwtUtils.cascadeAdd(thumbNameComp,l, SWT.MouseEnter,SWT.MouseExit, SWT.Paint, SWT.MouseDown);
			}
		}


		private class ThumbInfo{
			ISmartAttachment file;
			Thumbnail thumb;
			String tooltip;
			
			boolean isMouseIn;
			boolean isSelected;
			
			public ThumbInfo(ISmartAttachment file){
				this.file = file;
				tooltip = file.getFilename();
				if (file instanceof IntelAttachment){
					tooltip += "\nAdded: " + DateFormat.getDateInstance().format(((IntelAttachment)file).getDateCreated());
				}
			}
			
			public void createThumb(){
				if (thumb == null){
					thumb = new Thumbnail(file, false);
				}
			}
		}

	}
}
