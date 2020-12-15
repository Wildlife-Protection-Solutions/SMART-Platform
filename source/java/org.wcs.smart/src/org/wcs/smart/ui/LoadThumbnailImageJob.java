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

import java.io.InputStream;
import java.net.URL;
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
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SharedUtils;
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
				Path file = null;
				boolean deleteMe = false;
				try {
					if (attachment.getCopyFromLocation() != null) {
						file = attachment.getCopyFromLocation();
					} else {
						try {
							if (attachment.isEncrypted()) {
								deleteMe = true;
								Path p = EncryptUtils.decryptAttachment(attachment);
								if (p != null) file = p;
							}else {
								file = attachment.getAttachmentFile();
							}
						}catch (Exception ex) {
							file = null;
						}
						
					}
					
					try {
						if (Files.probeContentType(file).startsWith("audio")){ //$NON-NLS-1$
							URL url = SmartPlugIn.getDefault().getBundle().getResource(SmartPlugIn.AUDIO_CLIP_IMG);
							
							Image imgAudio = null;
							try{
								try(InputStream reader = url.openStream()){
									imgAudio = SmartUtils.readSvg(Display.getDefault(), reader, thumbnailSize/4);
								}catch (Exception ex) {
									ex.printStackTrace();
									throw ex;
								}
								
								//display audio icon
								Image image2 = new Image(Display.getDefault(),thumbnailSize,thumbnailSize );
								
								GC gc = new GC(image2);
								try{
									int x = thumbnailSize / 2 - thumbnailSize / 8;
									int y = thumbnailSize / 2 - thumbnailSize / 8-10;
									gc.drawImage(imgAudio, 0, 0, imgAudio.getBounds().width, imgAudio.getBounds().height, x, y, imgAudio.getBounds().width, imgAudio.getBounds().height);
									
									String text = SharedUtils.getFilenameExtension(file.getFileName().toString());
									Point et = gc.textExtent(text);
									gc.drawText(text, thumbnailSize / 2 - et.x / 2, y + thumbnailSize / 4);
								}finally {
									gc.dispose();
								}
								image = image2;
							}finally {
								if (imgAudio != null) imgAudio.dispose();
							}
						}
					}catch (Exception ex) {
						
					}
					
					if (image == null) {
						if (file == null || file.toAbsolutePath().normalize().toFile().length() > 200 * Math.pow(10, 6)) {
							// skip images > 200MB
							return;
						}
						image = SmartUtils.getImage(file, thumbnailSize);
					}
				}finally {
					if (deleteMe && file != null) {
						try {
							Files.delete(file);
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
