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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Single job for loading thumbnail images.
 * 
 * @author Emily
 *
 */
public class LoadThumbnailImageJob extends Job {

	private static LoadThumbnailImageJob INSTANCE = new LoadThumbnailImageJob();
	
	private List<Thumbnail> toLoad = Collections.synchronizedList( new ArrayList<>() );
	
	public static final LoadThumbnailImageJob getInstance() {
		return INSTANCE;
	}
	
	private LoadThumbnailImageJob() {
		super(Messages.LoadThumbnailImageJob_jobname);
	}
	
	public void addThumbnail(Thumbnail thumb) {
		boolean schedule = toLoad.isEmpty();
		toLoad.add(thumb);
		if (schedule) schedule();
	}
	
	public void removeThumbnail(Thumbnail thumb) {
		toLoad.remove(thumb);
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask(Messages.LoadThumbnailImageJob_taskname, IProgressMonitor.UNKNOWN);
		while(!toLoad.isEmpty()) {
			monitor.subTask(String.valueOf(toLoad.size()));
			Thumbnail thumb = toLoad.remove(0);
			if (!thumb.isDisposed()) loadImage(thumb);
		}
		return Status.OK_STATUS;
	}

	private void loadImage(Thumbnail thumb) {
		Image image = null;
		try {
			ISmartAttachment attachment = thumb.getAttachment();
			int thumbnailSize = thumb.getThumbnailSize();
			
			if (attachment == null)
				return;
			try {
				File file = null;
				boolean deleteMe = false;
				try {
					if (attachment.getCopyFromLocation() != null) {
						file = attachment.getCopyFromLocation();
					} else {
						try {
							if (attachment.isEncrypted()) {
								deleteMe = true;
								Path p = EncryptUtils.decryptAttachment(attachment);
								if (p != null) file = p.toFile();
							}else {
								file = attachment.getAttachmentFile();
							}
						}catch (Exception ex) {
							file = null;
						}
						
					}
					
					if (file == null || file.length() > 200 * Math.pow(10, 6)) {
						// skip images > 200MB
						return;
					}
					if (file.getAbsolutePath().toString().endsWith(".svg")) { //$NON-NLS-1$
						try {
							image = SmartUtils.readSvg(Display.getDefault(), file.toPath(), thumbnailSize);
							return ;
						}catch (Throwable t) {
							//eat this exception
							t.printStackTrace();
						}
					}
					
					Image rawImage = null;
					try{
						rawImage = new Image(Display.getDefault(), file.getAbsolutePath());
					}catch (Exception ex) {
						//eat this exception; probably not an image
					}
					if (rawImage == null) return;
					
					// scale image
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
					
					Transform imageTransform = SmartUtils.getExifImageTransform(file, thumbnailSize, thumbnailSize);
					if (imageTransform != null) {
						Image image3 = new Image(Display.getDefault(), thumbnailSize, thumbnailSize);
						GC gc3 = new GC(image3);
						gc3.setTransform(imageTransform);
						gc3.drawImage(rawImage, 0, 0, rawImage.getBounds().width, rawImage.getBounds().height, x, y, width, height);
						rawImage.dispose();
						image = image3;
					}else {
						// resize image
						Image image2 = new Image(Display.getDefault(), thumbnailSize, thumbnailSize);
						GC gc = new GC(image2);
						gc.drawImage(rawImage, 0, 0, bounds.width, bounds.height, x, y, width, height);
						rawImage.dispose();
						image = image2;
					}
					
				}finally {
					if (deleteMe && file != null) {
						try {
							Files.delete(file.toPath());
						}catch (Exception ex) {}
					}
				}
			} catch (SWTException ex) {
				// eatme
				ex.printStackTrace();
			}
		}finally {
			//necessary to release locks even if image is null
			thumb.setImage(image);
		}
	}
}
