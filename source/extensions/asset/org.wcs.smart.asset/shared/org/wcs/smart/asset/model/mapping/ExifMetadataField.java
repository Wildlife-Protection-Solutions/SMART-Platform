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
package org.wcs.smart.asset.model.mapping;

import java.text.MessageFormat;

import org.wcs.smart.asset.data.importer.FileMetadataReader;
import org.wcs.smart.asset.data.importer.MetadataUtils;
import org.wcs.smart.asset.model.AssetMetadataMapping.MetadataType;

import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;

/**
 * Exif metadata field mapping
 * @author Emily
 *
 */
public class ExifMetadataField implements IMetadataField{

	/*
	 * Metadata tag, only valid 
	 */
	private Tag tag;
	/*
	 * Exif metadata tag type
	 */
	private int tagType;
	/*
	 * Exif metadata tag value to map (optional, can be null)
	 */
	private String tagValue;
	
	
	/**
	 * Creates a new mapping for a given metadata
	 * tag.  Will match irregardless of the value.
	 * 
	 * @param tagType
	 */
	public ExifMetadataField(int tagType) {
		this.tagType = tagType;
	}
	
	/**
	 * Creates a new mapping for the given metadata
	 * tag and tagValue.  Will only match
	 * if the value of the tag matches the tagValue.
	 * 
	 * @param tagType
	 * @param tagValue
	 */
	public ExifMetadataField(int tagType, String tagValue) {
		this.tagType = tagType;
		this.tagValue = tagValue.trim().isEmpty() ? null : tagValue;
	}
	
	/**
	 * Creates a new mapping for the given metadata
	 * tag and tagValue.
	 * 
	 * @param tag
	 * @param tagValue
	 */
	public ExifMetadataField(Tag tag, String tagValue) {
		this(tag);
		this.tagValue = tagValue.trim().isEmpty() ? null : tagValue;
	}
	
	/**
	 * Creates a new mapping for the given metadata tag.
	 * @param tag
	 */
	public ExifMetadataField(Tag tag) {
		this(tag.getTagType());
		this.tagValue = null;
	}
	
	/**
	 * The matching tag value. Will return null
	 * if all values should be matched.
	 * 
	 * @return
	 */
	public String getTagValue() {
		return this.tagValue;
	}
	
	@Override
	public String keyAsString() {
		if (tag == null) {
			tag = MetadataUtils.findTagName(tagType);
		}
		if (tag == null) {
			return MessageFormat.format("Tag: {0}", String.format("0x%04x", tagType));
		}
		return MessageFormat.format("Tag: {0} ({1})", tag.getTagName(), tag.getTagTypeHex());
	}
	
	@Override
	public String valueAsString() {
		if (tagValue == null) return "";
		return tagValue;
	}

	@Override
	public String asString() {
		return String.valueOf(tagType) + "|" +  (tagValue == null? "" : tagValue);
	}

	/**
	 * Returns the {@link Directory} that contains the tagType of
	 * this metadata mapping field.
	 * @param metadata this field assumes this object is of 
	 * type {@link Metadata}
	 */
	@Override
	public Object findValue(Object metadata) {
		if (!(metadata instanceof Metadata)) return null;
		return FileMetadataReader.findDirectory((Metadata)metadata, null, this.tagType);
	}

	/**
	 * The exif tag type
	 * @return
	 */
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
