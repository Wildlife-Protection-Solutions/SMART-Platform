package org.wcs.smart.asset.data.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.wcs.smart.asset.AssetPlugIn;
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
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.xmp.XmpDirectory;

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
				AssetPlugIn.log(ex.getMessage(),  ex);
			}
		}
		return xmpMetadata;
	}
	
	/**
	 * 
	 * @param file
	 * @return  null indicates some error reading file; empty hash map
	 * indicates not exif metadata found
	 */
	public static HashMap<Directory, List<Tag>> readExifMetadata(Path file){
		HashMap<Directory, List<Tag>> results = new HashMap<>();
		
		try {
			Metadata metadata = ImageMetadataReader.readMetadata(file.toFile());
			for (Directory directory : metadata.getDirectories()) {
				List<Tag> tags = new ArrayList<>();
				for (Tag g : directory.getTags()) {
//					tags.add(new String[] {g.getTagName(), g.getDescription()});
					tags.add(g);
				}
				results.put(directory, tags);
			}
			return results;
		}catch (Exception ex) {
			//TODO: error
			return null;
		}
	}
	
	public static Metadata readMetadata(Path file) {
		try {
			return ImageMetadataReader.readMetadata(file.toFile());
		}catch (Exception ex) {
			ex.printStackTrace();
			//TODO:
		}
		return null;
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