/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.asset.data.importer;

import java.nio.file.Path;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.asset.IAssetLabelProvider;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetMetadataMapping;
import org.wcs.smart.asset.model.AssetMetadataMapping.State;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.AssetWaypoint;
import org.wcs.smart.asset.model.mapping.ExifMetadataField;
import org.wcs.smart.asset.model.mapping.XmpMetadataField;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;

/**
 * Asset file process or processing a collection of files.
 * 
 * @author Emily
 *
 */
public class FileProcessor {

	private static final String EXIF_METADATA_KEY = "EXIF_METADATA"; //$NON-NLS-1$

	public AtomicInteger NewObjectCounter = new AtomicInteger();
	
	private List<FileProxy> files;	
	private ConservationArea ca;
	private Locale locale;
	
	public enum ErrorMessages{
		METADATA_PARSE,
		CATEGORY_NOT_FOUND,
		BOOLEAN_PARSE_ERROR,
		DATE_PARSE_ERROR,
		LIST_ITEM_PARSE_ERROR,
		TREE_NODE_PARSE_ERROR,
		NUMBER_PARSE_ERROR,
		
		BOOLEAN_TAG_PARSE_ERROR,
		DATE_TAG_PARSE_ERROR,
		LIST_ITEM_TAG_PARSE_ERROR,
		TREE_NODE_TAG_PARSE_ERROR,
		NUMBER_TAG_PARSE_ERROR,
		
		ASSET_NOT_FOUND,
		STATION_OVERWRITE,
		STATION_LOCATION_NOT_FOUND,
		STATION_NOT_FOUND,
		MULTIPLE_DEPLOYMENTS;

		public String getMessage(Locale l){
			return SmartContext.INSTANCE.getClass(IAssetLabelProvider.class).getLabel(this, l);
		}
	}
	
	public FileProcessor(ConservationArea ca, Locale l) {
		this.ca = ca;
		this.locale = l;
		this.files = new ArrayList<>();
	}
	
	public Locale getLocale() {
		return this.locale;
	}
	
	public void addFile(Path file, IConnectionProvider provider) {
		for (FileProxy p : files) {
			if (p.getFile().equals(file)) return;
			//file already exists return null
		}
		List<FileProxy> addedProxies = new ArrayList<>();
		try {
			FileProxy proxy = FileMetadataReader.readFile(file, ca);
			try(Session session = provider.openSession()){

				//check for potential duplicate data
				StringBuilder sb = new StringBuilder();
				sb.append("SELECT count(*) FROM "); //$NON-NLS-1$
				sb.append("AssetWaypointAttachment a join a.id.waypointAttachment b "); //$NON-NLS-1$
				sb.append(" join b.waypoint c " ); //$NON-NLS-1$
				sb.append(" WHERE c.conservationArea = :ca AND "); //$NON-NLS-1$
				sb.append(" b.filename = :filename "); //$NON-NLS-1$
				
				Long fileCnt = (Long)session.createQuery(sb.toString())
				.setParameter("ca", ca) //$NON-NLS-1$
				.setParameter("filename", proxy.getFilename()) //$NON-NLS-1$
				.uniqueResult();
				
				if (fileCnt > 0) {
					proxy.addWarning(new DuplicateFileWarning(proxy.getFilename(), locale));
				}
				
				processMetadata(proxy, session);
				proxy.updateStationLocation(session, this);
			}
			files.add(proxy);
			addedProxies.add(proxy);
		}catch (Exception ex) {
			ex.printStackTrace();
			FileProxy p = new FileProxy(file, ca);
			files.add(p);
			p.addWarning(new ActionableWarning(MessageFormat.format(ErrorMessages.METADATA_PARSE.getMessage(locale), ex.getMessage())));
		}
		addedProxies.forEach(p->computeRelations(p));
		computeWaypoints();
		
		//sort on waypoint id
		files.sort((a,b)->{
			if (a.getIncidentGroup() == null && b.getIncidentGroup() == null) return 0;
			if (a.getIncidentGroup() == null) return -1;
			if (b.getIncidentGroup() == null) return 1;
			return a.getIncidentGroup().compareTo(b.getIncidentGroup());
		});
	}
	
