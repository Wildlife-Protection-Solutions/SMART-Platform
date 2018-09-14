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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
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

	private final CountDownLatch latch = new CountDownLatch(1);

	private Image image;
	private ISmartAttachment attachment;
	
	private int thumbnailSize = 100;
	private boolean autoDispose;
	private Composite thumbnailComposite;
	
	private String imageName;
	
	/*
	 * double click listener to
	 * open attachment
	 */
	private MouseListener doubleClickListener = new MouseAdapter(){
		@Override
		public void mouseDoubleClick(MouseEvent e) {
			if (attachment != null){
				AttachmentUtil.openAttachment(attachment);
			}
		}
	};
	
	/**
	 * Creates a new thumbnail of the given size.  Thumbnails are always
	 * square.
	 * @param attachment
	 * @param thumbnailSize
	 */
	public Thumbnail(ISmartAttachment attachment, int thumbnailSize){
		this(attachment, thumbnailSize, true);
	}
	
	/**
	 * Creates a new thumbnail of the given size (thumbnails are always square). If autodipose
	 * is selected the image is automatically disposed, otherwise
	 * user must dispose of image by calling disposeImage
	 * 
	 * @param attachment
	 * @param thumbnailSize
	 * @param autoDispose
	 */
	public Thumbnail(ISmartAttachment attachment, int thumbnailSize, boolean autoDispose){
		this.attachment = attachment;
		this.thumbnailSize = thumbnailSize;
		try{
			loadImageData();
		}catch (Exception ex){
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		this.autoDispose = autoDispose;
		
	}
	/**
	 * Creates a new thumbnail of the default size. If autoDipose
	 * is selected the image is automatically disposed, otherwise
	 * user must dispose of image by calling disposeImage
	 * 
	 * @param attachment
	 * @param autoDispose
	 */
	public Thumbnail(ISmartAttachment attachment, boolean autoDispose){
		this(attachment, 100, autoDispose);
	}
	
	/**
	 * Creates a new thumbnail using the default size.
	 * @param attachment
	 */
	public Thumbnail(ISmartAttachment attachment){
		this(attachment, 100);
	}

	/**
	 * Manually dispose of thumbnail image
	 */
	public void disposeImage(){
		if (image != null){
			image.dispose();
			image = null;
		}
//		loadImageDataJob.cancel();
		LoadThumbnailImageJob.getInstance().removeThumbnail(this);
	}
	
	/**
	 * Waits until loading the image is complete and then returns
	 * the loaded image
	 * @return
	 * @throws InterruptedException 
	 */
	public Image getImage() {
		if(image == null) {
			try {
				latch.await();
			} catch (InterruptedException e) {
				SmartPlugIn.log(e.getMessage(), e);
			}
		}
		return this.image;
	}
	/*
	 * generate the thumbnail in memory 
	 */
	private void loadImageData() throws Exception{
		LoadThumbnailImageJob.getInstance().addThumbnail(this);
	}
	
	/**
	 * Creates a thumbnail with a border.
	 * @param parent
	 * @return
	 */
	public Composite createThumbnail(Composite parent){
		return createThumbnail(parent, SWT.BORDER);
	}
	
	/**
	 * Creates the thumbnail widget with given style
	 * @param parent
	 */
	public Composite createThumbnail(Composite parent, int style){
		return createThumbnail(parent, 0, style);
	}
	
	ISmartAttachment getAttachment() {
		return this.attachment;
	}
	
	int getThumbnailSize() {
		return this.thumbnailSize;
	}
	
	public void setImageName(String name) {
		this.imageName = name;
	}
	
	void setImage(Image image) {
		this.image = image;
		latch.countDown();
		if (thumbnailComposite != null) {
			Display.getDefault().asyncExec(() -> {
				if (!thumbnailComposite.isDisposed())
					thumbnailComposite.redraw();
			});
		}
	}
	boolean isDisposed() {
		if (thumbnailComposite != null) return thumbnailComposite.isDisposed();
		return false;
	}
	/**
	 * Creates the thumbnail widget with given style using the given offset.  The 
	 * offset is the number of pixel to offset the image from the edge 
	 * of the composite 
	 * 
	 * @param parent
	 */
	public Composite createThumbnail(Composite parent, int offset, int style ){
		thumbnailComposite = new Composite(parent, style);
		thumbnailComposite.setLocation(0,0);
		thumbnailComposite.setSize(thumbnailSize, thumbnailSize);
		thumbnailComposite.addMouseListener(doubleClickListener);
		
		String fileName = null;
		if (attachment == null){
			fileName = ""; //$NON-NLS-1$
		}else if (attachment.getCopyFromLocation() != null){
			fileName = attachment.getCopyFromLocation().getName();
		}else if (imageName != null) {
			fileName = imageName;
		}else{
			fileName = attachment.getAttachmentFile().getName();
		}
		
		final String filename = fileName;
		
		Listener listener = new Listener() {
			public void handleEvent(Event e) {
				switch (e.type) {
					case SWT.Dispose:
						disposeImage();
						break;
					case SWT.Paint: {
						if (image != null){
							e.gc.drawImage(image, offset, offset);
						}else{
							List<String> toDraw = new ArrayList<String>();
							
							int width = thumbnailComposite.getClientArea().width - 8;
							
							int size = 0;
							StringBuilder sb = new StringBuilder();
							for (int i = 0; i <filename.length(); i ++){
								if (size + e.gc.getCharWidth(filename.charAt(i)) < width){
									sb.append(filename.charAt(i));
									size = e.gc.stringExtent(sb.toString()).x;
								}else{
									toDraw.add(sb.toString());
									sb = new StringBuilder();
									sb.append(filename.charAt(i));;
									size = e.gc.stringExtent(sb.toString()).x;
								}
							}
							if (sb.length() > 0) toDraw.add(sb.toString());
							int y = 4;
							
							for (String x : toDraw){
								e.gc.drawText(x, 4, y);
								y += e.gc.stringExtent(x).y;
							}
						}
						break;
					}
				}
			}
		};
		if (autoDispose){
			thumbnailComposite.addListener(SWT.Dispose, listener);
		}
		thumbnailComposite.addListener(SWT.Paint, listener);
		
		return thumbnailComposite;
	}
}
