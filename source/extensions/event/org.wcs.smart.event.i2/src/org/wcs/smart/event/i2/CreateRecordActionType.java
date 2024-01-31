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
package org.wcs.smart.event.i2;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.event.EventPlugIn;
import org.wcs.smart.event.i2.internal.Messages;
import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.EActionParameterValue;
import org.wcs.smart.event.model.EFilter;
import org.wcs.smart.event.model.IActionParameter;
import org.wcs.smart.event.model.IActionType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelObservation;
import org.wcs.smart.i2.model.IntelObservationAttribute;
import org.wcs.smart.i2.model.IntelObservationAttributeList;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelProfileRecordSource;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationAttributeList;

/**
 * Create new profile record action type
 * 
 * @author Emily
 *
 */
public class CreateRecordActionType implements IActionType {

	private static Logger logger = Logger.getLogger(CreateRecordActionTypeExecutor.class.getCanonicalName());

	private List<IActionParameter> parameters;
	
	public CreateRecordActionType() {
		parameters = new ArrayList<>();
		parameters.add(ProfileParameter.INSTANCE);
		parameters.add(SourceParameter.INSTANCE);
		parameters.add(TitleParameter.INSTANCE);
	}

	@Override
	public String getKey() {
		return CreateRecordActionTypeExecutor.KEY;
	}

	@Override
	public String getName(Locale l) {
		return Messages.AdvIntelLabelProvider_CreateActionTypeName;
	}

	@Override
	public String getDescription(Locale l) {
		return Messages.AdvIntelLabelProvider_CreateActionTypeDesc;
	}

	@Override
	public List<IActionParameter> getActionParameters() {
		return parameters;
	}

	@Override
	public void afterExecuted(Object data) {
		if (data == null) return;
		IEventBroker eventBroker = EclipseContextFactory.getServiceContext(EventPlugIn.getDefault().getBundle().getBundleContext()).get(IEventBroker.class);
		eventBroker.send(IntelEvents.RECORD_NEW, Collections.singletonList(data));
		
	}

}