	public void updateWaypointAndSort() {
		computeWaypoints();

		//sort on waypoint id
		files.sort((a,b)->{
			if (a.getIncidentGroup() == null && b.getIncidentGroup() == null) return 0;
			if (a.getIncidentGroup() == null) return -1;
			if (b.getIncidentGroup() == null) return 1;
			return a.getIncidentGroup().compareTo(b.getIncidentGroup());
		});
	}
	public void update() {
		files.forEach(p->p.getRelations().clear());
		files.forEach(p->computeRelations(p));
		updateWaypointAndSort();
	}
	
	private void computeWaypoints() {
		files.forEach(p->p.setIncidentGroup(null));
		int wpCnt = 1;
		for (FileProxy p : files) {
			if (p.setIncidentGroup(wpCnt)) {
				wpCnt ++;
			}
		}
	}
	
	/**
	 * file details sorted by date
	 * @return
	 */
	public List<FileProxy> getFiles() {
		return files;
	}
	
	public void removeFile(FileProxy file) {
		files.remove(file);
		
		for (FileProxy relation : file.getRelations()) {
			relation.getRelations().remove(file);
		}
	}
	
	
	public boolean isValid() {
		for (FileProxy proxy : files) {
			if (!proxy.isValid()) return false;
		}
		return true;
	}
	
	private void processMetadata(FileProxy p, Session session) throws Exception {
		List<AssetMetadataMapping> mappings = QueryFactory.buildQuery(session, AssetMetadataMapping.class,
				new Object[] {"conservationArea", ca}).list(); //$NON-NLS-1$
		mappings.sort((a,b)->a.getSearchOrder().compareTo(b.getSearchOrder()));
		
		for (AssetMetadataMapping m : mappings) {
			if (m.getMappedCategory() != null) m.getMappedCategory().getFullCategoryName();
			if (m.getMappedAttribute() != null) m.getMappedAttribute().getName();
			if (m.getMappedListItem() != null) m.getMappedListItem().getName();
			if (m.getMappedTreeNode() != null) m.getMappedTreeNode().getName();
		}
		//process mappings in order
		for (AssetMetadataMapping mapping : mappings) {
			if (mapping.getState() == State.DISABLED) continue;
			processMetadata(p, mapping, session);
		}
		
		//process observations
		mergeObservations(p);
	}
	
