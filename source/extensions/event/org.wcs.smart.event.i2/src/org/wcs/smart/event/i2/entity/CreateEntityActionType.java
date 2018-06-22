/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.event.i2.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.EFilter;
import org.wcs.smart.event.model.IActionParameter;
import org.wcs.smart.event.model.IActionType;
import org.wcs.smart.observation.model.WaypointObservation;

/**
 * Action that creates a new profile entity.
 * 
 * @author Emily
 *
 */
public class CreateEntityActionType implements IActionType {

	public static final String KEY = "org.wcs.smart.profile.i2.newentity"; //$NON-NLS-1$

	private static Logger logger = Logger.getLogger(CreateEntityActionType.class.getCanonicalName());

	private List<IActionParameter> parameters;
	
	public CreateEntityActionType() {
		parameters = new ArrayList<>();
		parameters.add(EntityTypeParameter.INSTANCE);
		parameters.add(MappingParameter.INSTANCE);
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getName(Locale l) {
		return "Create Profile Entity";
	}

	@Override
	public String getDescription(Locale l) {
		return "Creates a new entity in the profiles module optionally mapping data model attributes to entity attributes";
	}

	@Override
	public List<IActionParameter> getActionParameters() {
		return parameters;
	}

	@Override
	public void performAction(EAction action, EFilter filter, WaypointObservation data, Locale l) {
		//TODO:
	}

}
