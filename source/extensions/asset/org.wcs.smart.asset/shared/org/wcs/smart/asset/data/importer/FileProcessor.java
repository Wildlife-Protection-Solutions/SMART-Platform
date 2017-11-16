package org.wcs.smart.asset.data.importer;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetMetadataMapping;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.model.mapping.ExifMetadataField;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;

import com.drew.lang.Rational;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;

public class FileProcessor {

	private List<Path> files;
	private HashMap<Path, FileProxy> fileDetails;
	
	private ConservationArea ca;
	
	public FileProcessor(ConservationArea ca, List<Path> files) {
		this.ca = ca;
		this.files = files;
	}
	
	public void processFiles(IProgressMonitor monitor) {
		fileDetails = new HashMap<>();
	
		monitor.beginTask("Processing Asset Files", files.size());
		files.forEach(f->{
			monitor.subTask(f.toString());
			processFile(f, ca);	
			monitor.worked(1);
			if (monitor.isCanceled()) return;
		});
	}
	
	public void processFile(Path file, ConservationArea ca) {
		try {
			FileProxy proxy = FileMetadataReader.readFile(file, ca);
			try(Session session = HibernateManager.openSession()){
				processMetadata(proxy, session);
				proxy.updateAssetDeployment(session);
			}
			fileDetails.put(file,  proxy);
		}catch (Exception ex) {
			ex.printStackTrace();
			//TODO: process exception
			FileProxy p = new FileProxy(file, ca);
//			p.setProcessingException(ex);
			fileDetails.put(file,  p);
		}
	}
	
	public FileProxy getFileDetails(Path file) {
		return fileDetails.get(file);
	}
	
	public List<Path> getFiles() {
		return this.files;
	}
	
	public Collection<FileProxy> getFileDetails() {
		return fileDetails.values();
	}
	
	public void removeFile(FileProxy file) {
		files.remove(file.getFile());
		fileDetails.remove(file.getFile());
	}
	
	
	public boolean isValid() {
		if (fileDetails.size() != files.size()) return false;
		
		for (FileProxy proxy : fileDetails.values()) {
			if (!proxy.isValid()) return false;
		}
		return true;
	}
	
	
	public void processMetadata(FileProxy p, Session session) throws Exception {
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
			switch(mapping.getMetadataType() ) {
			case EXIF:
				processMetadata(p, mapping, session);
				break;
			case XMP:
				//TOOD: implement me
				break;
			default:
				//TOOD: mapping type not supported
			}
			
		}
		
		//process observations
		mergeObservations(p);
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
				p.addWarning(new ActionableWarning("The observation only has attribute mappings.  A category is required."));
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
		for (WaypointObservation wo : allObservations) wo.getCategory().getFullCategoryName();
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
		Metadata fileMetadata = (Metadata) p.getData("EXIF_METADATA");
		if (fileMetadata == null) {
			fileMetadata = FileMetadataReader.readMetadata(p.getFile());
			p.putData("EXIF_METADATA",  fileMetadata);
		}
		
		ExifMetadataField field = (ExifMetadataField) mapping.getMetadataField();
		if (field == null){
			
			Exception ex =  new Exception(MessageFormat.format("Could not parse mapping: {0}", mapping.getMetadataKey()));
			ex.printStackTrace();
			return;
		}

		Directory tag = (Directory) field.findValue(fileMetadata);
		if (tag == null) return; //tag not found
		
