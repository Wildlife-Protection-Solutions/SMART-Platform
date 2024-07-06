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
package org.wcs.smart.observation;

import java.util.Locale;

import org.wcs.smart.observation.internal.Messages;
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

		if (item == JsonFileProcessor.Messages.INVALID_JSON) return Messages.ObservationLabelProvider_invaldjson;
		if (item == JsonFileProcessor.Messages.MISSING_TYPE) return Messages.ObservationLabelProvider_typemissing;
		if (item == JsonFileProcessor.Messages.INVALID_TYPE) return Messages.ObservationLabelProvider_invalidtypeattribute;
		if (item == JsonFileProcessor.Messages.PROCESSOR_NOTFOUND) return Messages.ObservationLabelProvider_noprocessor;
		if (item == JsonFileProcessor.Messages.MISSING_PROPERTIES) return Messages.ObservationLabelProvider_missingproperties;
		if (item == JsonFileProcessor.Messages.MISSING_DATATYPE) return Messages.ObservationLabelProvider_mssingproperty1;
		if (item == JsonFileProcessor.Messages.MISSING_FEATURETYPE) return Messages.ObservationLabelProvider_missingproperty2;
	
		if (item instanceof IJsonFeatureProcessor.Messages) {
			return getMessage((IJsonFeatureProcessor.Messages)item, l);
		}
		return ""; //$NON-NLS-1$
	}
	
	public String getMessage(IJsonFeatureProcessor.Messages item, Locale l) {
		switch(item) {
		case EMPLOYEE_NOT_FOUND: return Messages.ObservationLabelProvider_employeenotfound;
		case CATEGORY_NOT_FOUND: return Messages.ObservationLabelProvider_categorynotfound;
		case ATTRIBUTE_NOT_FOUND: return Messages.ObservationLabelProvider_attributenotfound;
		case INVALID_BOOLEAN_ATTRIBUTE: return Messages.ObservationLabelProvider_invalidbooleanvalue;
		case INVALID_DATE_ATTRIBUTE: return Messages.ObservationLabelProvider_invaluddatevalue;
		case INVALID_TIME_ATTRIBUTE: return Messages.ObservationLabelProvider_invalidetimevalue;
		case INVALID_LIST_ATTRIBUTE: return Messages.ObservationLabelProvider_invalidlistitem;
		case INVALID_MLIST_ATTRIBUTE: return Messages.ObservationLabelProvider_invalidmultilist1;
		case INVALID_MLIST2_ATTRIBUTE: return Messages.ObservationLabelProvider_invalidmultilist2;
		case INVALID_TREE_ATTRIBUTE: return Messages.ObservationLabelProvider_invalidtreeitem;
		case INVALID_NUMBER_ATTRIBUTE: return Messages.ObservationLabelProvider_invalidnumbervalue;
		case SIGNATURE_TYPE_NOT_FOUND: return Messages.ObservationLabelProvider_SignatureTypeNotFound;
		case ATTACHMENT_TAG_NOT_FOUND: return Messages.ObservationLabelProvider_TagNotFound;
		case INVALID_CM_UUID: return Messages.ObservationLabelProvider_InvalidCmUuid;
		case CM_MISSING: return Messages.ObservationLabelProvider_CmNotFound;
		case INVALID_GEOMETRY_ATTRIBUTE: return Messages.ObservationLabelProvider_0;
		case INVALID_GEOMETRY_SRC_ATTRIBUTE: return Messages.ObservationLabelProvider_InvalidSource;
		case INVALID_LINE_ATTRIBUTE: return Messages.ObservationLabelProvider_NotLineGeometry;
		case INVALID_POLYGON_ATTRIBUTE: return Messages.ObservationLabelProvider_NotPolygonGeometry;
		}
		return ""; //$NON-NLS-1$
	}

}
