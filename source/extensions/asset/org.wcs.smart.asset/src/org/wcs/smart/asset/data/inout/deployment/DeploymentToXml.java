/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.asset.data.inout.deployment;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.data.inout.deployment.xml.v10.ObjectFactory;
import org.wcs.smart.asset.data.inout.deployment.xml.v10.XmlAssetDeployment;
import org.wcs.smart.asset.data.inout.deployment.xml.v10.XmlAssetDeploymentAttribute;
import org.wcs.smart.asset.data.inout.deployment.xml.v10.XmlAssetDeploymentDisruption;
import org.wcs.smart.asset.data.inout.deployment.xml.v10.XmlWaypoint;
import org.wcs.smart.asset.data.inout.deployment.xml.v10.XmlWaypointObservation;
import org.wcs.smart.asset.data.inout.deployment.xml.v10.XmlWaypointObservationAttribute;
import org.wcs.smart.asset.data.inout.deployment.xml.v10.XmlWaypointObservationGroup;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetDeploymentAttributeValue;
import org.wcs.smart.asset.model.AssetDeploymentDisruption;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.model.AssetWaypointAttachment;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.observation.model.ObservationAttachment;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.ZipUtil;

/**
 * Exports asset deployment, waypoints, and zip files.
 * @author Emily
 *
 */
public class DeploymentToXml {

	public static final String XML_FILE = "asset_deployment.xml"; //$NON-NLS-1$
	public static final String ATTCHMENT_DIR = "attachments"; //$NON-NLS-1$
	
	public void writeDeployment(AssetDeployment deployment, Session session, Path zipFile, IProgressMonitor monitor) throws Exception{
		
		Path xmlFile = null;
		//write xml file
		try {
			
			SubMonitor task = SubMonitor.convert(monitor, 4);
			
			task.split(1);
			task.subTask(Messages.DeploymentToXml_convertTask);
			XmlAssetDeployment xml = convert(deployment);
			
			task.subTask(Messages.DeploymentToXml_writingTask);
			task.split(1);
			xmlFile = Files.createTempFile("deployment", ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
			writeXmlFile(xmlFile, xml);
			
			//copy attachments
			task.subTask(Messages.DeploymentToXml_attachmentsTask);
			task.split(1);
			List<ISmartAttachment> allAttach = new ArrayList<ISmartAttachment>();
			for (AssetWaypoint aw : deployment.getAssetWaypoints()) {
				for (AssetWaypointAttachment a : aw.getAttachments()) {
					allAttach.add(a.getWaypointAttachment());
				}
				
				for (WaypointObservation wo : aw.getWaypoint().getAllObservations()) {
					allAttach.addAll(wo.getAttachments());
				}
			}
			//create zip file
			
			task.subTask(Messages.DeploymentToXml_zippingTask);
			task.setWorkRemaining(allAttach.size() + 1);
			
			try(ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(zipFile))) {
				zout.setLevel(Deflater.DEFAULT_COMPRESSION);
	
				/* add xml file to zip */
				task.split(1);
				ZipEntry zipEntry = new ZipEntry(XML_FILE);
				zout.putNextEntry(zipEntry);
				try (InputStream in = Files.newInputStream(xmlFile)) {
					zout.write(in.readAllBytes());
				}
				zout.closeEntry();
		        
				/* add all attachments */
				for (ISmartAttachment att : allAttach){
					task.split(1);
					zout.putNextEntry(new ZipEntry(ATTCHMENT_DIR + ZipUtil.DIR_PATH_SEPERATOR + att.getFilename()));
					try {
						att.computeFileLocation(session);
						EncryptUtils.decryptAttachment(att, zout);
					}catch (Exception ex) {
						//unable to decrypt file; option to include encrypted version
						AssetPlugIn.log(ex.getMessage(), ex);
					}
				}
			}
		}finally{
			try{
				//delete temp file
				if (xmlFile != null && Files.exists(xmlFile)) Files.delete(xmlFile);
			}catch(Exception ex){
				AssetPlugIn.log(ex.getMessage(),ex);
			}
		}
	}
	
