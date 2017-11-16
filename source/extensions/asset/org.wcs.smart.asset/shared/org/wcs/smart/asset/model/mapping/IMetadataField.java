package org.wcs.smart.asset.model.mapping;

import org.wcs.smart.asset.model.AssetMetadataMapping;

public interface IMetadataField<T> {

	/**
	 * Converts the metadata mapping to a user friendly description
	 * @return
	 */
	public String asUserString();
	
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
	 * @param file the file to search
	 * @return the value represented as a string; null
	 * if an error occurs of value not found
	 */
	public Object findValue(T file);
	
	public AssetMetadataMapping.MetadataType getType();
}
