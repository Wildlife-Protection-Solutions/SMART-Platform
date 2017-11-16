package org.wcs.smart.asset.model.mapping;

import java.text.MessageFormat;

import org.wcs.smart.asset.data.importer.FileMetadataReader;
import org.wcs.smart.asset.data.importer.MetadataUtils;
import org.wcs.smart.asset.model.AssetMetadataMapping.MetadataType;

import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

public class ExifMetadataField implements IMetadataField<Metadata>{

	private Tag tag;
	private int tagType;
	private String tagValue;
	
	
	public ExifMetadataField(int tagType) {
		this.tagType = tagType;
	}
	
	public ExifMetadataField(int tagType, String tagValue) {
		this.tagType = tagType;
		this.tagValue = tagValue.trim().isEmpty() ? null : tagValue;
	}
	
	public ExifMetadataField(Tag tag, String tagValue) {
		this(tag);
		this.tagValue = tagValue.trim().isEmpty() ? null : tagValue;
	}
	
	public ExifMetadataField(Tag tag) {
		this(tag.getTagType());
		this.tagValue = null;
	}
	
	public String getTagValue() {
		return this.tagValue;
	}
	
	@Override
	public String asUserString() {
		if (tag == null) {
			tag = MetadataUtils.findTagName(tagType);
		}
		if (tag == null) return MessageFormat.format("Tag: {0}", String.format("0x%04x", tagType));
		return MessageFormat.format("Tag: {0} ({1})", tag.getTagName(), tag.getTagTypeHex());
	}

	@Override
	public String asString() {
		return String.valueOf(tagType) + "|" +  (tagValue == null? "" : tagValue);
	}

	/**
	 * Returns the directory that continas the tagType of
	 * this field.
	 */
	@Override
	public Object findValue(Metadata metadata) {
		return FileMetadataReader.findDirectory(metadata, null, this.tagType);
	}

	public int getTagType() {
		return this.tagType;
	}
	
	@Override
	public MetadataType getType() {
		return MetadataType.EXIF;
	}

	
	public static ExifMetadataField parseMapping(String mappingString) {
		if (mappingString == null) return null; //TODO: throw an exception
		String[] bits = mappingString.split("\\|");
		if (bits.length == 1) return new ExifMetadataField(Integer.valueOf(bits[0]));
		if (bits.length == 2) return new ExifMetadataField(Integer.valueOf(bits[0]), bits[1]);
		return null; //TODO: throw an exception
	}
}
