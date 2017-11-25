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
package org.wcs.smart.asset.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.ui.Thumbnail;
import org.wcs.smart.util.SmartUtils;


/**
 * Table for displaying list of attachments.
 * '
 * @author Emily
 *
 */
public class AttachmentTable extends Composite {

	private List<ISmartAttachment> attachments;
	private FormToolkit toolkit;
	
	private List<ThumbInfo> thumbs;
	
	private Menu thumbMenu;
	
	private Color selectionColor = null;
	private Color mouseOverColor = null;
	private Color backgroundColor = null;
	private Color selectionBorderColor = null;
	
	private int thumbSize;
	
	public AttachmentTable(Composite parent, FormToolkit toolkit, IMenuCreator thumbsMenu, List<ISmartAttachment> files, int thumbSize){
		this(parent, toolkit, thumbsMenu, SWT.NONE, files, thumbSize);
	}
	
	public AttachmentTable(Composite parent, FormToolkit toolkit, IMenuCreator thumbsMenu, int style, List<ISmartAttachment> files, int thumbSize){
		super(parent, style);
		this.thumbSize = thumbSize;
		this.toolkit = toolkit;
		toolkit.adapt(this);
		
		setLayout(new RowLayout());
		((RowLayout)getLayout()).wrap = true;
		((RowLayout)getLayout()).marginWidth = 0;
		((RowLayout)getLayout()).marginHeight = 0;
		
		Color color = getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION);
		selectionColor = new Color(getDisplay(), SmartUtils.blend(new RGB(255, 255, 255), color.getRGB(), 75));
		mouseOverColor = new Color(getDisplay(), SmartUtils.blend(new RGB(255, 255, 255), color.getRGB(), 90));
		selectionBorderColor = new Color(getDisplay(), SmartUtils.blend(new RGB(0, 0, 0), color.getRGB(), 40));
		addListener(SWT.Dispose, e->{
			selectionColor.dispose();
			mouseOverColor.dispose();
			selectionBorderColor.dispose();
		});

		//ensure we manually dispose of images
		//as we reuse them if we can
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				if (thumbs == null) return;
				for (ThumbInfo t : thumbs){
					if (t.thumb != null) t.thumb.disposeImage();
				}
			}
		});
		if (thumbsMenu != null) thumbMenu = thumbsMenu.createMenu(this);
		
		this.attachments = files;
		createThumbnails();
	}

	
	private void createThumbnails() {
		addListener(SWT.MouseUp, e->clearSelection());
		
		thumbs = new ArrayList<ThumbInfo>();
		for (ISmartAttachment a : attachments){
			thumbs.add(new ThumbInfo(a));
		}
		int index = 0;
		for(ThumbInfo t : thumbs){
			t.index = index++;
		}
		
		for (ThumbInfo t : thumbs){

			Composite thumbNameComp = t.thumb.createThumbnail(AttachmentTable.this, SWT.NONE);
			thumbNameComp.setSize(thumbSize, thumbSize);
			thumbNameComp.setLayoutData(new RowData(thumbSize,thumbSize));
			
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

			cascadeAdd(thumbNameComp,t, SWT.MouseEnter,SWT.MouseExit, SWT.MouseDown, SWT.MouseUp, SWT.MouseMove, SWT.Paint);
		}
	}
	
	
	public List<ISmartAttachment> getSelection(){
		List<ISmartAttachment> sel = new ArrayList<ISmartAttachment>();
		for (ThumbInfo t : thumbs){
			if (t.isSelected) sel.add((ISmartAttachment) t.file);
		}
		return sel;
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
			thumb = new Thumbnail(file, thumbSize, true);
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
	

	public static void cascadeAdd(Control parent, Listener listener, int... eventTypes){
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
	
	/**
	 * Interface for creating a menu to attach to each thumbnail
	 * @author Emily
	 *
	 */
	public interface IMenuCreator {
		/**
		 * Creates a menu
		 * @param parent
		 * @return
		 */
		public Menu createMenu(AttachmentTable parent);
		
	}
}
