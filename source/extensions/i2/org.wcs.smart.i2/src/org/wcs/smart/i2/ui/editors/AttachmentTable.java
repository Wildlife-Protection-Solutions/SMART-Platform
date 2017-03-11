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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
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
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelAttachment;
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
	
	private Color selectionColor = null;
	private Color mouseOverColor = null;
	private Color backgroundColor = null;
	private Color selectionBorderColor = null;
	
	public AttachmentTable(Composite parent, FormToolkit toolkit, IMenuCreator thumbsMenu, int style){
		super(parent, style);
		this.toolkit = toolkit;
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		Color color = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
		selectionColor = new Color(getDisplay(), blend(new RGB(255, 255, 255), color.getRGB(), 75));
		mouseOverColor = new Color(getDisplay(), blend(new RGB(255, 255, 255), color.getRGB(), 90));
		selectionBorderColor = new Color(getDisplay(), blend(new RGB(0, 0, 0), color.getRGB(), 40));
		addListener(SWT.Dispose, e->{
			selectionColor.dispose();
			mouseOverColor.dispose();
			selectionBorderColor.dispose();
		});
		
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
				int mainWidth = infoSection.getClientArea().width - infoSection.getVerticalBar().getSize().x ;
				int width = infoSection.getSize().x - infoSection.getVerticalBar().getSize().x;	
				
				int cols = (int)Math.floor(width / (100.0 + 5 ));  //5 margin between images
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
	
	
	
	private Job redraw = new Job("redraw thumbnails"){ //$NON-NLS-1$

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
	private class ThumbnailComposite extends Composite  {
		private List<ThumbInfo> thumbs;
		
		public ThumbnailComposite(Composite parent){
			super(parent, SWT.NONE);
			GridLayout gl = new GridLayout();
			gl.marginWidth = gl.marginHeight = 0;
			setLayout(gl);
			
			setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			addListener(SWT.MouseUp, e->clearSelection());
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
			int index = 0;
			for(ThumbInfo t : thumbs){
				t.index = index++;
			}
		}
		
		private void initThumbs(){
			if (thumbs == null) return;
			for (ThumbInfo info : thumbs){
				info.createThumb();
			}
		}
		
		private void createThumbs(){
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
				t.thumbGui = thumbNameComp;

				I2SwtUtils.cascadeAdd(thumbNameComp,t, SWT.MouseEnter,SWT.MouseExit, SWT.MouseDown, SWT.MouseUp, SWT.MouseMove, SWT.Paint);
			}
		}
		
		
		private void clearSelection(){
			if (thumbs == null) return;
			for (ThumbInfo c : thumbs){
				c.isSelected = false;
				c.colorAll();
			}
		}
		
		private class ThumbInfo implements Listener{
			private static final String LAST_SELECTION_INDEX_KEY = "last_selection_index"; //$NON-NLS-1$
			ISmartAttachment file;
			Thumbnail thumb;
			String tooltip;
			Composite thumbGui;
			
			boolean isSelected;
			boolean mouseOver;
			int index;
			
			public ThumbInfo(ISmartAttachment file){
				this.file = file;
				tooltip = file.getFilename();
				if (file instanceof IntelAttachment){
					tooltip += "\n" + Messages.AttachmentTable_AddedLabel + DateFormat.getDateInstance().format(((IntelAttachment)file).getDateCreated()); //$NON-NLS-1$
				}
			}
			
			public void createThumb(){
				if (thumb == null){
					thumb = new Thumbnail(file, false);
				}
			}
			
			private void colorAll(){
				if (backgroundColor == null){
					backgroundColor = thumbGui.getBackground();
				}
				Color color = null;
				if(isSelected){
					color = selectionColor;
				}else{
					if (mouseOver){
						color = mouseOverColor;
					}else{
						color = backgroundColor;
					}
				}
				List<Control> kids = new ArrayList<Control>();
				kids.add(thumbGui);
				while(!kids.isEmpty()){
					Control c = kids.remove(0);
					c.setBackground(color);
					if (c instanceof Composite){
						for (Control cc : ((Composite)c).getChildren()){
							kids.add(cc);
						}
					}
				}
			}
			
			@Override
			public void handleEvent(Event event) {
				if (event.type == SWT.MouseEnter){
					mouseOver = true;
					colorAll();
					thumbGui.redraw();
				}else if (event.type == SWT.MouseExit){
					mouseOver = false;
					colorAll();
					thumbGui.redraw();
				}else if (event.type == SWT.MouseMove){
				}else if (event.type == SWT.MouseDown){
					if (event.stateMask == 0 && !isSelected) changeSelection(event);
				}else if (event.type == SWT.MouseUp){
					changeSelection(event);
				}else if (event.type == SWT.Paint){
					if (mouseOver){					
						event.gc.setForeground(selectionBorderColor);
						event.gc.setLineWidth(4);
						event.gc.drawRectangle(0, 0, thumbGui.getClientArea().width, thumbGui.getClientArea().height);
					}else if (isSelected){
						event.gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
						event.gc.setLineWidth(4);
						event.gc.drawRectangle(0, 0, thumbGui.getClientArea().width, thumbGui.getClientArea().height);
					}else{
						event.gc.setForeground(toolkit.getColors().getBorderColor());
						event.gc.setLineWidth(2);
						event.gc.drawRectangle(0, 0, thumbGui.getClientArea().width, thumbGui.getClientArea().height);
					}
				}
			}
			
			private void changeSelection(Event event){
				Integer lastSelection = (Integer) getParent().getData(LAST_SELECTION_INDEX_KEY);
				if (lastSelection == null) lastSelection = 0;
				getParent().setData(LAST_SELECTION_INDEX_KEY, index);
				
				if ((event.stateMask & SWT.CTRL) != 0){
					if (event.button == 1) isSelected = !isSelected;
				}else if ((event.stateMask & SWT.SHIFT) != 0){
					boolean newSelection = !isSelected;
					//clearSelection();
					int from = lastSelection;
					int to = index;
					if (index < lastSelection){
						from = index;
						to = lastSelection;
					}
					for (int i = from; i <= to; i ++){
						if (i == index){
							thumbs.get(i).isSelected = true;
						}else{
							thumbs.get(i).isSelected = newSelection;		
						}
						thumbs.get(i).colorAll();
					}
					
				}else{
					if (event.button == 1){
						clearSelection();
					}else if (!isSelected){
						clearSelection();
					}
					isSelected = true;
				}
				colorAll();
			}
		}
	}
	
	private static int blend(int v1, int v2, int ratio) {
		int b = (ratio * v1 + (100 - ratio) * v2) / 100;
		return Math.min(255, b);
	}

	/**
	 * Blends c1 and c2 based in the provided ratio.
	 * 
	 * @param c1
	 *            first color
	 * @param c2
	 *            second color
	 * @param ratio
	 *            percentage of the first color in the blend (0-100)
	 * @return the RGB value of the blended color
	 * @since 3.1
	 */
	private static RGB blend(RGB c1, RGB c2, int ratio) {
		int r = blend(c1.red, c2.red, ratio);
		int g = blend(c1.green, c2.green, ratio);
		int b = blend(c1.blue, c2.blue, ratio);
		return new RGB(r, g, b);
	}
}
