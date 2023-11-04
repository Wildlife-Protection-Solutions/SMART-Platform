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
package org.wcs.smart.cybertracker;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.wcs.smart.common.attachment.ISmartAttachment;

/**
 * Collection of image processing tools
 * 
 * @author Emily
 *
 */
public enum ImageProcessor {
	
	INSTANCE;
	
	private void logMe(Exception ex) {
		//TODO: 
		ex.printStackTrace();
	}
	/**
	 * Attempts to resize the attachment to the given size.  Resizes the file in the copyFromLocation
	 * and updates this location to the new file if the file was resized correctly.
	 * 
	 * @param attachment
	 * @param width
	 * @param height
	 */
	public void processAttachment(ISmartAttachment attachment, int width, int height){
		try{
			//fix this - files won't be deleted correctly
			Path tempDir = Files.createTempDirectory("smart.import"); //$NON-NLS-1$
			tempDir.toFile().deleteOnExit();
			Path outImage = tempDir.resolve(attachment.getFilename());
			if (resizeImage(attachment.getCopyFromLocation(), outImage, width, height)){
				if (Files.exists(outImage)){
					attachment.setCopyFromLocation(outImage);
					outImage.toFile().deleteOnExit();
				}
			}
		}catch(Exception ex){
			logMe(ex);
		}
	}
	
	/**
	 * Reads an image from the file.  Returns null if the file cannot be read.
	 * @param inFile
	 * @return
	 */
	public BufferedImage readImage(Path inFile){
		BufferedImage toResize = null;
		//attempt to resize
		try(ImageInputStream iis = ImageIO.createImageInputStream(inFile.toAbsolutePath().toFile())){
				
			Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
			if (!readers.hasNext()) return null;	//no image reader
				
			ImageReader reader = readers.next();
			reader.setInput(iis, true, true);
			try {
				toResize = reader.read(0, reader.getDefaultReadParam());
				return toResize;
			}catch (Exception ex){
				logMe(ex);
				return null;
			} finally {
				reader.dispose();
			}
		}catch (Exception ex){
			logMe(ex);
			return null;
		}
		
	}
	
	/**
	 * Attempts to resize the provided file to the target width and target height.  
	 * Aspect ratios are preserved.  False is returned if the file cannot be 
	 * resized.  If target sizes are less than 0 image will not be resized
	 * 
	 * @param inFile
	 * @param outFile
	 * @param targetWidth
	 * @param targetHeight
	 * @return
	 */
	public boolean resizeImage(Path inFile, Path outFile, int targetWidth, int targetHeight){
		if(targetWidth <=0 || targetHeight <= 0) return false;
		
		try{
			BufferedImage toResize = null;
			IIOMetadata metadata = null;
			ImageReader reader = null;
			//attempt to resize
			try(ImageInputStream iis = ImageIO.createImageInputStream(inFile.toAbsolutePath().toFile())){
				Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
				if (!readers.hasNext()) return false;	//no image reader
				reader = readers.next();
				reader.setInput(iis, true, true);
				try {
					toResize = reader.read(0, reader.getDefaultReadParam());
					metadata = reader.getImageMetadata(0);
				}catch (Exception ex){
					logMe(ex);
					return false;
				} finally {
					reader.dispose();
				}
			}	//close input stream
			

			//configure orientation of targets sizes
			if ((targetWidth < targetHeight && toResize.getWidth() > toResize
					.getHeight())
					|| (targetWidth > targetHeight && toResize.getWidth() < toResize
							.getHeight())) {
				int tmp = targetWidth;
				targetWidth = targetHeight;
				targetHeight = tmp;
			}

			if (toResize.getWidth() < targetWidth
					|| toResize.getHeight() < targetHeight)
				return false; // it's already smaller than target

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

			try (ImageOutputStream ios = ImageIO
					.createImageOutputStream(outFile.toAbsolutePath().toFile())) {
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
			return true;
		}catch(Exception ex){
			logMe(ex);
			return false;
		}
		
	}
}