	private void writeXmlFile(Path xmlFile, XmlAssetDeployment deployment) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		ObjectFactory objFactor = new ObjectFactory();
		JAXBElement<XmlAssetDeployment> element = objFactor.createDeployment(deployment);
		marshaller.marshal(element, xmlFile.toAbsolutePath().normalize().toFile());
	}
	
	
	private XmlAssetDeployment convert(AssetDeployment deployment) throws DatatypeConfigurationException {
			
		XmlAssetDeployment xml = new XmlAssetDeployment();
		xml.setAssetKey(deployment.getAsset().getId());
		xml.setAssetTypeKey(deployment.getAsset().getAssetType().getKeyId());
		
		xml.setStartDateTime( convertDateTime(deployment.getStartDate()) );
		if (deployment.getEndDate() != null) {
			xml.setEndDateTime( convertDateTime(deployment.getEndDate()) );
		}
		
		xml.setStationId(deployment.getStationLocation().getStation().getId());
		xml.setLocationId(deployment.getStationLocation().getId());
		
		
		for (AssetDeploymentAttributeValue value : deployment.getAttributeValues()) {
			XmlAssetDeploymentAttribute xmla = new XmlAssetDeploymentAttribute();
			xmla.setAttributeKey(value.getAttribute().getKeyId());
			xmla.setAttributeTypeKey(value.getAttribute().getType().name());
			
			xmla.setDoubleValue1(value.getNumberValue());
			xmla.setDoubleValue2(value.getNumberValue2());
			xmla.setStringValue(value.getStringValue());
			
			if (value.getAttributeListItem() != null) {
				xmla.setStringValue(value.getAttributeListItem().getKeyId());
			}
			
			xml.getAttributes().add(xmla);
		}
		
		for (AssetDeploymentDisruption disruption : deployment.getDisruptions()) {
			XmlAssetDeploymentDisruption xmld = new XmlAssetDeploymentDisruption();
			xmld.setComment(disruption.getComment());
			xmld.setStartDateTime( convertDateTime(disruption.getStartDate()) );
			xmld.setEndDateTime( convertDateTime(disruption.getEndDate()) );
			
			xml.getDisruptions().add(xmld);
		}
		
		for (AssetWaypoint aw : deployment.getAssetWaypoints()) {
			XmlWaypoint xmlw = new XmlWaypoint();
			xmlw.setState(aw.getState().name());
			xmlw.setId(aw.getWaypoint().getId());
			xmlw.setComment(aw.getWaypoint().getComment());
			xmlw.setDateTime( SmartUtils.toXmlDateTime( aw.getWaypoint().getDateTime()));
			xmlw.setLength(aw.getIncidentLength());
			
			xmlw.setX(aw.getWaypoint().getRawX());
			xmlw.setY(aw.getWaypoint().getRawY());
			
			xml.getWaypoints().add(xmlw);
			
			for (AssetWaypointAttachment awa : aw.getAttachments()) {
				xmlw.getAttachments().add(awa.getWaypointAttachment().getFilename());
			}
			
			for(WaypointObservationGroup group : aw.getWaypoint().getObservationGroups()) {
				XmlWaypointObservationGroup xmlg = new XmlWaypointObservationGroup();
				
				for (WaypointObservation wo : group.getObservations()) {
					XmlWaypointObservation xmlwo =  new XmlWaypointObservation();
					
					xmlwo.setCategoryKey(wo.getCategory().getHkey());
					if (wo.getObserver() != null) xmlwo.setObserverId(wo.getObserver().getId());
					
					for (WaypointObservationAttribute woa : wo.getAttributes()) {
						
						XmlWaypointObservationAttribute xmlaa = new XmlWaypointObservationAttribute();
						xmlaa.setAttributeKey(woa.getAttribute().getKeyId());
						xmlaa.setAttributeType(woa.getAttribute().getType().name());
						
						switch(woa.getAttribute().getType()) {
						case BOOLEAN:
						case NUMERIC:
							xmlaa.setDoubleValue(woa.getNumberValue());
							break;
						case DATE:
						case TEXT:
							xmlaa.getStringValue().add(woa.getStringValue());
							break;
						case LIST:
							if (woa.getAttributeListItem() != null) xmlaa.getStringValue().add(woa.getAttributeListItem().getKeyId());
							break;
						case MLIST:
							if (woa.getAttributeListItems() != null) woa.getAttributeListItems().forEach(e->xmlaa.getStringValue().add(e.getAttributeListItem().getKeyId()));
							break;
						case TREE:
							if (woa.getAttributeTreeNode() != null) xmlaa.getStringValue().add(woa.getAttributeTreeNode().getHkey());
							break;
						}
						
					
						xmlwo.getAttributes().add(xmlaa);
					}
					
					for (ObservationAttachment oa : wo.getAttachments()) {
						xmlwo.getAttachments().add(oa.getFilename());
					}
					xmlg.getObservations().add(xmlwo);
				}
				
				xmlw.getObservations().add(xmlg);
			}
			
		}
		
		return xml;
	}
	
	
	
	private XMLGregorianCalendar convertDateTime(LocalDateTime datetime) throws DatatypeConfigurationException {
		GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
		cal.set(datetime.getYear(),  datetime.getMonthValue()-1, datetime.getDayOfMonth(),  datetime.getHour(), datetime.getMinute(), datetime.getSecond());
		
		XMLGregorianCalendar xgc = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
//		xgc.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
		xgc.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
//		xgc.setYear(DatatypeConstants.FIELD_UNDEFINED);
//		xgc.setMonth(DatatypeConstants.FIELD_UNDEFINED);
//		xgc.setDay(DatatypeConstants.FIELD_UNDEFINED);
		
		
		return xgc;
	}
	
	
	
}
