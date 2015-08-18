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
package org.wcs.smart.connect.query;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.wcs.smart.er.model.SurveyWaypointSource;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.IWaypointSourceEngine;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
/**
 * Manager for dealing with waypoint
 * sources.
 * 
 * @author Emily
 *
 */
public enum WaypointSourceEngine implements IWaypointSourceEngine{
	
	INSTANCE;
	
	/**
	 * Cached sources
	 */
	private Map<String,IWaypointSource> supportedSources = new HashMap<String, IWaypointSource>();

	
	/**
	 * private constructor
	 */
	private WaypointSourceEngine(){
		supportedSources.put(PatrolWaypointSource.PATROL_WP_SOURCE_ID, new PatrolWaypointSource());
		supportedSources.put(IndepedentIncidentSource.KEY, new IndepedentIncidentSource());
		supportedSources.put(SurveyWaypointSource.KEY, new SurveyWaypointSource());
	}
	
	/**
	 * return set of all supported sources
	 * 
	 * @return
	 */
	public Collection<IWaypointSource> getSupportedSources(){
		return supportedSources.values();
	}
	/**
	 * Get the waypoint source for the given key
	 * @param sourceKey
	 * @return
	 */
	public IWaypointSource getSource(String sourceKey){
		return supportedSources.get(sourceKey);
	}
	
}
