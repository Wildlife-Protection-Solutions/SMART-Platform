package org.wcs.smart.event.model;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public interface IActionType {

	public static final String EXTENSION_ID = "org.wcs.smart.event.actiontype"; //$NON-NLS-1$
	
	public static final int MAX_KEY_LENGTH = 128;
	
	/**
	 * A unique identifier for the action type.  
	 * Maximum string length is 128 
	 * @return
	 */
	public String getKey();
	
	/**
	 * Get the name of the action type for the given locale
	 * 
	 * @param l
	 * @return
	 */
	public String getName(Locale l);
	
	/**
	 * The action type description
	 * @param l
	 * @return
	 */
	public String getDescription(Locale l);
	
	/**
	 * 
	 * @return must not return null; but can return an empty list is no parameters supported
	 */
	public List<IActionParameter> getActionParameters();
	
	public void performAction(Collection<EActionParameterValue> parameters, ActionData data);
	
}
