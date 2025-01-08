/*
 * Copyright (C) 2018 Wildlife Conservation Society
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
package org.wcs.smart.imageprocessor;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.SmartContext;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointAttachment;

/**
 * Processor for finding waypoint attachments and resizing them using 
 * the filters and target size provided.
 * 
 * @author Emily
 *
 */
public class ImageResizeProcessor extends Job{

	private List<IWaypointSource> sources;
	private LocalDate startDate;
	private LocalDate endDate;
	private double maxSize;
	private int newWidth;
	private int newHeight;
	
	private IProcessingMonitor statusReporter;
	
	public ImageResizeProcessor(List<IWaypointSource> types, LocalDate startDate, LocalDate endDate, double maxSize, int newWidth, int newHeight) {
		super(Messages.ImageResizeProcessor_JobName);
		this.sources = types;
		this.startDate = startDate;
		this.endDate = endDate;
		
		this.maxSize = maxSize;
		this.newWidth = newWidth;
		this.newHeight = newHeight;
		
		this.statusReporter = new IProcessingMonitor() {};
	}
	
	/**
	 * This status monitor is updated as the files are processed.  Can
	 * used for providing feedback to the user.
	 * 
	 * @param status
	 */
	public void setStatusMonitor(IProcessingMonitor status) {
		this.statusReporter = status;
	}
	
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		statusReporter.begin();
		
		try {
			List<ProcessingItem> items = new ArrayList<>();
			List<String> srcKeys = sources.stream().map(src->src.getKey()).collect(Collectors.toList());
			try(Session session = HibernateManager.openSession()){
				
				String aquerystr = "FROM WaypointAttachment WHERE waypoint in (FROM Waypoint WHERE conservationArea = :ca and sourceId in (:srcs)"; //$NON-NLS-1$
				String oquerystr = "FROM ObservationAttachment WHERE observation.observationGroup.waypoint in (FROM Waypoint WHERE conservationArea = :ca and sourceId in (:srcs)"; //$NON-NLS-1$
				
				if (startDate != null) {
					aquerystr += " and dateTime >= :start and dateTime <= :end"; //$NON-NLS-1$
					oquerystr += " and dateTime >= :start and dateTime <= :end"; //$NON-NLS-1$
				}
				aquerystr += ")"; //$NON-NLS-1$
				oquerystr += ")"; //$NON-NLS-1$
				
				
				Query<WaypointAttachment> aquery = session.createQuery(aquerystr, WaypointAttachment.class)
						.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
						.setParameter("srcs", srcKeys); //$NON-NLS-1$
				Query<ObservationAttachment> oquery = session.createQuery(oquerystr, ObservationAttachment.class)
						.setParameter("ca",  SmartDB.getCurrentConservationArea()) //$NON-NLS-1$
						.setParameter("srcs", srcKeys); //$NON-NLS-1$
				if (startDate != null) {
					aquery.setParameter("start", startDate.atStartOfDay()); //$NON-NLS-1$
					aquery.setParameter("end",  endDate.atTime(LocalTime.MAX)); //$NON-NLS-1$
					oquery.setParameter("start", startDate.atStartOfDay()); //$NON-NLS-1$
					oquery.setParameter("end", endDate.atTime(LocalTime.MAX)); //$NON-NLS-1$
				}
				
				try( ScrollableResults<WaypointAttachment> results = aquery.scroll()){
				
					while(results.next()) {
						WaypointAttachment wp = results.get();
						try {
							wp.computeFileLocation(session);
							if (processFile(wp)) items.add(new ProcessingItem(wp));
						}catch (Exception ex) {
							ObservationPlugIn.log(ex.getMessage(), ex);
						}
					}
				}
				
				try(ScrollableResults<ObservationAttachment> results = oquery.scroll()){
					
					while(results.next()) {
						ObservationAttachment wp = results.get();
						try {
							wp.computeFileLocation(session);
							if (processFile(wp)) items.add(new ProcessingItem(wp));
						}catch (Exception ex) {
							ObservationPlugIn.log(ex.getMessage(), ex);
						}
					}
				}
			}
			
			statusReporter.setItems(items);
			if (items.isEmpty()) return Status.OK_STATUS;
			
			if (statusReporter.isCancelled()) return Status.CANCEL_STATUS;
			
			for (ProcessingItem a : items) {
				a.setStatus(ProcessingItem.Status.PROCESSING);
				statusReporter.update(a);
				try {
					resizeImage(a, newWidth, newHeight);
				}catch (Exception ex) {
					ObservationPlugIn.log(ex.getMessage(), ex);
					a.setMessage(ex.getMessage());
					a.setStatus(ProcessingItem.Status.ERROR);
				}
				statusReporter.update(a);
				
				if (statusReporter.isCancelled()) return Status.CANCEL_STATUS;
				
			}
		}finally {
			statusReporter.done();
		}

