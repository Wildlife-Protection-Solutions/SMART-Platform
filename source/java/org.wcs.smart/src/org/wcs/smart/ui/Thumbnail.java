/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.common.attachment.ISmartAttachment;

/**
 * Class to represent a thumbnail for a SMART attachment.  Attachments
 * that can be read as images are displayed as images.  For all other attachments
 * filenames are displayed.
 * 
 * <p>When the thumbnail if first created the image is loaded; however
 * it is not associated with any composite until createThumbnail(Composite)
 * is called.  At this point the thumbnail will be drawn in the composite.</p>
 * <pre>
 * Thumbnail thumb = new Thumbnail("C:\\temp\\image.png");
 * thumb.createThumbnail(parent);
 * </pre>
 * @author Emily
 *
 */
public class Thumbnail {

	private Image image;
	private ISmartAttachment attachment;
	
	private int thumbnailSize = 100;
	
	/*
	 * double click listener to
	 * open attachment
	 */
	private MouseListener doubleClickListener = new MouseAdapter(){
		@Override
		public void mouseDoubleClick(MouseEvent e) {
			AttachmentUtil.openAttachment(attachment);
		}
	};
	
	/**
	 * Creates a new thumbnail of the given size.  Thumbnails are always
	 * square.
	 * @param attachment
	 * @param thumbnailSize
	 */
	public Thumbnail(ISmartAttachment attachment, int thumbnailSize){
		this.attachment = attachment;
		this.thumbnailSize = thumbnailSize;
		try{
			loadImageData();
		}catch (Exception ex){
			SmartPlugIn.log(ex.getMessage(), ex);
		}
	}
	
	/**
	 * Creates a new thumbnail using the default size.
	 * @param attachment
	 */
	public Thumbnail(ISmartAttachment attachment){
		this(attachment, 100);
	}
	
	/*
	 * generate the thumbnail in memory 
	 */
	private void loadImageData() throws Exception{
		try {
			if (attachment.getAttachmentFile().length() > 200 * Math.pow(10, 6)) {
				// skip images > 200MB
				return;
			}

			Image rawImage = new Image(Display.getDefault(), attachment.getAttachmentFile()
					.getAbsolutePath());

			//scale image
			Rectangle bounds = rawImage.getBounds();
			int x = 0, y = 0, width = 0, height = 0;
			if (bounds.width > bounds.height) {
				width = thumbnailSize;
				height = bounds.height * thumbnailSize / bounds.width;
				y = (thumbnailSize - height) / 2;
			} else {
				height = thumbnailSize;
				width = bounds.width * thumbnailSize / bounds.height;
				x = (thumbnailSize - width) / 2;
			}
			Image image2 = new Image(Display.getDefault(), thumbnailSize, thumbnailSize);
			GC gc = new GC(image2);
			gc.drawImage(rawImage, 0, 0, bounds.width, bounds.height, x, y, width, height);
			rawImage.dispose();

			image = image2;
		} catch (SWTException ex) {
			//eatme
		}
	}
	
	/**
	 * Creates the thumbnail widget
	 * @param parent
	 */
	public void createThumbnail(Composite parent){
		final Composite c = new Composite(parent, SWT.BORDER);
		c.setLocation(0,0);
		c.setSize(thumbnailSize, thumbnailSize);
		
		c.addMouseListener(doubleClickListener);
		
		if (image == null){	
			Label lbl = new Label(c, SWT.WRAP);
			try{
				lbl.setText(attachment.getAttachmentFile().getName());
			}catch (Exception ex){
				SmartPlugIn.log(ex.getMessage(), ex);
			}
			lbl.setLocation(0, 0);
			lbl.setSize(thumbnailSize, thumbnailSize);
			lbl.addMouseListener(doubleClickListener);
			return;
		}
		
		
		Listener listener = new Listener() {
			public void handleEvent(Event e) {
				switch (e.type) {
				case SWT.Dispose:
					image.dispose();
					break;
				case SWT.Paint: {
					e.gc.drawImage(image, 0,0);
				}
				}
			}
		};
		c.addListener(SWT.Dispose, listener);
		c.addListener(SWT.Paint, listener);
	}
}