	private void computeRelations(FileProxy fp) {
		if (fp.getAsset() == null) return;
		int seconds = fp.getAsset().getAssetType().getIncidentCutoff();
		//find all other files at the same location with the same asset and merge them

		
		for (FileProxy file : files) {
			file.setIncidentGroup(null);
			if (fp == file) continue;
			
			//they can be joined if they are assocated with the same station
			//within the incident cut-off time
			//they do not have to be at the same location within the station
			if (file.getAsset() != null && // && file.getAsset().equals(fp.getAsset())
					(fp.getStationLocation() != null && file.getStationLocation() != null && file.getStationLocation().getStation().equals(fp.getStationLocation().getStation())) &&
					file.getImageDate() != null && fp.getImageDate() != null 
					&& Math.abs(file.getImageDate().getTime() - fp.getImageDate().getTime()) < seconds * 1000)
				{
				fp.addRelation(file);
			}
		}
	}
	
	
	private void mergeObservations(FileProxy p) {
		List<WaypointObservation> allObservations = new ArrayList<>(p.getRawObservations());
		boolean startOver = true;
		while(startOver) {
			startOver = false;
			for (WaypointObservation wo : allObservations) {
				if (wo.getCategory() == null) {
					//we need to find another observation that
					//has a category that supports this attribute and does not already have
					//a value for this attribute
					for (WaypointObservation oo : allObservations) {
						if (oo.getCategory() == null) continue;
						
						boolean ok = true;
						for (WaypointObservationAttribute aa : wo.getAttributes()) {
							if (!supportsAttribute(oo.getCategory(),aa.getAttribute())) {
								ok = false;
								break;
							}
							if (containsAttribute(oo, aa.getAttribute())) {
								ok = false;
								break;
							}
						}
						if (ok) {
							//merge oo with wo and start over
							oo.getAttributes().addAll(wo.getAttributes());
							allObservations.remove(wo);
							startOver = true;
							break;
						}
						
					}
				}
				if (startOver) break;
			}
		}
		
		//at this point if there are any observations without categories we cannot save
		//those so we will remove them
		for (Iterator<WaypointObservation> iterator = allObservations.iterator(); iterator.hasNext();) {
			WaypointObservation waypointObservation = (WaypointObservation) iterator.next();
			if (waypointObservation.getCategory() == null) {
				if (waypointObservation.getAttributes().isEmpty()) {
					iterator.remove();
				}else {
					p.addWarning(new ActionableWarning(MessageFormat.format(ErrorMessages.CATEGORY_NOT_FOUND.getMessage(locale), waypointObservation.getAttributes().get(0).getAttribute().getName(), waypointObservation.getAttributes().get(0).getAttributeValueAsString(Locale.getDefault()))));
				}
				iterator.remove();
			}
		}
		
		//lets try to merge categories
		//multiple observations with the same category but different attributes get merged
		startOver = true;
		while(startOver) {
			startOver = false;
			for (WaypointObservation wo : allObservations) {
				for (WaypointObservation oo : allObservations) {
					if (oo.equals(wo)) continue;
					if (!oo.getCategory().equals(wo.getCategory())) continue;
					
					boolean ok = true;
					for (WaypointObservationAttribute aa : oo.getAttributes()) {
						if (!supportsAttribute(wo.getCategory(),aa.getAttribute())) {
							ok = false;
							break;
						}
						if (containsAttribute(wo, aa.getAttribute())) {
							ok = false;
							break;
						}
					}
					if (ok) {
						//merge oo with wo and start over
						wo.getAttributes().addAll(oo.getAttributes());
						allObservations.remove(oo);
						startOver = true;
						break;
					}
				}
				if (startOver) break;
			}
		}
		p.setObservations(allObservations);
		for (WaypointObservation wo : allObservations) {
			wo.getCategory().getFullCategoryName();
			for (WaypointObservationAttribute a : wo.getAttributes()) {
				a.setObservation(wo);
			}
		}
	}
	
	private boolean containsAttribute(WaypointObservation wo, Attribute a) {
		if (wo.getAttributes() == null) return false;
		for (WaypointObservationAttribute aa : wo.getAttributes()) {
			if (aa.getAttribute().equals(a)) return true;
		}
		return false;
	}
	private boolean supportsAttribute(Category c, Attribute a) {
		for (CategoryAttribute ca : c.getAttributes()) {
			if (ca.getAttribute().equals(a)) return true;
		}
		return false;
	}
	
	private void processMetadata(FileProxy p, AssetMetadataMapping mapping, Session session) throws Exception {
		Metadata fileMetadata = (Metadata) p.getData(EXIF_METADATA_KEY);
		if (fileMetadata == null) {
			fileMetadata = FileMetadataReader.readMetadata(p.getFile());
			p.putData(EXIF_METADATA_KEY,  fileMetadata);
		}
		if (mapping.getMetadataField() instanceof ExifMetadataField) {
			processMapping((ExifMetadataField) mapping.getMetadataField(), mapping, p, session, fileMetadata);
		}else if (mapping.getMetadataField() instanceof XmpMetadataField) {
			processMapping((XmpMetadataField) mapping.getMetadataField(), mapping, p, session, fileMetadata);
		}
		
		
	}
	
