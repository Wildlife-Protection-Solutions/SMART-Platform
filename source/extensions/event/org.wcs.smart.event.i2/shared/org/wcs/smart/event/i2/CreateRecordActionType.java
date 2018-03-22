package org.wcs.smart.event.i2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.hibernate.Session;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.attachment.AttachmentUtil;
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

public class CreateRecordActionType implements IActionType {

	public static final String KEY = "org.wcs.smart.profile.newrecord";
	
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
		return "Create Profile Record";
	}

	@Override
	public String getDescription(Locale l) {
		return "Creates a new record in the profiles module with the provided observation details";
	}

	@Override
	public List<IActionParameter> getActionParameters() {
		return parameters;
	}

	@Override
	public void performAction(EAction action, EFilter filter, WaypointObservation data) {
		//create a new intelligence record
		
		StringBuilder sb = new StringBuilder();
		sb.append(MessageFormat.format("Record automatically created by event system (Action: {0}; Filter: {1})", action.getId(), filter.getId()));
		sb.append("\n\n");
		sb.append(MessageFormat.format("Waypoint Source: {0}", data.getWaypoint().getSourceId()));
		sb.append("\n");
		sb.append(MessageFormat.format("Waypoint Date: {0}", (new SimpleDateFormat("MMM dd, yyyy HH:mm:ss")).format(data.getWaypoint().getDateTime())));
		sb.append("\n");
		sb.append(MessageFormat.format("Waypoint Comment: {0}", data.getWaypoint().getComment()));
		sb.append("\n");
		sb.append(MessageFormat.format("Observation: {0}", data.getCategory().getName()));
		sb.append("\n");
		for (WaypointObservationAttribute a : data.getAttributes()) {
			sb.append(MessageFormat.format("{0}: {1}", a.getAttribute().getName(), a.getAttributeValueAsString(Locale.getDefault())));
			sb.append("\n");
		}
		
		IntelRecord newRecord = new IntelRecord();
		newRecord.setConservationArea(data.getWaypoint().getConservationArea());
		newRecord.setAttributes(new ArrayList<>());
		newRecord.setDescription(sb.toString());
		newRecord.setComment("");
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
						EventPlugIn.log("Unable to compute file location for attachment, file will no be imported into new intelligence record: " + e.getMessage(), e);
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
						EventPlugIn.log("Unable to compute file location for attachment, file will no be imported into new intelligence record: " + e.getMessage(), e);
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
						new Object[] {"conservationArea", newRecord.getConservationArea()},
						new Object[] {"keyId", sourceParam.getParameterValue()}).uniqueResult();
					newRecord.setRecordSource(source);
				}
				//create a unquie title
				int uniqueNumber = 0;
				while(true) {
					String title = newRecord.getTitle();
					if (uniqueNumber > 0) title = title + " " + uniqueNumber;
					Long cnt = QueryFactory.buildCountQuery(session, IntelRecord.class, new Object[] {"conservationArea", newRecord.getConservationArea()},
							new Object[] {"title", title});
				
					if (cnt == 0) {
						break;
					}
					uniqueNumber++;
				}
				if (uniqueNumber > 0) {
					newRecord.setTitle(newRecord.getTitle() + " " + uniqueNumber);
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
