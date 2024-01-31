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
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.event.i2.IProfileEventLabelProvider.EventMessages;
import org.wcs.smart.event.i2.entity.ParameterKeys;
import org.wcs.smart.event.model.EAction;
import org.wcs.smart.event.model.EActionParameterValue;
import org.wcs.smart.event.model.EFilter;
import org.wcs.smart.event.model.IActionTypeExecutor;
import org.wcs.smart.hibernate.QueryFactory;
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
public class CreateRecordActionTypeExecutor implements IActionTypeExecutor {

	public static final String KEY = "org.wcs.smart.profile.newrecord"; //$NON-NLS-1$
	
	private static Logger logger = Logger.getLogger(CreateRecordActionTypeExecutor.class.getCanonicalName());


	@Override
	public String getKey() {
		return KEY;
	}
	
	private String getMessage(IProfileEventLabelProvider.EventMessages message, Locale l) {
		return SmartContext.INSTANCE.getClass(IProfileEventLabelProvider.class).getLabel(message, l);
	}

	@Override
	public Object performAction(EAction action, EFilter filter, WaypointObservation data, Locale l,
			Employee employee, Session session) {
		//create a new intelligence record
		
		IntelRecord newRecord = new IntelRecord();
		newRecord.setConservationArea(data.getWaypoint().getConservationArea());
		newRecord.setAttributes(new ArrayList<>());
		newRecord.setComment(""); //$NON-NLS-1$
		newRecord.setLocations(new ArrayList<>());
		newRecord.setPrimaryDate(data.getWaypoint().getDateTime());
		newRecord.setStatus(IntelRecord.Status.NEW);
		
			data = session.get(WaypointObservation.class, data.getUuid());
			
			StringBuilder sb = new StringBuilder();
			sb.append(MessageFormat.format(getMessage(EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg1, l), action.getId(), filter.getId()));
			sb.append("\n\n"); //$NON-NLS-1$
			sb.append(MessageFormat.format(getMessage(EventMessages.CreateRecordActionType_WaypointIdLabel, l), data.getWaypoint().getId()));
			sb.append("\n"); //$NON-NLS-1$
			sb.append(MessageFormat.format(getMessage(EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg2, l), data.getWaypoint().getSourceId()));
			sb.append("\n"); //$NON-NLS-1$
			sb.append(MessageFormat.format(getMessage(EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg3, l), (DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")).format(data.getWaypoint().getDateTime()))); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append(MessageFormat.format(getMessage(EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg4, l), data.getWaypoint().getComment() == null ? "" : data.getWaypoint().getComment())); //$NON-NLS-1$
			sb.append("\n"); //$NON-NLS-1$
			sb.append(MessageFormat.format(getMessage(EventMessages.AdvIntelLabelProvider_CreateActionTypeMsg5, l), data.getCategory().getName()));
			sb.append("\n"); //$NON-NLS-1$
			for (WaypointObservationAttribute a : data.getAttributesSorted()) {
				sb.append(MessageFormat.format("{0}: {1}", a.getAttribute().getName(), a.getAttributeValueAsString(Locale.getDefault()))); //$NON-NLS-1$
				sb.append("\n"); //$NON-NLS-1$
			}
			
			newRecord.setDescription(sb.toString());
			
			EActionParameterValue profileParam = action.findParameter(ParameterKeys.PROFILE_PARAM_KEY);
			if (profileParam == null || profileParam.getParameterValue().isEmpty()) {
				throw new RuntimeException(getMessage(EventMessages.CreateRecordActionType_ProfileParameterNotSet, l));
			}
			EActionParameterValue sourceParam = action.findParameter(ParameterKeys.RECORD_SOURCE_PARAM_KEY);
			EActionParameterValue titleParam = action.findParameter(ParameterKeys.RECORD_TITLE_PARAM_KEY);
			if (titleParam != null) {
				newRecord.setTitle(titleParam.getParameterValue());
			}else {
				newRecord.setTitle(""); //$NON-NLS-1$
			}
			newRecord.setAttachments(new ArrayList<>());
			
			IntelLocation location = new IntelLocation();
			location.setConservationArea(newRecord.getConservationArea());
			location.setComment(data.getWaypoint().getComment());
			location.setDateTime(data.getWaypoint().getDateTime());
			location.setId( data.getWaypoint().getId() );
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
				cloneAttribute.setGeom(aa.getGeom());
				cloneAttribute.setNumberValue2(aa.getNumberValue());
				cloneAttribute.setStringValue(aa.getStringValue());
				cloneAttribute.setObservation(io);
				
				if (aa.getAttributeListItems() != null) {
					cloneAttribute.setAttributeListItems(new ArrayList<>());
					for (WaypointObservationAttributeList al : aa.getAttributeListItems()) {
						IntelObservationAttributeList il = new IntelObservationAttributeList();
						il.setAttributeLisItem(al.getAttributeListItem());
						il.setObservationAttribute(cloneAttribute);
						cloneAttribute.getAttributeListItems().add(il);
					}
				}
				io.getObservationAttributes().add(cloneAttribute);
			}
			location.getObservations().add(io);
		
			
			String keyid = profileParam.getParameterValue();
			IntelProfile ip = QueryFactory.buildQuery(session, IntelProfile.class, 
					new Object[] {"keyId", keyid}, //$NON-NLS-1$
					new Object[] {"conservationArea", data.getWaypoint().getConservationArea()}).uniqueResult(); //$NON-NLS-1$
			
			if (ip == null) throw new RuntimeException(getMessage(EventMessages.CreateRecordActionType_ProfileNotFound, l));
			
			newRecord.setProfile(ip);
			newRecord.setSmartSource(data.getWaypoint());
			
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
					attachment.setDateCreated(LocalDateTime.now());
					attachment.setCreatedBy(employee);
					
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
					attachment.setDateCreated(LocalDateTime.now());
					attachment.setCreatedBy(employee);
					
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
					
					//ensure profile is valid for record source
					boolean ok = false;
					for (IntelProfileRecordSource irs : source.getProfiles()) {
						if (irs.getProfile().equals(ip)) {
							ok = true;
						}
					}
					if (!ok) {
						throw new RuntimeException(MessageFormat.format(getMessage(EventMessages.CreateRecordActionType_InvalidProfile, l), ip.getName(), source.getName()));
					}
					
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
					session.persist(aa.getAttachment());
				});
				session.persist(newRecord);
				session.getTransaction().commit();
			}catch(Exception ex) {
				logger.log(Level.SEVERE, ex.getMessage(), ex);
				return null;
			}
		
		return newRecord;
		
	}


}
