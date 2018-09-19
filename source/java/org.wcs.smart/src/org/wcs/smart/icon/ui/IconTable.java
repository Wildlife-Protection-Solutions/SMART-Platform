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
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.ui.Thumbnail;
import org.wcs.smart.util.SmartUtils;


/**
 * Table for displaying a collection of icon files
 * 
 * @author Emily
 *
 */
public class IconTable extends Composite implements Listener {

	private ThumbnailComposite thumb;
	
	private ScrolledForm infoSection;
	
	private IMenuCreator thumbMenu;
	
	private Color selectionColor = null;
	private Color mouseOverColor = null;
	private Color backgroundColor = null;
	private Color selectionBorderColor = null;

	private int currentIndex = 0;
	private int pageSize = 5;
	private int thumbnailSize = 150;
	
	private Composite spacer = null;
	
	private int numColumns = 1;
	private List<IconFile> attachments;
	
	private Job loadImagesJob = new Job("loading images") { //$NON-NLS-1$
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			while(true){
				//create spaces for remaining images
				if (attachments == null) return Status.CANCEL_STATUS;
				monitor.setTaskName(currentIndex + "/" + attachments.size()); //$NON-NLS-1$
				if (currentIndex >= attachments.size()) {
					break;
				}
				List<IconFile> items = attachments.subList(currentIndex, Math.min(currentIndex + pageSize, attachments.size()));
				currentIndex += pageSize;
				
				final boolean[] needmore = new boolean[] {false};
				
				Display.getDefault().syncExec(()->{
					if (spacer != null) {
						spacer.dispose();
						spacer = null;
					}
					thumb.addFiles(items);
					thumb.createThumbs();
					getParent().layout(true, true);
					layoutAttachments();
					needmore[0] = needsToLoad();
				});
				
				if (!needmore[0]) break;	
			}
			
			Display.getDefault().syncExec(()->{
				layoutAttachments();
				
			});	
			return Status.OK_STATUS;
		}		
	};
	
	public IconTable(Composite parent, IMenuCreator thumbMenu){
		this(parent, SWT.NONE, thumbMenu);
	}
	
	public IconTable(Composite parent){
		this(parent, SWT.NONE);
	}
	
	public IconTable(Composite parent, int style){
		this(parent, style, null);
	}
	
	@Override
	public void setBackground(Color c) {
		super.setBackground(c);
		infoSection.setBackground(c);
		if (thumb != null) thumb.setBackground(c);
	}
	
	public IconTable(Composite parent, int style, IMenuCreator thumbMenu){
		super(parent, style);
		this.thumbMenu = thumbMenu;
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		selectionColor = SmartUtils.getListSelectedColor(getDisplay());
		mouseOverColor = SmartUtils.getListSelectedColor(getDisplay());
		selectionBorderColor = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
		addListener(SWT.Dispose, e->{
			selectionColor.dispose();
			mouseOverColor.dispose();
			selectionBorderColor.dispose();
		});
		
		infoSection = new ScrolledForm(this, SWT.V_SCROLL | SWT.H_SCROLL | SWT.VERTICAL);
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
				if (thumb != null && thumb.thumbs != null){
					for (IconTable.ThumbnailComposite.ThumbInfo t : thumb.thumbs){
						if (t.thumb != null) t.thumb.disposeImage();
					}
				}
			}
		});
		infoSection.getVerticalBar().addListener(SWT.Selection, e->{
			if (thumb == null) return;
			scheduleLoadJob();
		});
		
		createMenu();
	}

	private void createMenu() {
		
	}
	public void setThumbnailSize(int size) {
		this.thumbnailSize = size;
		setAttachments(attachments);
	}
	
	/**
	 * Set the results to display 
	 * 
	 * @param resultSet
	 */
	public void setAttachments(List<IconFile> attachments) {
		this.attachments = attachments;
		currentIndex = 0;
		Display.getDefault().syncExec(()->{
			if (thumb != null) {
				thumb.dispose();
				thumb = null;
			}
			if (infoSection == null || infoSection.isDisposed()) return;
			thumb = new ThumbnailComposite(infoSection.getBody());
			thumb.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			thumb.setBackground(getBackground());
			infoSection.setOrigin(0, 0);
			setSize();
			layoutAttachments();
		});
	}
	
	@Override
	public void handleEvent(Event event) {
		if (event == null || event.type == SWT.Resize){
			if (infoSection == null || infoSection.isDisposed()) return;
			if (thumb != null){
				setSize();
				layoutAttachments();
			}
		}
	}
	
	/*
	 * schedule load images job if job is not already running
	 * 
	 */
	private void scheduleLoadJob() {
		if (needsToLoad()) {
			//TODO: the might be finished before we are done here.
			if (loadImagesJob.getState() != Job.RUNNING) loadImagesJob.schedule();
		}
	}
	
	/*
	 * determine if we have more images to load
	 */
	private boolean needsToLoad() {
		if (attachments == null) return false;
		if (currentIndex > attachments.size()) return false;
		int spaces = thumb.getChildren().length % numColumns;
		boolean more = infoSection.getSize().y + infoSection.getVerticalBar().getSelection() >= thumb.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		boolean more2 = infoSection.getSize().y + infoSection.getVerticalBar().getSelection() >= thumb.computeSize(SWT.DEFAULT, SWT.DEFAULT).y - thumbnailSize && spaces != 0;
		return more || more2;
	}
	
	private void setSize() {
		int width = infoSection.getSize().x - infoSection.getVerticalBar().getSize().x;	
		int cols = (int)Math.floor(width / (thumbnailSize + 5 ));  //5 margin between images
		if (cols < 1) cols = 1;
		numColumns = cols;
		thumb.updateLayout(cols);
		thumb.layout(true);
		
		if (attachments != null) {
			int rows = (int)Math.ceil(attachments.size() / (double)cols);
			int y = (thumbnailSize + 5)* (rows);
			int heightRequired = y;
			
			if (spacer != null) {
				spacer.dispose();
				spacer = null;
			}
			
			if (heightRequired > thumb.getSize().y) {
				spacer = new Composite(infoSection.getBody(), SWT.BORDER);
				spacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
				spacer.setVisible(true);
				((GridData)spacer.getLayoutData()).heightHint = heightRequired - thumb.getSize().y;
			}
			infoSection.reflow(true);
		}
	}
	
	/*
	 * layout attachments
	 */
	private void layoutAttachments() {
		scheduleLoadJob();
	}
	
	/*
	 * get selected attachments
	 */
	public List<ISmartAttachment> getSelection(){
		List<ISmartAttachment> sel = new ArrayList<ISmartAttachment>();
		for (ThumbnailComposite.ThumbInfo t : thumb.thumbs){
			if (t.isSelected) sel.add(t.file);
		}
		return sel;
	}
	
	/**
	 * adds a listener to given control and all children
	 * @param parent
	 * @param listener
	 * @param eventTypes
	 */
	private static void cascadeAdd(Control parent, Listener listener, int... eventTypes){
		List<Control> controls = new ArrayList<Control>();
		controls.add(parent);
		while(!controls.isEmpty()){
			Control c = controls.remove(0);
			
			for (int e: eventTypes){
				c.addListener(e, listener);
			}
			
			if (c instanceof Composite){
				for (Control kid : ((Composite)c).getChildren()){
					controls.add(kid);
				}
			}
		}
	}
	
	private void fireSelectionEvent() {
		Event swtEvent = new Event();
		swtEvent.widget = this;
		this.notifyListeners(SWT.Selection, swtEvent);
	}
	
	/*
	 * A composite for thumbnails
	 */
	private class ThumbnailComposite extends Composite  {
		private List<ThumbInfo> thumbs;
		private ThumbInfo lastHover = null; 
//		private AttachmentTooltipShell shell;
		
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
		
		public void addFiles(List<IconFile> fileNames) {
			if (thumbs == null){
				thumbs = new ArrayList<IconTable.ThumbnailComposite.ThumbInfo>();
			}
			for (IconFile a : fileNames){
				thumbs.add(new ThumbInfo(a));
			}
			int index = 0;
			for(ThumbInfo t : thumbs){
				t.index = index++;
			}	
		}
		
		private void createThumbs(){
			if (thumbs == null) return;
			if (isDisposed()) return;
			for (ThumbInfo t : thumbs){
				if (t.thumbGui != null) continue;
				Composite parent = new Composite(this, SWT.NONE);
				parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
				if (t.thumb == null) t.createThumb();
				Composite thumbNameComp = t.thumb.createThumbnail(parent, SWT.NONE);
				thumbNameComp.setToolTipText(t.file.getIcon().getName());
				if (thumbMenu != null) thumbNameComp.setMenu(thumbMenu.createMenu(thumbNameComp));
				thumbNameComp.setData(t);
				thumbNameComp.setBackground(thumb.getBackground());
				thumbNameComp.addDisposeListener(new DisposeListener() {					
					@Override
					public void widgetDisposed(DisposeEvent e) {
						thumbNameComp.setMenu(null);
					}
				});
				t.thumbGui = thumbNameComp;

				cascadeAdd(thumbNameComp,t, SWT.MouseEnter,SWT.MouseExit, SWT.MouseDown, SWT.MouseUp, SWT.MouseMove, SWT.Paint, SWT.MouseHover);
			}
		}
		

		private void clearSelection(){
			if (thumbs == null) return;
			for (ThumbInfo c : thumbs){
				c.isSelected = false;
				c.colorAll();
			}
			fireSelectionEvent();
		}
		
		private class ThumbInfo implements Listener{
			private static final String LAST_SELECTION_INDEX_KEY = "last_selection_index"; //$NON-NLS-1$
			IconFile file;
			Thumbnail thumb;
			Composite thumbGui;
			
			boolean isSelected;
			boolean mouseOver;
			int index;
			
			public ThumbInfo(IconFile file){
				this.file = file;
			}
			
			public void createThumb(){
				if (thumb == null){
					thumb = new Thumbnail(file, thumbnailSize, false);
					thumb.setImageName(file.getIcon().getName());
					
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
					if (c == null) continue;
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
						event.gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BORDER));
						event.gc.setLineWidth(2);
						event.gc.drawRectangle(0, 0, thumbGui.getClientArea().width, thumbGui.getClientArea().height);
					}
//				}else if (event.type == SWT.MouseHover) {
//					if (this != lastHover || shell.isDisposed()) {
//						getShell().getDisplay().asyncExec(()->{
//							//EG: running this in async exec makes it work nicely on mac
//							if (resultSet instanceof IDesktopPagedImageResultSet) {
//								shell = new AttachmentTooltipShell(getShell(), (IDesktopPagedImageResultSet)resultSet, file);
//								shell.open(thumbGui.toDisplay(event.x+10, event.y));
//								lastHover = this;
//							}
//						});
//					}
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
				fireSelectionEvent();
			}
		}
	}

	
	public interface IMenuCreator{
		public Menu createMenu(Composite composite);
	}
}