	private void processMapping(XmpMetadataField field, AssetMetadataMapping mapping, FileProxy p, Session session, Metadata fileMetadata ) {
		if (field == null){
			//Exception ex =  new Exception(MessageFormat.format("Could not parse mapping: {0}", mapping.getMetadataKey()));
			//ex.printStackTrace();
			return;
		}

		String pathValue = (String) field.findValue(fileMetadata);
		if (pathValue == null) return; //not value found
		
		if (mapping.getMappedAssetProperty() != null) {
			switch(mapping.getMappedAssetProperty()) {
			case ASSET_ID:
				if (p.getAsset() != null) return;	//we already have an asset from another mapping; do not try again
				findAsset(p, pathValue, session);
				return;
			case LOCATION_ID:
				if (p.getStationLocation() != null) return; //already have a location; do not try again
				findLocation(p, pathValue, session);
				return;
			case STATION_ID:
				if (p.getStation() != null) return; //already have a station; do not try again
				findStation(p, pathValue, session);
				return;
			case WAYPOINT_COMMENT:
				//merge comments
				String comment = pathValue;
				if (p.getWaypointComment() != null) comment += "\n" + comment; //$NON-NLS-1$
				p.setWaypointComment(comment);
			}
		} else {
			//we are mapping categories/attributes
			if (mapping.getMappedAttribute() == null && mapping.getMappedCategory() != null) {
				//mapping a category but no attribute
				boolean add = false;
				if (field.getValue() == null) {
					//we only check for existence which is does
					//so add the category as an observation
					add = true;
				} else {
					//only mapped to this category if the tag value matches the provided value
					if (field.getValue().trim().equalsIgnoreCase(pathValue.trim())) {
						add = true;
					}
				}
				if (add) {
					WaypointObservation wo = new WaypointObservation();
					wo.setCategory(mapping.getMappedCategory());
					wo.setAttributes(new ArrayList<>());
					p.addRawObservation(wo);
				}
				
			}else if (mapping.getMappedAttribute() != null) {
				//mapping an attribute and maybe a value
				WaypointObservation wo = new WaypointObservation();
				if (mapping.getMappedCategory() != null) wo.setCategory(mapping.getMappedCategory());
				WaypointObservationAttribute woa = new WaypointObservationAttribute();
				woa.setAttribute(mapping.getMappedAttribute());
				wo.setAttributes(new ArrayList<>());
				wo.getAttributes().add(woa);
				
				//the tag value identifies the item to map to
				if (mapping.getMappedAttribute().getType() == Attribute.AttributeType.BOOLEAN) {
					Boolean value = null;
					try {
						value = Boolean.valueOf(pathValue);
					}catch (Exception ex) {
						p.addWarning(new ActionableWarning(MessageFormat.format(ErrorMessages.BOOLEAN_PARSE_ERROR.getMessage(locale), pathValue, mapping.getMappedAttribute().getName())));
						return;
					}
					woa.setNumberValue(value == true ? 1.0 : 0);
				}else if (mapping.getMappedAttribute().getType() == Attribute.AttributeType.DATE) {
					Date d = null;
					try {
						d = DateFormat.getDateInstance(DateFormat.MEDIUM).parse(pathValue);
					}catch (Exception ex) {
						d = null;
					}
					try {
						d = DateFormat.getDateInstance(DateFormat.LONG).parse(pathValue);
					}catch (Exception ex) {
						d = null;
					}
					try {
						d = DateFormat.getDateInstance(DateFormat.SHORT).parse(pathValue);
					}catch (Exception ex) {
						d = null;
					}
					if (d == null) {
						p.addWarning(new ActionableWarning(MessageFormat.format(ErrorMessages.DATE_PARSE_ERROR.getMessage(locale), pathValue, mapping.getMappedAttribute().getName())));
						return;
					}
					woa.setDateValue(d);
				}else if (mapping.getMappedAttribute().getType() == Attribute.AttributeType.LIST) {
					if (mapping.getMappedListItem() != null) {
						if (pathValue.equalsIgnoreCase(field.getValue())) {
							woa.setAttributeListItem(mapping.getMappedListItem());
						}else {
							return;
						}
					}else {
						AttributeListItem item = null;
						for (AttributeListItem i : mapping.getMappedAttribute().getAttributeList()) {
							if (i.getKeyId().equalsIgnoreCase(pathValue)) {
								item = i;
								break;
							}
							for (Label l : i.getNames()) {
								if (l.getValue().equalsIgnoreCase(pathValue)) {
									item = i;
									break;
								}
							}
							if (item != null) break;
						}
						if (item == null) {
							p.addWarning(new ActionableWarning(MessageFormat.format(ErrorMessages.LIST_ITEM_PARSE_ERROR.getMessage(locale), pathValue, mapping.getMappedAttribute().getName())));
							return;
						}
						woa.setAttributeListItem(item);
					}
					
				}else if (mapping.getMappedAttribute().getType() == Attribute.AttributeType.TREE) {
					if (mapping.getMappedTreeNode() != null) {
						if (pathValue.equalsIgnoreCase(field.getValue())) {
							woa.setAttributeTreeNode(mapping.getMappedTreeNode());
						}else {
							return;
						}
					}else {
						AttributeTreeNode item = null;
						List<AttributeTreeNode> toSearch = new ArrayList<>();
						toSearch.addAll(mapping.getMappedAttribute().getTree());
						while(!toSearch.isEmpty()) {
							AttributeTreeNode n = toSearch.remove(0);
							if (n.getKeyId().equalsIgnoreCase(pathValue)) {
								item = n;
								break;
							}
							for (Label l : n.getNames()) {
								if (l.getValue().equalsIgnoreCase(pathValue)) {
									item = n;
									break;
								}
							}
							if (n.getChildren() != null) toSearch.addAll(n.getChildren());	
						}
						if (item == null) {
							p.addWarning(new ActionableWarning(MessageFormat.format(ErrorMessages.TREE_NODE_PARSE_ERROR.getMessage(locale), pathValue, mapping.getMappedAttribute().getName())));
							return;
						}
						
						woa.setAttributeTreeNode(item);
					}
				}else if (mapping.getMappedAttribute().getType() == Attribute.AttributeType.NUMERIC) {
					Double numberValue = null;
					try {
						numberValue = Double.parseDouble(pathValue);
					}catch (Exception ex) {
						p.addWarning(new ActionableWarning(MessageFormat.format(ErrorMessages.NUMBER_PARSE_ERROR.getMessage(locale), pathValue, mapping.getMappedAttribute().getName())));
						return;	
					}
					
					woa.setNumberValue(numberValue);	
				}else if (mapping.getMappedAttribute().getType() == Attribute.AttributeType.TEXT) {
					woa.setStringValue(pathValue);
				}
				
				p.addRawObservation(wo);
			}
		}
	}

