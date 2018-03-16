package org.wcs.smart.event.model;

import java.util.Locale;

public interface IActionParameter {

	/**
	 * Parameter unique identifier
	 * @return
	 */
	public String getKey();
	
	/**
	 * 
	 * @param l the parameter name for the given locale
	 * @return
	 */
	public String getName(Locale l);
	
	/**
	 * 
	 * @return if the parameter is required or not
	 */
	public boolean isRequired();
}
