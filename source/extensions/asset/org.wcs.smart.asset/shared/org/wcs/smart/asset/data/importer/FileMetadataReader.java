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
package org.wcs.smart.asset.data.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.wcs.smart.asset.model.mapping.XmpMetadataField;
import org.wcs.smart.ca.ConservationArea;

import com.adobe.xmp.XMPIterator;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.properties.XMPPropertyInfo;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDirectoryBase;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.xmp.XmpDirectory;

/**
 * Utilities for reading metadata from varioud images files.
 * @author Emily
 *
 */
public class FileMetadataReader {

	private static Logger logger = Logger.getLogger(XmpMetadataField.class.getCanonicalName());
	
	/**
	 * Create file proxy from the file for the given conservation area
	 * @param file
	 * @param ca
	 * @return
	 * @throws Exception
	 */
	public static FileProxy readFile(Path file, ConservationArea ca) throws Exception {
		//at a minimum lets read the lat/long
		FileProxy fileInfo = new FileProxy(file, ca);
		readExifMetadata(file, fileInfo);
		return fileInfo;
	}
	
	private static void readExifMetadata(Path file, FileProxy fileInfo) throws ImageProcessingException, IOException {
		Metadata metadata = ImageMetadataReader.readMetadata(file.toFile());
		
		for (Directory directory : metadata.getDirectoriesOfType(GpsDirectory.class)) {
			GeoLocation geoLocation = ((GpsDirectory)directory).getGeoLocation();
			if (geoLocation != null){
				Date dateTime = ((GpsDirectory) directory).getGpsDate();
				
				fileInfo.setPosition(geoLocation.getLongitude(), geoLocation.getLatitude());
				fileInfo.setImageDate(dateTime);
			}			
		}
		
		//check other directories for a date
		if (fileInfo.getImageDate() == null) {
			for (Directory directory : metadata.getDirectoriesOfType(ExifSubIFDDirectory.class)) {
				Date orig = ((ExifSubIFDDirectory)directory).getDateOriginal();
				if (orig != null) {
					fileInfo.setImageDate(orig);
					break;
				}
				Date digit = ((ExifSubIFDDirectory)directory).getDateDigitized();
				if (digit != null) {
					fileInfo.setImageDate(digit);
					break;
				}
			}
		}
		if (fileInfo.getImageDate() == null) {
			for (Directory directory : metadata.getDirectoriesOfType(ExifIFD0Directory.class)) {
				Date date = directory.getDate(ExifDirectoryBase.TAG_DATETIME);
				if (date != null) {
					fileInfo.setImageDate(date);
				}
					
			}
		}
			
	}
	
	/**
	 * Gets the xmp metadata from the file as a list of
	 * String[] where the first element is the tag path and the second
	 * the tag value.
	 * @param file
	 * @return
	 * @throws Exception
	 */
	public static List<String[]> readXmpMetadata(Path file) throws Exception{
		Metadata metadata = ImageMetadataReader.readMetadata(file.toFile());
		XmpDirectory xmp = metadata.getFirstDirectoryOfType(XmpDirectory.class);
		return readXmpMetadata(xmp);
	}
	
	/**
	 * Read xmp metadata from the given xmp directory
	 * String[] where the first element is the tag path and the second
	 * the tag value.
	 * 
	 * @param xmpDirectory
	 * @return
	 */
	public static List<String[]> readXmpMetadata(XmpDirectory xmpDirectory){
		List<String[]> xmpMetadata = new ArrayList<>();
		if (xmpDirectory != null) {
			XMPMeta meta = xmpDirectory.getXMPMeta();
			try {
				XMPIterator i = meta.iterator();
				while(i.hasNext()) {
					XMPPropertyInfo info = (XMPPropertyInfo)i.next();
					if (info.getPath() != null && !info.getPath().isEmpty())
						xmpMetadata.add(new String[] {info.getPath(), info.getValue()});
				}
			}catch (Exception ex) {
				logger.log(Level.WARNING, ex.getMessage(), ex);
			}
		}
		return xmpMetadata;
	}
	
	/**
	 * 
	 * @param file
	 * @return  null indicates some error reading file; empty hash map
	 * indicates no exif metadata found
	 */
	public static HashMap<Directory, List<Tag>> readExifMetadata(Path file){
		HashMap<Directory, List<Tag>> results = new HashMap<>();
		
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(file.toFile());
			for (Directory directory : metadata.getDirectories()) {
				List<Tag> tags = new ArrayList<>();
				for (Tag g : directory.getTags()) {
					tags.add(g);
				}
				results.put(directory, tags);
			}
			return results;
		}catch (Exception ex) {
			logger.log(Level.WARNING,ex.getMessage(),ex);
			return null;
		}
	}
	
	public static Metadata readMetadata(Path file) throws Exception {
		return ImageMetadataReader.readMetadata(file.toFile());
	}
	
	
	/**
	 * 
	 * @param metadata
	 * @param directoryName directory name is optional; if null all directories will be searched
	 * @param tagType
	 * @return
	 */
	public static Directory findDirectory(Metadata metadata, String directoryName, int tagType) {
		if (metadata == null) return null;
		for (Directory directory : metadata.getDirectories()) {
			if (directoryName != null && !directory.getName().equals(directoryName)) continue;
			for (Tag g : directory.getTags()) {
				if (g.getTagType() == tagType) return directory;
			}
		}
		return null;
	}
}