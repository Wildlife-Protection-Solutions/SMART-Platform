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

import org.wcs.smart.asset.model.AssetMetadataMapping;

/**
 * Metadata mapping.  Mapping from a metadata field to
 * a SMART asset field or observation field.
 * 
 * @author Emily
 *
 */
public interface IMetadataField {

	/**
	 * Converts the metadata mapping to a user friendly description
	 * @return
	 */
	public String keyAsString();
	
	/**
	 * Converts the metadata mapping to a user friendly description
	 * @return
	 */
	public String valueAsString();
	
	/**
	 * Serializes the metadata mapping to a string;
	 * 
	 * @return
	 */
	public String asString();
	
	
	/**
	 * Finds the value represented by the mapping in the
	 * given file.  
	 * 
	 * @param file the metadata object to search
	 * @return the value represented as a string; null
	 * if an error occurs of value not found
	 */
	public Object findValue(Object file);
	
	/**
	 * 
	 * @return the metadata mapping type
	 */
	public AssetMetadataMapping.MetadataType getType();
}
