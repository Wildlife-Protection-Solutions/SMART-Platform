package org.wcs.smart.asset.model.mapping;

import java.text.MessageFormat;

import org.wcs.smart.asset.data.importer.FileMetadataReader;
import org.wcs.smart.asset.model.AssetMetadataMapping.MetadataType;

import com.drew.metadata.Metadata;

public class XmpMetadataField implements IMetadataField<Metadata>{

	private String directory;
	private String tag;
	private String tagValue;
	
	
	public XmpMetadataField(String directoryName, String tagName, String tagValue) {
		this(directoryName, tagName);
		this.tagValue = tagValue.trim().isEmpty() ? null : tagValue;
	}
	
	public XmpMetadataField(String directoryName, String tagName) {
		this.directory = directoryName;
		this.tag = tagName;
		this.tagValue = null;
	}
	
	public String getTagValue() {
		return this.tagValue;
	}
	
	@Override
	public String asUserString() {
		return MessageFormat.format("{0} ({1})", tag, directory);
	}

	@Override
	public String asString() {
		return directory + "|" + tag + "|" +  (tagValue == null? "" : tagValue);
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
		if (bits.length == 2) return new XmpMetadataField(bits[0], bits[1]);
		if (bits.length == 3) return new XmpMetadataField(bits[0], bits[1], bits[2]);
		return null; //TODO: throw an exception
	}
}
