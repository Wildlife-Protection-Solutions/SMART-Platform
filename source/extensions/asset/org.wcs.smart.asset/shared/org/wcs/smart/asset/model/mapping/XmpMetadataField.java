package org.wcs.smart.asset.model.mapping;

import java.text.MessageFormat;

import org.wcs.smart.asset.data.importer.FileMetadataReader;
import org.wcs.smart.asset.model.AssetMetadataMapping.MetadataType;

import com.drew.metadata.Metadata;

public class XmpMetadataField implements IMetadataField<Metadata>{

	private String directory;
	private String tag;
	
	public XmpMetadataField(String directoryName, String tagName) {
		this.directory = directoryName;
		this.tag = tagName;
	}
	
	@Override
	public String asUserString() {
		return MessageFormat.format("{0} ({1})", tag, directory);
	}

	@Override
	public String asString() {
		return directory + "|" + tag;
	}

	@Override
	public String findValue(Metadata metadata) {
		return FileMetadataReader.findValue(metadata, this.directory, this.tag);
	}

	@Override
	public MetadataType getType() {
		return MetadataType.XMP;
	}

	
	public static XmpMetadataField parseMapping(String mappingString) {
		String[] bits = mappingString.split("\\|");
		if (bits.length != 2) return null; //TODO: throw an exception
		return new XmpMetadataField(bits[0], bits[1]);
	}
}
