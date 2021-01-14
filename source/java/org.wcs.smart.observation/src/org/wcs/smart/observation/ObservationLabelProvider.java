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
		
		if (item == IJsonFeatureProcessor.Messages.EMPLOYEE_NOT_FOUND) return Messages.ObservationLabelProvider_employeenotfound;
		if (item == IJsonFeatureProcessor.Messages.CATEGORY_NOT_FOUND) return Messages.ObservationLabelProvider_categorynotfound;
		if (item == IJsonFeatureProcessor.Messages.ATTRIBUTE_NOT_FOUND) return Messages.ObservationLabelProvider_attributenotfound;
		if (item == IJsonFeatureProcessor.Messages.INVALID_BOOLEAN_ATTRIBUTE) return Messages.ObservationLabelProvider_invalidbooleanvalue;
		if (item == IJsonFeatureProcessor.Messages.INVALID_DATE_ATTRIBUTE) return Messages.ObservationLabelProvider_invaluddatevalue;
		if (item == IJsonFeatureProcessor.Messages.INVALID_LIST_ATTRIBUTE) return Messages.ObservationLabelProvider_invalidlistitem;
		if (item == IJsonFeatureProcessor.Messages.INVALID_MLIST_ATTRIBUTE) return Messages.ObservationLabelProvider_invalidmultilist1;
		if (item == IJsonFeatureProcessor.Messages.INVALID_MLIST2_ATTRIBUTE) return Messages.ObservationLabelProvider_invalidmultilist2;
		if (item == IJsonFeatureProcessor.Messages.INVALID_TREE_ATTRIBUTE) return Messages.ObservationLabelProvider_invalidtreeitem;
		if (item == IJsonFeatureProcessor.Messages.INVALID_NUMBER_ATTRIBUTE) return Messages.ObservationLabelProvider_invalidnumbervalue;

		return ""; //$NON-NLS-1$
	}

}
