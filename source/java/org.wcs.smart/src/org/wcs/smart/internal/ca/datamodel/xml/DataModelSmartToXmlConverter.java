package org.wcs.smart.internal.ca.datamodel.xml;

import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AggregationType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeListType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.CategoryAttributeLink;
import org.wcs.smart.internal.ca.datamodel.xml.generate.CategoryType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.CategoryTypeList;
import org.wcs.smart.internal.ca.datamodel.xml.generate.LanguageListType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.LanguageType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.ListNode;
import org.wcs.smart.internal.ca.datamodel.xml.generate.MinMaxType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.NameType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.TreeNodeType;

public class DataModelSmartToXmlConverter {

	
	 
	public static org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel convert(DataModel dm) throws JAXBException, ParseException {
		
		org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel xml = new org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel();
		
		HashMap<String, Language> llookup = processLanguages(dm, xml);
		processAttribute(dm, xml, llookup);
		processCategories(dm, xml, llookup);
		
		return xml;
		
		
	}
	private static void processCategories(DataModel dm, org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel xml, HashMap<String, Language> llookup){
	
		if (dm.getCategories() != null){
			CategoryTypeList ctl = new CategoryTypeList();
			xml.setCategories(ctl);
			for (Category child : dm.getCategories()){
				processCategory(child, ctl.getCategories(), llookup);
			}
		}
	}
	
	private static void processCategory(Category child, List<CategoryType> parentList, HashMap<String, Language> llookup ){
		CategoryType ct = new CategoryType();
		setNames(ct.getNames(), child.getNames(), llookup);
		ct.setIsactive(child.getIsActive());
		ct.setIsmultiple(child.getIsMultiple());
		ct.setKey(child.getKeyId());
		
		if (child.getAttributes() != null){
			for (CategoryAttribute map : child.getAttributes()){
				CategoryAttributeLink link = new CategoryAttributeLink();
				link.setAttributekey(map.getAttribute().getKeyId());
				link.setIsactive(map.getIsActive());
				ct.getAttributes().add(link);
			}
		}
		if (child.getChildren() != null){
			for (Category c : child.getChildren()){
				processCategory(c, ct.getCategories(), llookup);
			}
		}
		parentList.add(ct);
				
	}
	
	
	private static void processAttribute(DataModel dm, org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel xml, HashMap<String, Language> llookup){
		
		AttributeListType atl = new AttributeListType();
		xml.setAttributes(atl);
		for (Attribute att : dm.getAttributes()){
			AttributeType at = new AttributeType();
			at.setIsrequired(att.getIsRequired());
			at.setKey(att.getKeyId());
			if (att.getMaxValue() != null || att.getMinValue() != null){
				MinMaxType mmt = new MinMaxType();	
				mmt.setMaxValue(att.getMaxValue());
				mmt.setMinValue(att.getMinValue());
				at.setQaMinmax(mmt);
			}
			at.setQaRegex(att.getRegex());
			at.setType(att.getType().name());
			
			if (att.getAggregations() != null){
				for (Aggregation agg : att.getAggregations()){
					AggregationType agt = new AggregationType();
					agt.setAggregation(agg.getName());
					at.getAggregations().add(agt);
				}
			}
			
			
			if (att.getTree() != null){
				for (AttributeTreeNode child: att.getTree()){
					processTreeNode(child, at.getTrees(), llookup);
				}
			}
			if (att.getAttributeList() != null){
				for (AttributeListItem item : att.getAttributeList()){
					ListNode ln = new ListNode();
					ln.setKey(item.getKeyId());
					ln.setIsactive(item.getIsActive());
					setNames(ln.getNames(), item.getNames(), llookup);
					
					at.getValues().add(ln);
				}
			}
			
			setNames(at.getNames(), att.getNames(), llookup);
			
			atl.getAttributes().add(at);
		}
		
	}

	private static void processTreeNode(AttributeTreeNode node,
			List<TreeNodeType> parentList, HashMap<String, Language> llookup) {

		TreeNodeType tnt = new TreeNodeType();
		tnt.setKey(node.getKeyId());
		setNames(tnt.getNames(), node.getNames(), llookup);
		tnt.setIsactive(node.getIsActive());
		parentList.add(tnt);
		
		if (node.getChildren() != null) {
			for (AttributeTreeNode child : node.getChildren()) {
				processTreeNode(child, tnt.getChildrens(), llookup);
			}
		}
	}
	
	
	private static void setNames(List<NameType> list, Set<Label> names,HashMap<String, Language> llookup){
		for (Label lbl: names){
			NameType nt = new NameType();
			nt.setValue(lbl.getValue());
			nt.setLanguageCode(llookup.get(new String(lbl.getLanguage().getUuid())).getCode());
			list.add(nt);
		}
		
	}
	private static HashMap<String, Language> processLanguages(DataModel dm, org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel xml){
		HashMap<String, Language> lookup = new HashMap<String, Language>();
		LanguageListType llt = new LanguageListType();
		xml.setLanguages(llt);
		for (Language ll : dm.getConservationArea().getLanguages()){
			LanguageType lt = new LanguageType();
			lt.setCode(ll.getCode());
			llt.getLanguages().add(lt);
			lookup.put(new String(ll.getUuid()), ll);
		}	
		return lookup;
		
	}
	
}
