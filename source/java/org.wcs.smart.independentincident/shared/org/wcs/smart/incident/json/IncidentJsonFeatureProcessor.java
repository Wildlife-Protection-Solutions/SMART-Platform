/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.incident.json;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.Session;
import org.json.simple.JSONObject;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.incident.IIncidentLabelProvider;
import org.wcs.smart.incident.IndepedentIncidentSource;
import org.wcs.smart.observation.json.IJsonFeatureProcessor;
import org.wcs.smart.observation.model.Waypoint;

import com.ibm.icu.text.MessageFormat;

/**
 * Processes incident JSON features into SMART Independent Incidents
 * 
 * @author Emily
 *
 */
public class IncidentJsonFeatureProcessor extends IJsonFeatureProcessor {

	private static final String DATATYPE = "incident"; //$NON-NLS-1$
	
	public enum Messages{
		INVALID_DATA_TYPE,
		INVALID_FEATURE_TYPE,
		COMPLETE_MSG;
		
		public String getMessage(Locale l) {
			return SmartContext.INSTANCE.getClass(IIncidentLabelProvider.class).getLabel(this, l);
		}
	}
	
	private Set<Waypoint> createdFeatures = new HashSet<>();

	/**
	 * @return <code>true</code> if this processor can process the given feature
	 * type.  
	 */
	@Override
	public boolean canProcess(String featureType) {
		return featureType.equalsIgnoreCase(DATATYPE); 
	}

	/**
	 * 
	 * @return set of features created by this processor
	 */
	public Set<Waypoint> getCreatedFeatures(){
		return this.createdFeatures;
	}
	
	@Override
	public void processFeature(JSONObject feature, ConservationArea ca, Session session, Locale l) throws Exception {

		JSONObject props = (JSONObject) feature.get(JSON_PROPERTIES);

		String dtype = props.get(JSON_SMARTDATATYPE).toString(); 
		if (!dtype.equalsIgnoreCase(DATATYPE))
			throw new Exception(MessageFormat.format(Messages.INVALID_DATA_TYPE.getMessage(l), dtype, DATATYPE));

		String ftype = props.get(JSON_SMARTFEATURETYPE).toString();
		if (!ftype.equalsIgnoreCase(JSON_FT_OBSERVATION))
		throw new Exception(MessageFormat.format(Messages.INVALID_FEATURE_TYPE.getMessage(l), ftype, JSON_FT_OBSERVATION));

		Waypoint wp = super.createWaypoint(feature, ca, session, l);
		wp.setSourceId(IndepedentIncidentSource.KEY);
		
		//reset uuids as we don't do any mapping at this time
		//the future plan is to map these to existing objects in our 
		//database and update the existing objects
		wp.setUuid(null);
		wp.getObservationGroups().forEach(g->g.setUuid(null));

		session.saveOrUpdate(wp);
		createdFeatures.add(wp);
	}

	/**
	 * Creates a user friendly message describing the actions 
	 * applied to the database
	 */
	@Override
	public String getMessage(Locale l) {
		if (createdFeatures.isEmpty())
			return null;
 
		return MessageFormat.format(Messages.COMPLETE_MSG.getMessage(l), createdFeatures.size(),
				createdFeatures.stream().map(wp->wp.getId()).collect(Collectors.joining(", "))); //$NON-NLS-1$
	}
	
}
