package org.wcs.smart.asset.data.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.wcs.smart.ca.ConservationArea;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.GpsDirectory;

public class FileMetadataReader {

	
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
	}
	
	/**
	 * 
	 * @param file
	 * @return  null indicates some error reading file; empty hash map
	 * indicates not exif metadata found
	 */
	public static HashMap<String, List<String[]>> readExifMetadata(Path file){
		HashMap<String, List<String[]>> results = new HashMap<>();
		
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(file.toFile());
			for (Directory directory : metadata.getDirectories()) {
				List<String[]> tags = new ArrayList<>();
				for (Tag g : directory.getTags()) {
					tags.add(new String[] {g.getTagName(), g.getDescription()});
				}
				results.put(directory.getName(), tags);
			}
			return results;
		}catch (Exception ex) {
			//TODO: error
			return null;
		}
	}
	
	public static Metadata readMetadata(Path file) {
		try {
			ImageMetadataReader.readMetadata(file.toFile());
		}catch (Exception ex) {
			//TODO:
		}
		return null;
	}
	
	
	public static String findValue(Metadata metadata, String directoryName, String tagName) {
		if (metadata == null) return null;
		for (Directory directory : metadata.getDirectories()) {
			if (!directory.getName().equalsIgnoreCase(directoryName)) continue;
			for (Tag g : directory.getTags()) {
				if (g.getTagName().equalsIgnoreCase(tagName)) return g.getDescription();
			}
		}
		return null;
	}
}


//	public static void main(String args[]) throws Exception{
//		Path p = Paths.get("C:\\Users\\Emily\\Desktop\\images\\IMG_9192.JPG");
//		FileMetadataReader.readExifMetadata(p, null);
//		
//		System.out.println("test");
//		for (Directory d : metadata.getDirectories()) {
//			for (Tag t : d.getTags()) {
//				
//				System.out.println(t.getDirectoryName() + ":" + t.getTagName());
//			}
//		}
//	}
	/*
	 * test
JPEG:Compression Type
JPEG:Data Precision
JPEG:Image Height
JPEG:Image Width
JPEG:Number of Components
JPEG:Component 1
JPEG:Component 2
JPEG:Component 3
Exif IFD0:Make
Exif IFD0:Model
Exif IFD0:Orientation
Exif IFD0:X Resolution
Exif IFD0:Y Resolution
Exif IFD0:Resolution Unit
Exif IFD0:Software
Exif IFD0:Date/Time
Exif IFD0:YCbCr Positioning
Exif SubIFD:Exposure Time
Exif SubIFD:F-Number
Exif SubIFD:Exposure Program
Exif SubIFD:ISO Speed Ratings
Exif SubIFD:Exif Version
Exif SubIFD:Date/Time Original
Exif SubIFD:Date/Time Digitized
Exif SubIFD:Components Configuration
Exif SubIFD:Shutter Speed Value
Exif SubIFD:Aperture Value
Exif SubIFD:Brightness Value
Exif SubIFD:Exposure Bias Value
Exif SubIFD:Metering Mode
Exif SubIFD:Flash
Exif SubIFD:Focal Length
Exif SubIFD:Subject Location
Exif SubIFD:Sub-Sec Time Original
Exif SubIFD:Sub-Sec Time Digitized
Exif SubIFD:FlashPix Version
Exif SubIFD:Color Space
Exif SubIFD:Exif Image Width
Exif SubIFD:Exif Image Height
Exif SubIFD:Sensing Method
Exif SubIFD:Scene Type
Exif SubIFD:Exposure Mode
Exif SubIFD:White Balance Mode
Exif SubIFD:Focal Length 35
Exif SubIFD:Scene Capture Type
Exif SubIFD:Lens Specification
Exif SubIFD:Lens Make
Exif SubIFD:Lens Model
Apple Makernote:Unknown tag (0x0001)
Apple Makernote:Run Time
Apple Makernote:Unknown tag (0x0004)
Apple Makernote:Unknown tag (0x0005)
Apple Makernote:Unknown tag (0x0006)
Apple Makernote:Unknown tag (0x0007)
Apple Makernote:Unknown tag (0x0008)
Apple Makernote:Unknown tag (0x0014)
GPS:GPS Latitude Ref
GPS:GPS Latitude
GPS:GPS Longitude Ref
GPS:GPS Longitude
GPS:GPS Altitude Ref
GPS:GPS Altitude
GPS:GPS Time-Stamp
GPS:GPS Speed Ref
GPS:GPS Speed
GPS:GPS Img Direction Ref
GPS:GPS Img Direction
GPS:GPS Dest Bearing Ref
GPS:GPS Dest Bearing
GPS:GPS Date Stamp
GPS:Unknown tag (0x001f)
Exif Thumbnail:Compression
Exif Thumbnail:X Resolution
Exif Thumbnail:Y Resolution
Exif Thumbnail:Resolution Unit
Exif Thumbnail:Thumbnail Offset
Exif Thumbnail:Thumbnail Length
Huffman:Number of Tables
File:File Name
File:File Size
File:File Modified Date
*/
