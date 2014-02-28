package org.wcs.smart.entity.xml;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.xml.model.AggregationType;
import org.wcs.smart.entity.xml.model.DataModelAttribute;
import org.wcs.smart.entity.xml.model.LabelType;
import org.wcs.smart.entity.xml.model.ListNode;
import org.wcs.smart.entity.xml.model.MinMaxType;
import org.wcs.smart.entity.xml.model.TreeNodeType;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Converts EntityType schemas to xml format.
 * <p>Does not include entities or any sighting data in export.</p>
 * @author Emily
 *
 */
public class EntityTypeToXmlConverter {

	/**
	 * Converts a EntityType to the XML representation
	 * of the xml type.
	 * 
	 * 
	 * @param entityType
	 * @return
	 */
	public static org.wcs.smart.entity.xml.model.EntityType toXml(EntityType entityType, IProgressMonitor monitor){
		monitor.beginTask(MessageFormat.format(Messages.EntityTypeToXmlConverter_Progress1, new Object[]{entityType.getName()}), entityType.getAttributes().size() + 1);
		org.wcs.smart.entity.xml.model.EntityType xml = new org.wcs.smart.entity.xml.model.EntityType();

		xml.setKeyid(entityType.getKeyId());
		xml.setStatus(entityType.getStatus().name());
		xml.setType(entityType.getType().name());
		xml.getNames().addAll(processNames(entityType));
		xml.setDmAttribute(processAttribute(entityType.getDmAttribute()));
		monitor.worked(1);
		
		for (EntityAttribute et : entityType.getAttributes()){
			monitor.subTask(MessageFormat.format(Messages.EntityTypeToXmlConverter_Progress2, new Object[]{et.getName()}));
			
			org.wcs.smart.entity.xml.model.EntityAttribute xmlAtt = new org.wcs.smart.entity.xml.model.EntityAttribute();
			
			xmlAtt.setDmAttribute(processAttribute(et.getDmAttribute()));
			xmlAtt.setIsPrimary(et.getIsPrimary());
			xmlAtt.setIsRequired(et.getIsRequired());
			xmlAtt.setKeyid(et.getKeyId());
			xmlAtt.getAliases().addAll(processNames(et));
			
			xml.getAttributes().add(xmlAtt);
			monitor.worked(1);
		}
		monitor.done();
		
		return xml;
	}
	
	/**
	 * exports all labels associated with a named item
	 * @param item
	 * @return
	 */
	private static List<LabelType> processNames(NamedItem item){
		List<LabelType> values = new ArrayList<LabelType>();
		for (Label l : item.getNames()){
			LabelType lt = new LabelType();
			lt.setLanguageCode(l.getLanguage().getCode());
			lt.setValue(l.getValue());
			lt.setIsDefault(l.getLanguage().equals(SmartDB.getCurrentConservationArea().getDefaultLanguage()));
			values.add(lt);
		}
		return values;
	}
	
	/**
	 * Exports a data model attribute
	 * @param dmAttribute
	 * @return
	 */
	private static DataModelAttribute processAttribute(Attribute dmAttribute){
		DataModelAttribute at = new DataModelAttribute();
		at.setIsrequired(dmAttribute.getIsRequired());
		at.setKey(dmAttribute.getKeyId());
		if (dmAttribute.getMaxValue() != null || dmAttribute.getMinValue() != null){
			MinMaxType mmt = new MinMaxType();	
			mmt.setMaxValue(dmAttribute.getMaxValue());
			mmt.setMinValue(dmAttribute.getMinValue());
			at.setQaMinmax(mmt);
		}
		at.setQaRegex(dmAttribute.getRegex());
		at.setType(dmAttribute.getType().name());
			
		if (dmAttribute.getAggregations() != null){
			for (Aggregation agg : dmAttribute.getAggregations()){
				AggregationType agt = new AggregationType();
				agt.setAggregation(agg.getName());
				at.getAggregations().add(agt);
			}
		}
			
			
		if (dmAttribute.getTree() != null){
			for (AttributeTreeNode child: dmAttribute.getTree()){
				processTreeNode(child, at.getTree());
			}
		}
		if (dmAttribute.getAttributeList() != null){
			for (AttributeListItem item : dmAttribute.getAttributeList()){
				ListNode ln = new ListNode();
				ln.setKey(item.getKeyId());
				ln.setIsactive(item.getIsActive());
				ln.getNames().addAll(processNames(item));					
				at.getValues().add(ln);
			}
		}
		
		at.getNames().addAll(processNames(dmAttribute));
		return at;
	}

	private static void processTreeNode(AttributeTreeNode node,
			List<TreeNodeType> parentList) {

		TreeNodeType tnt = new TreeNodeType();
		tnt.setKey(node.getKeyId());
		
		tnt.getNames().addAll(processNames(node));
		tnt.setIsactive(node.getIsActive());
		parentList.add(tnt);
	
		if (node.getChildren() != null) {
			for (AttributeTreeNode child : node.getChildren()) {
				processTreeNode(child, tnt.getChildren());
			}
		}
	}

}