		return Status.OK_STATUS;
	}

	private boolean processFile(ISmartAttachment attachment) throws Exception{
		if (attachment.getAttachmentFile().toAbsolutePath().toFile().length() <  maxSize * 1048576l) return false;
		if (!isImage(attachment)) return false;
		return true;
	}
	
	private boolean isImage(ISmartAttachment file) throws Exception {
		Path p = EncryptUtils.decryptAttachment(file);
		try (InputStream is = Files.newInputStream(p)){
			try (ImageInputStream iis = ImageIO.createImageInputStream(is)){
				Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
				if (!readers.hasNext()) return false;	//no image reader
				readers.forEachRemaining(r->r.dispose());
				return true;
			}catch (Exception ex){
				return false;
			}
		}finally {
			Files.delete(p);
		}
	}
	
	public void resizeImage(ProcessingItem item, int targetWidth, int targetHeight) throws Exception{
		if(targetWidth <=0 || targetHeight <= 0) {
			item.setStatus(ProcessingItem.Status.WARNING);
			item.setMessage(Messages.ImageResizeProcessor_invalidTargetSize);
			return ;
		}
		
		Path decryptedAttachment = EncryptUtils.decryptAttachment(item.getAttachment());
		
		BufferedImage toResize = null;
		IIOMetadata metadata = null;
		ImageReader reader = null;
		
		try{
			//read image
			//image-ext does not properly close file so use native java input stream
			try(ImageInputStream iis =  new FileImageInputStream(decryptedAttachment.toFile())){
				Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
				if (!readers.hasNext()) {
					item.setStatus(ProcessingItem.Status.ERROR);
					item.setMessage(Messages.ImageResizeProcessor_NoReaderFound);
					return ;	//no image reader
				}
				reader = readers.next();
				reader.setInput(iis, true, true);
				try {
					toResize = reader.read(0, reader.getDefaultReadParam());
					metadata = reader.getImageMetadata(0);
				}finally {
					reader.dispose();
				}
			}	//close input stream
			

			//configure orientation of targets sizes
			if ((targetWidth < targetHeight && toResize.getWidth() > toResize.getHeight())
					|| (targetWidth > targetHeight && toResize.getWidth() < toResize.getHeight())) {
				int tmp = targetWidth;
				targetWidth = targetHeight;
				targetHeight = tmp;
			}

			if (toResize.getWidth() <= targetWidth
					|| toResize.getHeight() <= targetHeight) {
				item.setStatus(ProcessingItem.Status.WARNING);
				item.setMessage(Messages.ImageResizeProcessor_NothingToDo);
				return; // it's already smaller than target
			}
			
			//perform resize
			double ratio = Math.min(
					targetWidth / (double) toResize.getWidth(),
					targetHeight / (double) toResize.getHeight());
			int newWidth = (int) Math.round(toResize.getWidth() * ratio);
			int newHeight = (int) Math.round(toResize.getHeight() * ratio);

			BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, toResize.getType());
			Graphics2D g = resizedImage.createGraphics();
			try {
				g.drawImage(toResize, 0, 0, newWidth, newHeight, null);
			} finally {
				g.dispose();
			}

			//write image
			
			Path core = Paths.get(SmartContext.INSTANCE.getFilestoreLocation()).resolve(EncryptUtils.TEMP_DIR);
			Path outputFile = EncryptUtils.uniqueFile(core.resolve(item.getAttachment().getFilename()));
			
			//make sure we use the java imao io outputstream here.  Using the imageio-ext-streams library
			//results in a bug with some metadata
			try(ImageOutputStream ios = new FileImageOutputStream(outputFile.toFile())){
				ImageWriter writer = ImageIO.getImageWriter(reader);
				try {
					ImageWriteParam imageWriteParam = writer.getDefaultWriteParam();
					imageWriteParam.setCompressionMode(ImageWriteParam.MODE_COPY_FROM_METADATA);
					if (imageWriteParam instanceof JPEGImageWriteParam) {
						//to fix #2442 - javax.imageio.IIOException: Missing Huffman code table entry
						//this will always create huffman tables
						((JPEGImageWriteParam)imageWriteParam).setOptimizeHuffmanTables(true);
					}
					writer.setOutput(ios);
					writer.write(null, new IIOImage(resizedImage, null, metadata),imageWriteParam);
				} finally {
					writer.dispose();
				}
			}
			
			//encrypt and move to original file
			Path encryptedFile = EncryptUtils.uniqueFile(core.resolve(item.getAttachment().getFilename()));
			EncryptUtils.encryptFile(outputFile, encryptedFile, item.getAttachment());
			if (Files.exists(encryptedFile)) {
				Files.move(encryptedFile, item.getAttachment().getAttachmentFile(), StandardCopyOption.REPLACE_EXISTING);
			}
			
			for (Path p : new Path[] {outputFile, encryptedFile}) {
				try {
					Files.delete(p);
				}catch (Exception ex) {
					
				}
			}
			
			item.setStatus(ProcessingItem.Status.OK);
			return;
			
		}finally {
			try {
				Files.delete(decryptedAttachment);
			}catch (Exception ex) {
				ObservationPlugIn.log(ex.getMessage(), ex);
			}
		}
		
	}
	
}
