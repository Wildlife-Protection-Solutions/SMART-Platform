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
package org.wcs.smart.connect.i18n.labels;

import java.util.Locale;

import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.observation.IObservationLabelProvider;
import org.wcs.smart.observation.json.IJsonFeatureProcessor;
import org.wcs.smart.observation.json.JsonFileProcessor;

/**
 * Desktop implementation of label provider for observation plugin
 * 
 * @author Emily
 *
 */
public class ObservationLabelProvider implements IObservationLabelProvider {

	@Override
	public String getLabel(Object item, Locale l) {

		if (item == JsonFileProcessor.Messages.INVALID_JSON) return Messages.getString("ObservationLabelProvider_invalidjson", l); //$NON-NLS-1$
		if (item == JsonFileProcessor.Messages.MISSING_TYPE) return Messages.getString("ObservationLabelProvider_invalidjsonmissingtype", l); //$NON-NLS-1$
		if (item == JsonFileProcessor.Messages.INVALID_TYPE) return Messages.getString("ObservationLabelProvider_invalidtype", l); //$NON-NLS-1$
		if (item == JsonFileProcessor.Messages.PROCESSOR_NOTFOUND) return Messages.getString("ObservationLabelProvider_processornotfound", l); //$NON-NLS-1$
		if (item == JsonFileProcessor.Messages.MISSING_PROPERTIES) return Messages.getString("ObservationLabelProvider_missingproperties", l); //$NON-NLS-1$
		if (item == JsonFileProcessor.Messages.MISSING_DATATYPE) return Messages.getString("ObservationLabelProvider_missingproperty1", l); //$NON-NLS-1$
		if (item == JsonFileProcessor.Messages.MISSING_FEATURETYPE) return Messages.getString("ObservationLabelProvider_missingproperty2", l); //$NON-NLS-1$
		
		if (item instanceof IJsonFeatureProcessor.Messages) {
			return getMessageLabel((IJsonFeatureProcessor.Messages)item, l);
		}
		return ""; //$NON-NLS-1$
	}
	
	private String getMessageLabel(IJsonFeatureProcessor.Messages item, Locale l) {
		switch(item) {
		case EMPLOYEE_NOT_FOUND: return Messages.getString("ObservationLabelProvider_employeenotfound", l); //$NON-NLS-1$
		case CATEGORY_NOT_FOUND: return Messages.getString("ObservationLabelProvider_categorynotfound", l); //$NON-NLS-1$
		case ATTRIBUTE_NOT_FOUND: return Messages.getString("ObservationLabelProvider_attributenotfound", l); //$NON-NLS-1$
		case INVALID_BOOLEAN_ATTRIBUTE: return Messages.getString("ObservationLabelProvider_invalidboolean", l); //$NON-NLS-1$
		case INVALID_DATE_ATTRIBUTE: return Messages.getString("ObservationLabelProvider_invaliddate", l); //$NON-NLS-1$
		case INVALID_LIST_ATTRIBUTE: return Messages.getString("ObservationLabelProvider_invalidlistitem", l); //$NON-NLS-1$
		case INVALID_MLIST_ATTRIBUTE: return Messages.getString("ObservationLabelProvider_invalidmlistitem", l); //$NON-NLS-1$
		case INVALID_MLIST2_ATTRIBUTE: return Messages.getString("ObservationLabelProvider_invalidemlist2", l); //$NON-NLS-1$
		case INVALID_TREE_ATTRIBUTE: return Messages.getString("ObservationLabelProvider_invalidtreeitem", l); //$NON-NLS-1$
		case INVALID_NUMBER_ATTRIBUTE: return Messages.getString("ObservationLabelProvider_invalidnumeric", l); //$NON-NLS-1$
		case SIGNATURE_TYPE_NOT_FOUND: return Messages.getString("ObservationLabelProvider.SignatureTypeNotFound", l); //$NON-NLS-1$
		case INVALID_CM_UUID: return Messages.getString("ObservationLabelProvider.InvalidCmUuid", l); //$NON-NLS-1$
		case CM_MISSING: return Messages.getString("ObservationLabelProvider.CmNotFound", l);	 //$NON-NLS-1$
		case INVALID_GEOMETRY_ATTRIBUTE: return "Invalid WKB for geometry attribute ''{0}''. Observation attribute will not be imported.";
		case INVALID_GEOMETRY_SRC_ATTRIBUTE: return "The value ''{0}'' is not a valid source value for Geometry attribute. The value will be set to Unknown";
		case INVALID_LINE_ATTRIBUTE: return "The geometry provided for attribute ''{0}'' is not a linear geometry. Observation attribute will not be imported.";
		case INVALID_POLYGON_ATTRIBUTE: return "The geometry provided for the attribute ''{0}'' is not a polygon geometry. Observation attribute will not be imported.";
		}
		return ""; //$NON-NLS-1$
	}

}
