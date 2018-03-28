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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.event.EventPlugIn;
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
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Create new profile record action type
 * 
 * @author Emily
 *
 */
public class CreateRecordActionType implements IActionType {

	public static final String KEY = "org.wcs.smart.profile.newrecord"; //$NON-NLS-1$

	public static final String NAME = CreateRecordActionType.class.getName() + ".name"; //$NON-NLS-1$
	public static final String DESCRIPTION = CreateRecordActionType.class.getName() + ".description"; //$NON-NLS-1$
	public static final String MESSAGE = CreateRecordActionType.class.getName() + ".msg"; //$NON-NLS-1$
	public static final String WP_SOURCE = CreateRecordActionType.class.getName() + ".msg"; //$NON-NLS-1$
	public static final String WP_DATE = CreateRecordActionType.class.getName() + ".msg"; //$NON-NLS-1$
	public static final String WP_CMT = CreateRecordActionType.class.getName() + ".msg"; //$NON-NLS-1$
	public static final String WP_OBS = CreateRecordActionType.class.getName() + ".msg"; //$NON-NLS-1$
	
	
	private static Logger logger = Logger.getLogger(CreateRecordActionType.class.getCanonicalName());

	private List<IActionParameter> parameters;
	
	public CreateRecordActionType() {
		parameters = new ArrayList<>();
		parameters.add(SourceParameter.INSTANCE);
		parameters.add(TitleParameter.INSTANCE);
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getName(Locale l) {
		return SmartContext.INSTANCE.getClass(IAdvIntelLabelProvider.class).getLabel(NAME, l);
	}

	@Override
	public String getDescription(Locale l) {
		return SmartContext.INSTANCE.getClass(IAdvIntelLabelProvider.class).getLabel(DESCRIPTION, l);
	}

	@Override
	public List<IActionParameter> getActionParameters() {
		return parameters;
	}

	@Override
	public void performAction(EAction action, EFilter filter, WaypointObservation data, Locale l) {
		//create a new intelligence record
		
		StringBuilder sb = new StringBuilder();
		sb.append(MessageFormat.format( SmartContext.INSTANCE.getClass(IAdvIntelLabelProvider.class).getLabel(MESSAGE, l), action.getId(), filter.getId()));
		sb.append("\n\n"); //$NON-NLS-1$
		sb.append(MessageFormat.format(SmartContext.INSTANCE.getClass(IAdvIntelLabelProvider.class).getLabel(WP_SOURCE, l), data.getWaypoint().getSourceId()));
		sb.append("\n"); //$NON-NLS-1$
		sb.append(MessageFormat.format(SmartContext.INSTANCE.getClass(IAdvIntelLabelProvider.class).getLabel(WP_DATE, l), (new SimpleDateFormat("MMM dd, yyyy HH:mm:ss")).format(data.getWaypoint().getDateTime()))); //$NON-NLS-1$
		sb.append("\n"); //$NON-NLS-1$
		sb.append(MessageFormat.format(SmartContext.INSTANCE.getClass(IAdvIntelLabelProvider.class).getLabel(WP_CMT, l), data.getWaypoint().getComment()));
		sb.append("\n"); //$NON-NLS-1$
		sb.append(MessageFormat.format(SmartContext.INSTANCE.getClass(IAdvIntelLabelProvider.class).getLabel(WP_OBS, l), data.getCategory().getName()));
		sb.append("\n"); //$NON-NLS-1$
		for (WaypointObservationAttribute a : data.getAttributes()) {
			sb.append(MessageFormat.format("{0}: {1}", a.getAttribute().getName(), a.getAttributeValueAsString(Locale.getDefault()))); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
		}
		
		IntelRecord newRecord = new IntelRecord();
		newRecord.setConservationArea(data.getWaypoint().getConservationArea());
		newRecord.setAttributes(new ArrayList<>());
		newRecord.setDescription(sb.toString());
		newRecord.setComment(""); //$NON-NLS-1$
		newRecord.setLocations(new ArrayList<>());
		newRecord.setPrimaryDate(data.getWaypoint().getDateTime());
		newRecord.setStatus(IntelRecord.Status.NEW);
		
		EActionParameterValue sourceParam = action.findParameter(SourceParameter.INSTANCE.getKey());
		EActionParameterValue titleParam = action.findParameter(TitleParameter.INSTANCE.getKey());
		if (titleParam != null) {
			newRecord.setTitle(titleParam.getParameterValue());
		}
		newRecord.setAttachments(new ArrayList<>());
		
		IntelLocation location = new IntelLocation();
		location.setConservationArea(newRecord.getConservationArea());
		location.setComment(data.getWaypoint().getComment());
		location.setDateTime(data.getWaypoint().getDateTime());
		location.setId(String.valueOf( data.getWaypoint().getId()));
		location.setRecord(newRecord);
		location.setGeometry(GeometryFactoryProvider.getFactory().createPoint(new Coordinate(data.getWaypoint().getX(),data.getWaypoint().getY())));
		newRecord.getLocations().add(location);

		location.setObservations(new ArrayList<>());
		
		IntelObservation io = new IntelObservation();
		io.setCategory(data.getCategory());
		io.setObservationAttributes(new ArrayList<>());
		io.setLocation(location);
		for (WaypointObservationAttribute aa : data.getAttributes()) {
			IntelObservationAttribute cloneAttribute = new IntelObservationAttribute();
			cloneAttribute.setAttribute(aa.getAttribute());
			cloneAttribute.setAttributeListItem(aa.getAttributeListItem());
			cloneAttribute.setAttributeTreeNode(aa.getAttributeTreeNode());
			cloneAttribute.setNumberValue(aa.getNumberValue());
			cloneAttribute.setStringValue(aa.getStringValue());
			cloneAttribute.setObservation(io);
			io.getObservationAttributes().add(cloneAttribute);
		}
		location.getObservations().add(io);
		
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor(false))){
			if(data.getAttachments() != null) {
				for (ObservationAttachment a : data.getAttachments()) {
					try {
						a.computeFileLocation(session);
					} catch (Exception e) {
						logger.log(Level.WARNING, "Unable to compute file location for attachment, file will no be imported into new intelligence record: " + e.getMessage(), e); //$NON-NLS-1$
						continue;
					}
	
					IntelAttachment attachment = new IntelAttachment();
					attachment.setConservationArea(newRecord.getConservationArea());
					attachment.setFilename(a.getFilename());
					attachment.setCopyFromLocation(a.getAttachmentFile());
					attachment.setDateCreated(new Date());
					attachment.setCreatedBy(SmartDB.getCurrentEmployee());
					
					IntelRecordAttachment rattachment = new IntelRecordAttachment();
					rattachment.setAttachment(attachment);
					rattachment.setRecord(newRecord);
					rattachment.setAttachment(attachment);
					
					newRecord.getAttachments().add(rattachment);
				}
			}
			if (data.getWaypoint().getAttachments() != null) {
				for (WaypointAttachment a : data.getWaypoint().getAttachments()) {
					try {
						a.computeFileLocation(session);
					} catch (Exception e) {
						logger.log(Level.WARNING, "Unable to compute file location for attachment, file will no be imported into new intelligence record: " + e.getMessage(), e); //$NON-NLS-1$
						continue;
					}
	
					IntelAttachment attachment = new IntelAttachment();
					attachment.setConservationArea(newRecord.getConservationArea());
					attachment.setFilename(a.getFilename());
					attachment.setCopyFromLocation(a.getAttachmentFile());
					attachment.setDateCreated(new Date());
					attachment.setCreatedBy(SmartDB.getCurrentEmployee());
					
					IntelRecordAttachment rattachment = new IntelRecordAttachment();
					rattachment.setAttachment(attachment);
					rattachment.setRecord(newRecord);
					rattachment.setAttachment(attachment);
					
					newRecord.getAttachments().add(rattachment);
				}
			}
			session.beginTransaction();
			try {
				if (sourceParam != null) {
					IntelRecordSource source = QueryFactory.buildQuery(session, IntelRecordSource.class, 
						new Object[] {"conservationArea", newRecord.getConservationArea()}, //$NON-NLS-1$
						new Object[] {"keyId", sourceParam.getParameterValue()}).uniqueResult(); //$NON-NLS-1$
					newRecord.setRecordSource(source);
				}
				//create a unquie title
				int uniqueNumber = 0;
				while(true) {
					String title = newRecord.getTitle();
					if (uniqueNumber > 0) title = title + " " + uniqueNumber; //$NON-NLS-1$
					Long cnt = QueryFactory.buildCountQuery(session, IntelRecord.class,
							new Object[] {"conservationArea", newRecord.getConservationArea()}, //$NON-NLS-1$
							new Object[] {"title", title}); //$NON-NLS-1$
				
					if (cnt == 0) {
						break;
					}
					uniqueNumber++;
				}
				if (uniqueNumber > 0) {
					newRecord.setTitle(newRecord.getTitle() + " " + uniqueNumber); //$NON-NLS-1$
				}
				
				newRecord.getAttachments().forEach(aa->{
					session.save(aa.getAttachment());
				});
				session.save(newRecord);
				session.getTransaction().commit();
			}catch(Exception ex) {
				EventPlugIn.displayLog(ex.getMessage(), ex);
				return;
			}
		}
		IEventBroker eventBroker = EclipseContextFactory.getServiceContext(EventPlugIn.getDefault().getBundle().getBundleContext()).get(IEventBroker.class);
		eventBroker.send(IntelEvents.RECORD_NEW, Collections.singletonList(newRecord));
		
	}

}