	private void processMapping(ExifMetadataField field, AssetMetadataMapping mapping, FileProxy p, Session session, Metadata fileMetadata ) {
		if (field == null){
			//Exception ex =  new Exception(MessageFormat.format("Could not parse mapping: {0}", mapping.getMetadataKey()));
			//ex.printStackTrace();
			return;
		}

		Directory tag = (Directory) field.findValue(fileMetadata);
		if (tag == null) return; //tag not found
		if (mapping.getMappedAssetProperty() != null) {
			String tagvalue= tag.getDescription(field.getTagType());			
			switch(mapping.getMappedAssetProperty()) {
			case ASSET_ID:
				if (p.getAsset() != null) return;	//we already have an asset from another mapping; do not try again
				findAsset(p, tagvalue, session);
				return;
			case LOCATION_ID:
				if (p.getStationLocation() != null) return; //already have a location; do not try again
				findLocation(p, tagvalue, session);
				return;
			case STATION_ID:
				if (p.getStation() != null) return; //already have a station; do not try again
				findStation(p, tagvalue, session);
				return;
			case WAYPOINT_COMMENT:
				p.setWaypointComment(tagvalue);
			}
		} else {
			//we are mapping categories/attributes
			if (mapping.getMappedAttribute() == null && mapping.getMappedCategory() != null) {
				//mapping a category but no attribute
				boolean add = false;
				if (field.getTagValue() == null) {
					//we only check for existence which is does
					//so add the category as an observation
					add = true;
				} else {
					//only mapped to this category if the tag value matches the provided value
					String exifTagValue = tag.getDescription(field.getTagType());
					if (exifTagValue == null) return; //no value; not observation
					if (field.getTagValue().trim().equalsIgnoreCase(exifTagValue.trim())) {
						add = true;
					}
				}
				if (add) {
					WaypointObservation wo = new WaypointObservation();
					wo.setCategory(mapping.getMappedCategory());
					wo.setAttributes(new ArrayList<>());
					p.addRawObservation(wo);
				}
				
			}else if (mapping.getMappedAttribute() != null) {
				//mapping an attribute and maybe a value
				WaypointObservation wo = new WaypointObservation();
				if (mapping.getMappedCategory() != null) wo.setCategory(mapping.getMappedCategory());
				WaypointObservationAttribute woa = new WaypointObservationAttribute();
				woa.setAttribute(mapping.getMappedAttribute());
				wo.setAttributes(new ArrayList<>());
				wo.getAttributes().add(woa);
				
				
				//the tag value identifies the item to map to
				if (mapping.getMappedAttribute().getType() == Attribute.AttributeType.BOOLEAN) {
					Boolean value = tag.getBooleanObject(field.getTagType());
					if (value == null) {
						p.addWarning(new ActionableWarning(MessageFormat.format(ErrorMessages.BOOLEAN_TAG_PARSE_ERROR.getMessage(locale), tag.getStringValue(field.getTagType()), mapping.getMappedAttribute().getName())) );
						return;
					}
					woa.setNumberValue(value == true ? 1.0 : 0);
				}else if (mapping.getMappedAttribute().getType() == Attribute.AttributeType.DATE) {
					Date value = tag.getDate(field.getTagType());
					if (value == null) {
						p.addWarning(new ActionableWarning(MessageFormat.format(ErrorMessages.DATE_TAG_PARSE_ERROR.getMessage(locale), tag.getStringValue(field.getTagType()), mapping.getMappedAttribute().getName())));
						return;
					}
					woa.setDateValue(value);
				}else if (mapping.getMappedAttribute().getType() == Attribute.AttributeType.LIST) {
					String value = tag.getDescription(field.getTagType());
					if (mapping.getMappedListItem() != null) {
						if (value.equalsIgnoreCase(field.getTagValue())) {
							woa.setAttributeListItem(mapping.getMappedListItem());
						}else {
							return;
						}
					}else {
						AttributeListItem item = null;
						for (AttributeListItem i : mapping.getMappedAttribute().getAttributeList()) {
							if (i.getKeyId().equalsIgnoreCase(value)) {
								item = i;
								break;
							}
							for (Label l : i.getNames()) {
								if (l.getValue().equalsIgnoreCase(value)) {
									item = i;
									break;
								}
							}
							if (item != null) break;
						}
						if (item == null) {
							p.addWarning(new ActionableWarning(MessageFormat.format(ErrorMessages.LIST_ITEM_TAG_PARSE_ERROR.getMessage(locale), tag.getStringValue(field.getTagType()), mapping.getMappedAttribute().getName())));
							return;
						}
						woa.setAttributeListItem(item);
					}
					
				}else if (mapping.getMappedAttribute().getType() == Attribute.AttributeType.TREE) {
					String value = tag.getDescription(field.getTagType());
					if (mapping.getMappedTreeNode() != null) {
						if (value.equalsIgnoreCase(field.getTagValue())) {
							woa.setAttributeTreeNode(mapping.getMappedTreeNode());
						}else {
							return;
						}
					}else {
						AttributeTreeNode item = null;
						List<AttributeTreeNode> toSearch = new ArrayList<>();
						toSearch.addAll(mapping.getMappedAttribute().getTree());
						while(!toSearch.isEmpty()) {
							AttributeTreeNode n = toSearch.remove(0);
							if (n.getKeyId().equalsIgnoreCase(value)) {
								item = n;
								break;
							}
							for (Label l : n.getNames()) {
								if (l.getValue().equalsIgnoreCase(value)) {
									item = n;
									break;
								}
							}
							if (n.getChildren() != null) toSearch.addAll(n.getChildren());	
						}
						if (item == null) {
							p.addWarning(new ActionableWarning(MessageFormat.format(ErrorMessages.TREE_NODE_TAG_PARSE_ERROR.getMessage(locale), tag.getStringValue(field.getTagType()), mapping.getMappedAttribute().getName())));
							return;
						}
						
						woa.setAttributeTreeNode(item);
					}
				}else if (mapping.getMappedAttribute().getType() == Attribute.AttributeType.NUMERIC) {
					Double value = tag.getDoubleObject(field.getTagType());
					if (value == null) {
						Integer x = tag.getInteger(field.getTagType());
						if (x != null) value = x.doubleValue();
					}
					if (value == null) {
						Float x = tag.getFloatObject(field.getTagType());
						if (x != null) value = x.doubleValue();
					}
					if (value == null) {
						Rational x = tag.getRational(field.getTagType());
						if (x != null) value = x.doubleValue();
					}
					if (value == null) {
						p.addWarning(new ActionableWarning(MessageFormat.format(ErrorMessages.NUMBER_TAG_PARSE_ERROR.getMessage(locale), tag.getStringValue(field.getTagType()), mapping.getMappedAttribute().getName())));
						return;
					}
					woa.setNumberValue(value);	
				}else if (mapping.getMappedAttribute().getType() == Attribute.AttributeType.TEXT) {
					String value = tag.getDescription(field.getTagType());
					woa.setStringValue(value);
				}
				
				p.addRawObservation(wo);
			}
		}
	}
	private void findAsset(FileProxy p, String id, Session session) {
		//search the database for the given asset id
		String hql = "FROM Asset WHERE conservationArea = :ca and lower(id) = lower(:id)"; //$NON-NLS-1$
		Asset asset = (Asset) session.createQuery(hql)
				.setParameter("ca", ca) //$NON-NLS-1$
				.setParameter("id", id) //$NON-NLS-1$
				.uniqueResult();
		if (asset != null) {
			asset.getAssetType().getIncidentCutoff();
			p.setAsset(asset);
			return;
		}else {
			p.addWarning(new NewAssetWarning(MessageFormat.format(ErrorMessages.ASSET_NOT_FOUND.getMessage(locale), id), id));
		}
	}
	
