package org.wcs.smart.shared.labels;

import java.util.Locale;

import org.wcs.smart.incident.IIncidentLabelProvider;
import org.wcs.smart.incident.IndepedentIncidentSource;

public class IncidentLabelProvider implements IIncidentLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {
		if (item instanceof IndepedentIncidentSource){
			return "Independent Incident";
		}
		return null;
	}

}