		if (mapping.getMappedAssetProperty() != null) {
			
			String tagvalue= tag.getString(field.getTagType());
			
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
			}
		} else {
			//we are mapping categories/attributes
			if (tag == null) return;  //tag not found; so we cannot make any observations from this
			if (mapping.getMappedAttribute() == null && mapping.getMappedCategory() != null) {
				//mapping a category but no attribute
				boolean add = false;
				if (field.getTagValue() == null) {
					//we only check for existence which is does
					//so add the category as an observation
					add = true;
				} else {
					//only mapped to this category if the tag value matches the provided value
					String exifTagValue = tag.getString(field.getTagType());
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
						p.addWarning(new ActionableWarning(MessageFormat.format("Could not parse boolean value from the tag {0} for mapping to boolean attribute",tag.getStringValue(field.getTagType()))));
						return;
					}
					woa.setNumberValue(value == true ? 1.0 : 0);
				}else if (mapping.getMappedAttribute().getType() == Attribute.AttributeType.DATE) {
					Date value = tag.getDate(field.getTagType());
					if (value == null) {
						p.addWarning(new ActionableWarning(MessageFormat.format("Could not parse date from the tag {0} for mapping to date attribute",tag.getStringValue(field.getTagType()))));
						return;
					}
					woa.setDateValue(value);
				}else if (mapping.getMappedAttribute().getType() == Attribute.AttributeType.LIST) {
					String value = tag.getString(field.getTagType());
					if (mapping.getMappedListItem() != null) {
						if (value.equalsIgnoreCase(field.getTagValue())) {
							woa.setAttributeListItem(mapping.getMappedListItem());
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
							p.addWarning(new ActionableWarning(MessageFormat.format("Could parse date from the tag {0} for mapping to list attribute.  Not list item with this value found.", tag.getStringValue(field.getTagType()))));
							return;
						}
						woa.setAttributeListItem(item);
					}
					
				}else if (mapping.getMappedAttribute().getType() == Attribute.AttributeType.TREE) {
					String value = tag.getString(field.getTagType());
					if (mapping.getMappedTreeNode() != null) {
						if (value.equalsIgnoreCase(field.getTagValue())) {
							woa.setAttributeTreeNode(mapping.getMappedTreeNode());
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
							p.addWarning(new ActionableWarning(MessageFormat.format("Could parse date from the tag {0} for mapping to tree attribute.  Not tree not found that matches the value.", tag.getStringValue(field.getTagType()))));
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
						p.addWarning(new ActionableWarning(MessageFormat.format("Could not parse number from the tag {0} for mapping to number attribute",tag.getStringValue(field.getTagType()))));
						return;
					}
					woa.setNumberValue(value);	
				}else if (mapping.getMappedAttribute().getType() == Attribute.AttributeType.TEXT) {
					String value = tag.getString(field.getTagType());
					woa.setStringValue(value);
				}
				
				p.addRawObservation(wo);
			}
		}
	}
	
	private void findAsset(FileProxy p, String id, Session session) {
		//search the database for the given asset id
		String hql = "FROM Asset WHERE conservationArea = :ca and upper(id) = :id";
		Asset asset = (Asset) session.createQuery(hql)
				.setParameter("ca", ca)
				.setParameter("id", id.toUpperCase())
				.uniqueResult();
		if (asset != null) {
			p.setAsset(asset);
			return;
		}else {
			p.addWarning(new ActionableWarning(MessageFormat.format("No asset found with id ''{0}''", id)));
			//TODO: no asset found add a warning to the proxy and continue....we might find
			//the asset in a different mapping
		}
	}
	
	private void findLocation(FileProxy p, String id, Session session) {
		//search the database for the given asset id
		
		String hql = "FROM AssetStationLocation WHERE station.conservationArea = :ca and upper(id) = :id";
		AssetStationLocation location = (AssetStationLocation) session.createQuery(hql)
				.setParameter("ca", ca)
				.setParameter("id", id.toUpperCase())
				.uniqueResult();
		if (location != null) {
			if (p.getStation() != null && p.getStation() != location.getStation()) {
				//TODO: we need a warning; station is going to be overwritted by location
				p.addWarning(new ActionableWarning(MessageFormat.format("Station location {1} is not associated with the station {0} found for the file.  The station location attribute will take precidence.", p.getStation().getId(), location.getId())) );
			}
			p.setStationLocation(location);
			return;
		}else {
			//TODO: no location found add a warning to the proxy and continue....we might find
			//in a different mapping
			p.addWarning(new ActionableWarning(MessageFormat.format("No station location found with id ''{0}''", id)));
		}
	}
	
	private void findStation(FileProxy p, String id, Session session) {
		//search the database for the given asset id
		
		String hql = "FROM AssetStation WHERE conservationArea = :ca and upper(id) = :id";
		AssetStation station = (AssetStation) session.createQuery(hql)
				.setParameter("ca", ca)
				.setParameter("id", id.toUpperCase())
				.uniqueResult();
		if (station != null) {
			if (p.getStationLocation() != null && p.getStationLocation().getStation() != station) {
				//TODO: warning and do not overwrite;
				return;
			}
			p.setStation(station);
			return;
		}else {
			//TODO: no station found add a warning to the proxy and continue....we might find
			//in a different mapping
			p.addWarning(new ActionableWarning(MessageFormat.format("No station found for id ''{0}''", id)));
		}
	}
}