	private void findLocation(FileProxy p, String id, Session session) {
		//search the database for the given asset id
		String hql = "FROM AssetStationLocation WHERE station.conservationArea = :ca and lower(id) = lower(:id)"; //$NON-NLS-1$
		AssetStationLocation location = (AssetStationLocation) session.createQuery(hql)
				.setParameter("ca", ca) //$NON-NLS-1$
				.setParameter("id", id) //$NON-NLS-1$
				.uniqueResult();
		if (location != null) {
			if (p.getStation() != null && p.getStation() != location.getStation()) {
				p.addWarning(new ActionableWarning(MessageFormat.format(ErrorMessages.STATION_OVERWRITE.getMessage(locale), p.getStation().getId(), location.getId())) );
			}
			p.setStationLocation(location);
			return;
		}else {
			p.addWarning(new NewLocationWarning(MessageFormat.format(ErrorMessages.STATION_LOCATION_NOT_FOUND.getMessage(locale), id), id));
		}
	}
	
	private void findStation(FileProxy p, String id, Session session) {
		//search the database for the given asset id
		
		String hql = "FROM AssetStation WHERE conservationArea = :ca and lower(id) = lower(:id)"; //$NON-NLS-1$
		AssetStation station = (AssetStation) session.createQuery(hql)
				.setParameter("ca", ca) //$NON-NLS-1$
				.setParameter("id", id) //$NON-NLS-1$
				.uniqueResult();
		if (station != null) {
			if (p.getStationLocation() != null && p.getStationLocation().getStation() != station) {
				return;
			}
			p.setStation(station);
			return;
		}else {
			p.addWarning(new ActionableWarning(MessageFormat.format(ErrorMessages.STATION_NOT_FOUND.getMessage(locale), id)));
		}
	}
		

