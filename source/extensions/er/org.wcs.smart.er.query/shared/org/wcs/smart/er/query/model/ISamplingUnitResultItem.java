package org.wcs.smart.er.query.model;

import java.util.UUID;

public interface ISamplingUnitResultItem {

	/**
	 * Finds the properties value of the associated sampling
	 * unit attribute key.
	 * 
	 * @param attributeKey sampling unit attribute key
	 * @return the value associated with the given key
	 */
	public Object getSamplingUnitAttributeValue(String attributeKey);
	
	/**
	 * Adds an sampling unit property value to the result 
	 * @param key the attribute key
	 * @param value the attribute value
	 */
	public void addSamplingUnitAttributeValue(String key, Object value);
	
	
	/**
	 * sets the sampling unit uuid
	 * @param uuid
	 */
	public void setSamplingUnitUuid(UUID uuid);
	/**
	 * 
	 * @return the sampling unit uuid
	 */
	public UUID getSamplingUnitUuid();
	
	/**
	 * sampling unit id
	 * @return
	 */
	public String getSamplingUnitId() ;

	public void setSamplingUnitId(String samplingUnitId) ;
}


