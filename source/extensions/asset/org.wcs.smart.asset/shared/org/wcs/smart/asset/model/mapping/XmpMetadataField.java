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

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.wcs.smart.asset.model.AssetMetadataMapping.MetadataType;

import com.adobe.internal.xmp.XMPIterator;
import com.adobe.internal.xmp.properties.XMPPropertyInfo;
import com.drew.metadata.Metadata;
import com.drew.metadata.xmp.XmpDirectory;

/**
 * XMP metadata field mapping
 * @author Emily
 *
 */
public class XmpMetadataField implements IMetadataField{

	private static Logger logger = Logger.getLogger(XmpMetadataField.class.getCanonicalName());
	
	private static final String SEPERATOR = "|"; //$NON-NLS-1$
	
	/*
	 * Metadata path (eg. ns:item) 
	 */
	private String path;
	
	/*
	 * Exif metadata tag value to map (optional, can be null)
	 */
	private String value;
	
	
	/**
	 * Creates a new mapping for a given metadata
	 * path.  Will match irregardless of the path value.
	 * 
	 * @param tagType
	 */
	public XmpMetadataField(String path) {
		this.path = path;
	}
	
	/**
	 * Creates a new mapping for the given metadata
	 * path and value .  Will only match
	 * if the value of the path matches the value.
	 * 
	 * @param tagType
	 * @param value
	 */
	public XmpMetadataField(String path, String value) {
		this(path);
		this.value = (value == null || value.trim().isEmpty()) ? null : value;
	}
	
	/**
	 * The matching value. Will return null
	 * if all values should be matched.
	 * 
	 * @return
	 */
	public String getValue() {
		return this.value;
	}
	
	/**
	 * Xmp Path
	 * @return
	 */
	public String getPath() {
		return this.path;
	}
	
	@Override
	public String keyAsString(Locale l) {
		return path;
	}
	
	@Override
	public String valueAsString() {
		if (value == null) return ""; //$NON-NLS-1$
		return value;
	}

	@Override
	public String asString() {
		return String.valueOf(path) + SEPERATOR +  (value == null? "" : value); //$NON-NLS-1$ 
	}

	/**
	 * Returns the {@link String} that matches the path/
	 * 
	 * @param metadata this field assumes this object is of 
	 * type {@link Metadata}
	 */
	@Override
	public Object findValue(Object metadata) {
		if (!(metadata instanceof Metadata)) return null;
		try {
			XmpDirectory xmpDirectory = ((Metadata)metadata).getFirstDirectoryOfType(XmpDirectory.class);
			if (xmpDirectory == null) return null;
			if (xmpDirectory.getXMPMeta() == null) return null;
			XMPIterator itr = xmpDirectory.getXMPMeta().iterator();
			while(itr.hasNext()) {
				XMPPropertyInfo info = (XMPPropertyInfo)itr.next();
				if (info.getPath() != null && !info.getPath().isEmpty() && info.getPath().equalsIgnoreCase(path)) {
					return info.getValue();
				}
			}
		}catch (Exception ex) {
			logger.log(Level.WARNING, ex.getMessage(), ex);
			return null;
		}
		return null;
	}

	
	@Override
	public MetadataType getType() {
		return MetadataType.XMP;
	}

	
	/**
	 * Converts a mapping string to an XmpMetdataField. Returns null
	 * if mappingString cannot be parsed.
	 * @param mappingString
	 * @return
	 */
	public static XmpMetadataField parseMapping(String mappingString) {
		if (mappingString == null) return null;
		String[] bits = mappingString.split("\\" + SEPERATOR); //$NON-NLS-1$
		if (bits.length == 1) return new XmpMetadataField(bits[0]);
		if (bits.length == 2) return new XmpMetadataField(bits[0], bits[1]);
		return null;
	}
}