	//TODO: if track is set we probably don't want to be merge deployments; each
	//time out should probably be a deployments but we can deal with that when we
	//get to processing videos
	public static AssetDeployment findAssetDeployment(Waypoint wp, Asset asset, AssetStationLocation location, Session session, Locale l) {
		
		//1.  Find a deployment for the asset that is between the start and end date of the waypoint
		String hql = "FROM AssetDeployment WHERE asset = :asset and startDate <= :date1 and (endDate is null or endDate>=:date2)"; //$NON-NLS-1$
		List<AssetDeployment> matchingDeployment = session.createQuery(hql, AssetDeployment.class)
			.setParameter("date1", wp.getDateTime()) //$NON-NLS-1$
			.setParameter("date2",  wp.getDateTime()) //$NON-NLS-1$
			.setParameter("asset", asset) //$NON-NLS-1$
			.list();
		
		if (matchingDeployment.size() > 1) {
			throw new IllegalStateException(MessageFormat.format(ErrorMessages.MULTIPLE_DEPLOYMENTS.getMessage(l), asset.getId() ));
		}
		
		if (matchingDeployment.size() == 1) {
			AssetDeployment matchedDeployment = matchingDeployment.get(0);
			//if station location matches; this is fine we can merge with previous deployment
			if (matchedDeployment.getStationLocation().equals(location)) return matchedDeployment;
			
			//here the station location does not match so this camera has been deployed to a different location
			if (matchedDeployment.getEndDate() == null) {
				//we can update this deployment
				//set the end date to the last waypoint date we have
				Date endDate = null;
				for (AssetWaypoint aw : matchedDeployment.getAssetWaypoints()) {
					if (endDate == null || aw.getWaypoint().getDateTime().after(endDate)) endDate = aw.getWaypoint().getDateTime();
				}
				if (endDate == null) {
					//deployment had not waypoints the best we can do is take the current time and subtract a minute
					endDate = new Date(wp.getDateTime().getTime() - 60_000);
				}
				matchedDeployment.setEndDate(endDate);
				
				//create a new deployment & return it
				return createNewDeployment(asset, wp.getDateTime(), location);
			
			}else {
				throw new IllegalStateException(MessageFormat.format(ErrorMessages.MULTIPLE_DEPLOYMENTS.getMessage(l), asset.getId() ));
			}
			
		} else {
			//not deployments that contained the wp date were found
			//search for previous and next and see if we can expand one of them
			hql = "FROM AssetDeployment WHERE asset = :asset"; //$NON-NLS-1$
			List<AssetDeployment> allDeployments = session.createQuery(hql, AssetDeployment.class).setParameter("asset", asset).list(); //$NON-NLS-1$
			
			AssetDeployment prev = null;
			AssetDeployment next = null;
			
			for (AssetDeployment d : allDeployments) {
				if (d.getEndDate() != null && d.getEndDate().before(wp.getDateTime())) {
					if (prev == null) {
						prev = d;
					}else if (d.getEndDate().after(prev.getEndDate())) {
						prev = d;
					}
				}
				if (d.getStartDate().after(wp.getDateTime())) {
					if (next == null) {
						next = d;
					}else if (d.getStartDate().before(next.getStartDate())) {
						next = d;
					}
				}
			}
			
			if (prev == null && next == null) {
				return createNewDeployment(asset, wp.getDateTime(), location);
			}
			if (prev != null && prev.getStationLocation().equals(location) && next != null && next.getStationLocation().equals(location)) {
				//both prev and next are candidates, pick the closer one and extend it
				if (wp.getDateTime().getTime() - prev.getEndDate().getTime() > next.getStartDate().getTime() - wp.getDateTime().getTime()) {
					//pick next
					next.setStartDate(wp.getDateTime());
					return next;
				}else {
					//pick prev
					prev.setEndDate(wp.getDateTime());
					return prev;
				}
			}
			if (prev != null && prev.getStationLocation().equals(location)) {
				//only previous is a candidate
				prev.setEndDate(wp.getDateTime());
				return prev;
			}
			if (next != null && next.getStationLocation().equals(location)) {
				//only next is candidate
				next.setStartDate(wp.getDateTime());
				return next;
			}
			if (next != null) {
				//create a new with an end date of next start time
				AssetDeployment d = createNewDeployment(asset, wp.getDateTime(), location);
				d.setEndDate(next.getStartDate());
				return d;
			}else {
				//create a new one with no end date
				return createNewDeployment(asset, wp.getDateTime(), location);
			}
			
		}
	}
	
	private static AssetDeployment createNewDeployment(Asset asset, Date startDate, AssetStationLocation location) {
		AssetDeployment newDeployment = new AssetDeployment();
		newDeployment.setAsset(asset);
		newDeployment.setAssetWaypoints(new ArrayList<>());
		newDeployment.setAttributeValues(new ArrayList<>());
		newDeployment.setDisruptions(new ArrayList<>());
		newDeployment.setStartDate(startDate);
		newDeployment.setEndDate(null);
		newDeployment.setStationLocation(location);
		return newDeployment;
	}
	
	/**
	 * For providing database session to file processor
	 * @author Emily
	 *
	 */
	public interface IConnectionProvider {
		/**
		 * Opens a new database session.  It is up to the user to close the session when they are done with it.
		 * @return
		 */
		public Session openSession();
	};
}
