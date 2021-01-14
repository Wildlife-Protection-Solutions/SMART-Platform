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
package org.wcs.smart.observation.model;

import java.util.UUID;

import org.wcs.smart.observation.internal.Messages;
import org.wcs.smart.observation.json.IJsonFeatureProcessor;

/**
 * A source provider that performs desktop specific processes associated with
 * a waypoint source. 
 * 
 * Each waypoint source type should have it's own source provider.
 * 
 * @author Emily
 *
 */
public interface IWaypointSourceProvider {

	/**
	 * Error string
	 */
	public static final String ERROR_STR = Messages.IWaypointSourceUiProvider_ErrorString;
	
	/**
	 * Finds and displays the UI editor associated with the 
	 * provided waypoint.
	 *  
	 * @param waypointUuid
	 */
	public default void findAndShow(UUID waypointUuid) {}
	
	/**
	 * Post processes data JSON data processed by the given
	 * feature processor.  Users should check the type of
	 * processor to ensure it is valid.  Should be used
	 * to fire events or other Desktop specific actions to
	 * be performed after json data is loaded.
	 * 
	 * @param processor
	 */
	public default void postProcessJsonData(IJsonFeatureProcessor processor) {}
}
