package org.wcs.smart.connect.database;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.persistence.Tuple;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.SimpleDataModel;
import org.wcs.smart.ca.icon.Icon;

/**
 * Class that load the data model bypassing
 * the hibernate objects. It specifically selects the required
 * fields from the data model object tables and creates new objects
 * with these values.
 * 
 * This is done as loading all the translation objects via hibernate
 * resulted in a large number of queries to the database (one per data model
 * object) and for large data models this resulted in a very slow api results on
 * connect.
 * 
 * This will NOT ALLOW you to export icon files so only use
 * it if you don't want the icon files.
 *  
 * @author Emily
 *
 */
public class FastDataModelLoader {

	public SimpleDataModel loadDataModel(Session session, ConservationArea ca) {
		List<Label> labels = session.createQuery("FROM Label l WHERE l.id.language.ca = :ca", Label.class) //$NON-NLS-1$
				.setParameter("ca",ca) //$NON-NLS-1$
				.list();
			
		//map of all labels
		HashMap<UUID, HashSet<Label>> allLabels = new HashMap<>();
		//default name 
		HashMap<UUID, String> nameLabel = new HashMap<>();
			
		labels.forEach(e->{
			Hibernate.initialize(e.getLanguage());
			UUID uuid = e.getElementuuid().getUuid();
			if (!allLabels.containsKey(uuid)) allLabels.put(uuid,  new HashSet<>());
			allLabels.get(uuid).add(e);
			if (e.getLanguage().isDefault()) {
				nameLabel.put(uuid, e.getValue());
			}
		}); 

		/* -- icons -- */
		List<Tuple> icons = session.createQuery("SELECT uuid, keyId FROM org.wcs.smart.ca.icon.Icon WHERE conservationArea = :ca", Tuple.class) //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.list();
		
		Map<UUID, Icon> iconMap = new HashMap<>();
		for (Tuple t : icons) {
			Icon clone = new Icon();
			clone.setConservationArea(ca);
			clone.setKeyId(t.get(1, String.class));
			clone.setUuid(t.get(0, UUID.class));
			clone.setNames(allLabels.get(clone.getUuid()));
			clone.setName(nameLabel.get(clone.getUuid()));
			iconMap.put(clone.getUuid(), clone);
		}
		
		/* -- categories -- */
		List<Tuple> categories = session.createQuery("SELECT uuid, keyId, hkey, categoryOrder, isActive, isMultiple, parent.uuid, icon.uuid FROM Category WHERE conservationArea = :ca ", Tuple.class) //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.list();
			
		HashMap<UUID, Category> uuidToCategory = new HashMap<>();
		HashMap<UUID, UUID> categoryKidToParent = new HashMap<>();
		List<Category> roots = new ArrayList<>();
		
		for (Tuple t : categories) {
			Category c = new Category();
			c.setUuid(t.get(0, UUID.class));
			c.setAttributes(new ArrayList<>());
			c.setCategoryOrder( t.get(3, Integer.class) );
			c.setHkey(t.get(2, String.class));
			c.setKeyId(t.get(1, String.class));
			c.setIsActive(t.get(4, Boolean.class));
			c.setIsMultiple(t.get(5, Boolean.class));
			c.setNames(allLabels.get(c.getUuid()));
			c.setName(nameLabel.get(c.getUuid()));
			c.setConservationArea(ca);
			c.setChildren(new ArrayList<>());
			c.setIcon(iconMap.get(t.get(7, UUID.class)));
				
			UUID parent = t.get(6, UUID.class);
			uuidToCategory.put(c.getUuid(),c);
			if (parent == null) {
				roots.add(c);
			}else {
				categoryKidToParent.put(c.getUuid(), parent);
			}		
		}
		//setup parent/child relationships
		for (Entry<UUID,UUID> parent : categoryKidToParent.entrySet()) {				
			Category cparent = uuidToCategory.get(parent.getValue());
			Category ckid = uuidToCategory.get(parent.getKey());
			cparent.getChildren().add(ckid);
			ckid.setParent(cparent);
		}
		//sort categories
		for (Category c : uuidToCategory.values()) {
			c.getChildren().sort((a,b)->Integer.compare(a.getCategoryOrder(), b.getCategoryOrder()));
		}
		
		/* -- attributes -- */
		List<Tuple> attributes = session.createQuery("SELECT uuid, type, keyId, isRequired, maxValue, minValue, regex, icon.uuid FROM Attribute WHERE conservationArea = :ca", Tuple.class) //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.list();

		HashMap<UUID, Attribute> attributemap = new HashMap<>();
		for (Tuple row : attributes) {
			Attribute newattribute = new Attribute();
				
			newattribute.setConservationArea(ca);
			newattribute.setIsRequired(row.get(3, Boolean.class));
			newattribute.setKeyId(row.get(2, String.class));
			newattribute.setMaxValue(row.get(4, Double.class));
			newattribute.setMinValue(row.get(5, Double.class));
			newattribute.setRegex(row.get(6, String.class));
			newattribute.setType(row.get(1, Attribute.AttributeType.class));
			newattribute.setUuid(row.get(0, UUID.class));
			newattribute.setIcon(iconMap.get(row.get(7, UUID.class)));
			newattribute.setNames(allLabels.get(newattribute.getUuid()));
			newattribute.setName(nameLabel.get(newattribute.getUuid()));
				
			newattribute.setAggregations(new ArrayList<>());
			
			if (newattribute.getType().isList()) {
				newattribute.setAttributeList(new ArrayList<>());
			}else if (newattribute.getType() == Attribute.AttributeType.TREE) {
				newattribute.setTree(new ArrayList<>());
			}
				
			attributemap.put(newattribute.getUuid(), newattribute);
		}

		//aggregations
		List<Aggregation> allaggs = session.createQuery("FROM Aggregation", Aggregation.class).list(); //$NON-NLS-1$
		Map<String, Aggregation> aggmap = new HashMap<>();
		for(Aggregation g : allaggs) {
			aggmap.put(g.getName(), g);
		}
			
		List<Tuple> aggregations = session.createQuery("SELECT a.uuid, k.name FROM Attribute a join a.aggregations k WHERE a.conservationArea = :ca", Tuple.class) //$NON-NLS-1$
				.setParameter("ca", ca) //$NON-NLS-1$
				.list();
		for (Tuple t : aggregations) {
			Attribute a = attributemap.get(t.get(0, UUID.class));
			String agg = t.get(1, String.class);
			a.getAggregations().add(aggmap.get(agg));				
		}	
			
		
		//list items
		List<Tuple> listitems = session.createQuery("SELECT uuid, keyId, listOrder, isActive, attribute.uuid, icon.uuid FROM AttributeListItem li WHERE li.attribute.conservationArea = :ca", Tuple.class) //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.list();
		for (Tuple t : listitems) {
			AttributeListItem newli = new AttributeListItem();
			newli.setIcon(iconMap.get(t.get(5, UUID.class)));
			newli.setIsActive(t.get(3, Boolean.class));
			newli.setKeyId(t.get(1, String.class));
			newli.setListOrder(t.get(2, Integer.class));
			newli.setUuid(t.get(0, UUID.class));
			newli.setNames(allLabels.get(newli.getUuid()));
			newli.setName(nameLabel.get(newli.getUuid()));
		
			Attribute a = attributemap.get(t.get(4, UUID.class));
			newli.setAttribute(a);
			a.getAttributeList().add(newli);
		}
		
		//tree nodes
		List<Tuple> treeitems = session.createQuery("SELECT uuid, keyId, hkey, nodeOrder, isActive, attribute.uuid, parent.uuid, icon.uuid FROM AttributeTreeNode li WHERE li.attribute.conservationArea = :ca", Tuple.class) //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.list();
			
		Map<UUID, UUID> treeKid2Parent = new HashMap<>();
		Map<UUID, AttributeTreeNode> nodemap = new HashMap<>();
			
		for (Tuple t : treeitems) {
			AttributeTreeNode newtn = new AttributeTreeNode();
			newtn.setIcon(iconMap.get(t.get(7, UUID.class)));
			newtn.setIsActive(t.get(4, Boolean.class));
			newtn.setKeyId(t.get(1, String.class));
			newtn.setHkey(t.get(2, String.class));
			newtn.setNodeOrder(t.get(3, Integer.class));
			newtn.setUuid(t.get(0, UUID.class));
			newtn.setParent(newtn);
			newtn.setChildren(new ArrayList<>());
			newtn.setNames(allLabels.get(newtn.getUuid()));
			newtn.setName(nameLabel.get(newtn.getUuid()));
				
			Attribute a = attributemap.get(t.get(5, UUID.class));
			newtn.setAttribute(a);
			nodemap.put(newtn.getUuid(), newtn);
				
			UUID parent = t.get(6, UUID.class);
			if (parent == null) {
				a.getTree().add(newtn);
			}else {
				treeKid2Parent.put(newtn.getUuid(), parent);
			}
		}
		//relationships
		for (Entry<UUID, UUID> kidparent : treeKid2Parent.entrySet()) {
			AttributeTreeNode kid = nodemap.get(kidparent.getKey());
			AttributeTreeNode parent = nodemap.get(kidparent.getValue());
				
			parent.getChildren().add(kid);
			kid.setParent(parent);
		}
		//sort nodes
		nodemap.values().forEach(e->{
			e.getChildren().sort((a,b)->Integer.compare(a.getNodeOrder(), b.getNodeOrder()));
		});
			
		//sort root lists/nodes
		for (Attribute a : attributemap.values()) {
			if (a.getAttributeList() != null &&  !a.getAttributeList().isEmpty()) {
				a.getAttributeList().sort((l1,l2)->Integer.compare(l1.getListOrder(), l2.getListOrder()));
			}
			if (a.getTree() != null && !a.getTree().isEmpty()) {
				a.getTree().sort((l1,l2)->Integer.compare(l1.getNodeOrder(), l2.getNodeOrder()));
			}
		}
		
		/* -- category attribute links -- */
		List<Tuple> links = session.createQuery("SELECT id.attribute.uuid, id.category.uuid, order, isActive FROM CategoryAttribute WHERE id.attribute.conservationArea = :ca", Tuple.class) //$NON-NLS-1$
			.setParameter("ca", ca) //$NON-NLS-1$
			.list();
			
		for (Tuple t : links) {				
			Category c = uuidToCategory.get(t.get(1, UUID.class));
			Attribute a = attributemap.get(t.get(0, UUID.class));
			CategoryAttribute caa = new CategoryAttribute();
			caa.setAttribute(a);
			caa.setCategory(c);
			caa.setOrder(t.get(2, Integer.class));
			caa.setIsActive(t.get(3, Boolean.class));
			c.getAttributes().add(caa);
		}
			
		roots.sort((a,b)->Integer.compare(a.getCategoryOrder(),b.getCategoryOrder()));
		SimpleDataModel dm = new SimpleDataModel(ca, roots, new ArrayList<Attribute>(attributemap.values()));
		return dm;		
	}
}
