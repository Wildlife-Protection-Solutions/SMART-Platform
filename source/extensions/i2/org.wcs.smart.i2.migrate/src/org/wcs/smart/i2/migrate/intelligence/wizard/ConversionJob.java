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
package org.wcs.smart.i2.migrate.intelligence.wizard;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.migrate.MigratePlugin;
import org.wcs.smart.i2.migrate.intelligence.Intel6Database;
import org.wcs.smart.i2.migrate.intelligence.IntelMappingRecord;
import org.wcs.smart.i2.migrate.intelligence.IntelligenceItem;
import org.wcs.smart.i2.migrate.internal.Messages;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordAttachment;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.patrol.model.PatrolMotivatedRecord;
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolWaypoint;

/**
 * Job to convert SMART6 intelligence records to profile records
 * and save them to the database
 *  
 * @author Emily
 *
 */
public class ConversionJob implements IRunnableWithProgress {

	private List<IntelMappingRecord> mappings;
	private Intel6Database smart6;
	private Map<ConservationArea, Employee> userMappings;
	public ConversionJob(List<IntelMappingRecord> mappings, Intel6Database smart6, Map<ConservationArea, Employee> userMappings) {
		this.mappings = mappings;
		this.smart6 = smart6;
		this.userMappings = userMappings;
	}
	
	@Override
	public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
		
		Set<ConservationArea> cas = mappings.stream()
				.map(e->e.getConservationArea()).collect(Collectors.toSet());
		
		List<IntelRecord> thisCaNewRecords = new ArrayList<>();
		
		SubMonitor task = SubMonitor.convert(monitor);
		task.beginTask(Messages.ConversionJob_TaskName, cas.size());
		
		int added = 0;
		try(Session session = HibernateManager.openSession(new AttachmentInterceptor(false))){
			session.beginTransaction();
			try {
				for (ConservationArea ca : cas) {
					SubMonitor catask = task.split(1);
					
					Collection<IntelligenceItem> items = smart6.getIntelItems(ca);
					
					Map<UUID, List<UUID>> intelToPatrol = smart6.getMotivatedByLinks(ca);
					
					catask.beginTask(ca.getNameLabel(), items.size());
					
					for(IntelligenceItem item : items) {
						catask.split(1);
						IntelMappingRecord mapping = null;
						for (IntelMappingRecord mr : mappings) {
							if (mr.getConservationArea().equals(ca) &&
									mr.getSmart6Source().getUuid().equals(item.getSource())) {
								mapping = mr;
								break;
							}
						}
						if (mapping != null) {
							IntelRecord r = convertItem(item, mapping, session);
							
							List<UUID> patrols = intelToPatrol.get(item.getUuid());
							for (UUID patrol : patrols) {
								Patrol p = session.get(Patrol.class, patrol);
								if (p != null) {
									PatrolMotivatedRecord mr = new PatrolMotivatedRecord();
									mr.getId().setIntelRecord(r);
									mr.getId().setPatrol(p);
									session.save(mr);
								}
								
							}
							
							
							added ++;
							if (ca.equals(SmartDB.getCurrentConservationArea())) {
								thisCaNewRecords.add(r);
							}
						}
					}
				
				}
				
				session.getTransaction().commit();
			}catch (Exception ex) {
				session.getTransaction().rollback();
				throw new InvocationTargetException(ex);
			}
		}

		IEventBroker eventBroker = EclipseContextFactory.getServiceContext(MigratePlugin.getDefault().getBundle().getBundleContext()).get(IEventBroker.class);
		eventBroker.send(IntelEvents.RECORD_NEW, thisCaNewRecords);
		
		final int fadded = added;
		Display.getDefault().syncExec(()->{
			MessageDialog.openInformation(Display.getDefault().getActiveShell(), Messages.ConversionJob_CompleteTitle, MessageFormat.format(Messages.ConversionJob_CompleteMsg, fadded));
		});
	}
	
	private IntelRecord convertItem(IntelligenceItem item, IntelMappingRecord mapping, Session session) {
		
		IntelRecord record = new IntelRecord();
		record.setConservationArea(mapping.getConservationArea());
		record.setRecordSource(mapping.getRecordSource());
		record.setProfile(mapping.getProfile());
		record.setStatus(IntelRecord.Status.NEW);
		
		record.setTitle(item.getName());
		record.setDescription(item.getDescription());
		record.setComment(Messages.ConversionJob_ProfileRecordComment);
		
		record.setPrimaryDate(item.getRecievedDate().atStartOfDay());
		
		if (item.getCreator() != null) {
			Employee e = session.get(Employee.class, item.getCreator());
			if (e != null && e.getConservationArea().equals(record.getConservationArea())) {
				record.setCreatedBy(e);
				record.setLastModifiedBy(e);
			}
		}
		if (record.getCreatedBy() == null) {
			record.setCreatedBy(userMappings.get(record.getConservationArea()));
			record.setLastModifiedBy(userMappings.get(record.getConservationArea()));
		}
		
		record.setDateCreated(LocalDateTime.now());
		record.setDateModified(LocalDateTime.now());
		
		record.setAttachments(new ArrayList<>());
		record.setLocations(new ArrayList<>());
		int cnt = 1;
		for (Coordinate c : item.getPoints()) {
			IntelLocation location = new IntelLocation();
			location.setConservationArea(record.getConservationArea());
			location.setId(String.valueOf(cnt++));
			location.setGeometry(GeometryFactoryProvider.getFactory().createPoint(c));
			location.setRecord(record);
			location.setDateTime(record.getPrimaryDate());
			record.getLocations().add(location);
		}
		
		record.setAttributes(new ArrayList<>());
		if (mapping.getToDateMapping() != null && item.getToDate() != null) {
			IntelRecordAttributeValue value = new IntelRecordAttributeValue();
			value.setAttribute(mapping.getToDateMapping());
			value.setDateValue(item.getToDate());
			value.setRecord(record);
			record.getAttributes().add(value);
		}
		if (mapping.getFromDateMapping() != null && item.getRecievedDate() != null) {
			if (!mapping.getToDateMapping().equals(mapping.getFromDateMapping())) {
				IntelRecordAttributeValue value = new IntelRecordAttributeValue();
				value.setAttribute(mapping.getFromDateMapping());
				value.setDateValue(item.getToDate());
				value.setRecord(record);
				record.getAttributes().add(value);
			}
		}
		
		if (item.getPatroluuid() != null) {
			Patrol p = session.get(Patrol.class, item.getPatroluuid());
			if (p != null) {
				//link to first waypoint in patrol
				List<PatrolWaypoint> ps = p.getFirstLeg().getPatrolLegDays().get(0).getWaypoints();
				if (ps.size() > 0) {
					record.setSmartSource(ps.get(0).getWaypoint());
				}
			}
		}
		
		session.save(record);
		
		
		for (Path p : item.getAttachments()) {
			IntelAttachment ia = new IntelAttachment();
			ia.setConservationArea(mapping.getConservationArea());
			ia.setCopyFromLocation(p);
			ia.setFilename(p.getFileName().toString());
			ia.setDateCreated(LocalDateTime.now());
			ia.setCreatedBy(record.getCreatedBy());
			session.save(ia);
			
			IntelRecordAttachment recordattachment = new IntelRecordAttachment();
			recordattachment.setAttachment(ia);
			recordattachment.setRecord(record);
			record.getAttachments().add(recordattachment);
		}
		session.flush();
		return record;
		
	}

}
