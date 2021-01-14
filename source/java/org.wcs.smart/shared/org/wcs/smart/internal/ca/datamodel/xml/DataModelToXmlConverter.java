/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.internal.ca.datamodel.xml;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.SimpleDataModel;
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

/**
 * Converts a database data-model to the xml representation 
 * of the data model.
 * 
 * @author egouge
 *
 */
public class DataModelToXmlConverter {

	
	/**
	 * Converts an smart model to xml model 
	 * @param dm smart data model
	 * @param monitor progress monitor
	 * @return xml data model
	 */
	public org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel convert(SimpleDataModel dm) {
		
		org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel xml = new org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel();
		HashMap<String, Language> llookup = processLanguages(dm, xml);
		processAttributes(dm, xml, llookup);
		processCategories(dm, xml, llookup);
		return xml;
		
		
	}
	protected void processCategories(SimpleDataModel dm, 
			org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel xml, 
			HashMap<String, Language> llookup){
	
		if (dm.getCategories() != null){
			CategoryTypeList ctl = new CategoryTypeList();
			xml.setCategories(ctl);
			for (Category child : dm.getCategories()){
				processCategory(child, ctl.getCategories(), llookup);
			}
		}
	}
	
	protected void processCategory(Category child, List<CategoryType> parentList, 
			HashMap<String, Language> llookup ){
		
		CategoryType ct = new CategoryType();
		setNames(ct.getNames(), child.getNames(), llookup);
		ct.setIsactive(child.getIsActive());
		ct.setIsmultiple(child.getIsMultiple());
		ct.setKey(child.getKeyId());
		if (child.getIcon() != null) ct.setIconkey(child.getIcon().getKeyId());
		
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
	
	
	protected void processAttributes(SimpleDataModel dm, 
			org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel xml, 
			HashMap<String, Language> llookup){
		
		AttributeListType atl = new AttributeListType();
		xml.setAttributes(atl);
		for (Attribute att : dm.getAttributes()){
			atl.getAttributes().add(processAttribute(llookup, att));
		}
		
	}
	
	protected AttributeType processAttribute(HashMap<String, Language> llookup, Attribute att) {
		AttributeType at = new AttributeType();
		at.setIsrequired(att.getIsRequired());
		at.setKey(att.getKeyId());
		if (att.getIcon() != null) at.setIconkey(att.getIcon().getKeyId());
		
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
				if (item.getIcon() != null) ln.setIconkey(item.getIcon().getKeyId());
				setNames(ln.getNames(), item.getNames(), llookup);
				
				at.getValues().add(ln);
			}
		}
		
		setNames(at.getNames(), att.getNames(), llookup);
		
		return at;
	}

	protected void processTreeNode(AttributeTreeNode node,
			List<TreeNodeType> parentList, HashMap<String, Language> llookup) {

		TreeNodeType tnt = new TreeNodeType();
		tnt.setKey(node.getKeyId());
		setNames(tnt.getNames(), node.getNames(), llookup);
		tnt.setIsactive(node.getIsActive());
		parentList.add(tnt);
		if (node.getIcon() != null) tnt.setIconkey(node.getIcon().getKeyId());
		
		if (node.getChildren() != null) {
			for (AttributeTreeNode child : node.getChildren()) {
				processTreeNode(child, tnt.getChildrens(), llookup);
			}
		}
	}
	
	
	protected void setNames(List<NameType> list, Set<Label> names,
			HashMap<String, Language> llookup){
		
		if (names == null){
			return;
		}
		for (Label lbl: names){
			NameType nt = new NameType();
			nt.setValue(lbl.getValue());
			nt.setLanguageCode(llookup.get(lbl.getLanguage().getUuid().toString()).getCode());
			list.add(nt);
		}
	}
	
	protected HashMap<String, Language> processLanguages(SimpleDataModel dm, org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel xml){
		HashMap<String, Language> lookup = new HashMap<String, Language>();
		LanguageListType llt = new LanguageListType();
		xml.setLanguages(llt);
		for (Language ll : dm.getConservationArea().getLanguages()){
			LanguageType lt = new LanguageType();
			lt.setCode(ll.getCode());
			llt.getLanguages().add(lt);
			lookup.put(ll.getUuid().toString(), ll);
		}	
		return lookup;
		
	}
	
}
