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

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.internal.Messages;
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
public class DataModelSmartToXmlConverter {

	
	/**
	 * Converts an smart model to xml model 
	 * @param dm smart data model
	 * @param monitor progress monitor
	 * @return xml data model
	 */
	public static org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel convert(DataModel dm, IProgressMonitor monitor) {
		
		monitor.beginTask(Messages.DataModelSmartToXmlConverter_Progress_convertingDm, 3);
		org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel xml = new org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel();
		
		monitor.subTask(Messages.DataModelSmartToXmlConverter_Progress_Languages);
		HashMap<String, Language> llookup = processLanguages(dm, xml);
		monitor.worked(1);
		monitor.subTask(Messages.DataModelSmartToXmlConverter_Progress_Attributes);
		processAttribute(dm, xml, llookup, monitor);
		monitor.worked(1);
		monitor.subTask(Messages.DataModelSmartToXmlConverter_ProgressCategories);
		processCategories(dm, xml, llookup, monitor);
		monitor.worked(1);
		return xml;
		
		
	}
	private static void processCategories(DataModel dm, 
			org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel xml, 
			HashMap<String, Language> llookup, IProgressMonitor monitor){
	
		if (dm.getCategories() != null){
			CategoryTypeList ctl = new CategoryTypeList();
			xml.setCategories(ctl);
			for (Category child : dm.getCategories()){
				processCategory(child, ctl.getCategories(), llookup, monitor);
			}
		}
	}
	
	private static void processCategory(Category child, List<CategoryType> parentList, 
			HashMap<String, Language> llookup, IProgressMonitor monitor ){
		
		monitor.subTask(MessageFormat.format(Messages.DataModelSmartToXmlConverter_ProgressCategory, new Object[]{child.getName()}));
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
				processCategory(c, ct.getCategories(), llookup, monitor);
			}
		}
		parentList.add(ct);
				
	}
	
	
	private static void processAttribute(DataModel dm, 
			org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel xml, 
			HashMap<String, Language> llookup,
			IProgressMonitor monitor){
		
		AttributeListType atl = new AttributeListType();
		xml.setAttributes(atl);
		for (Attribute att : dm.getAttributes()){
			monitor.subTask(MessageFormat.format(Messages.DataModelSmartToXmlConverter_ProgressAttribute, new Object[]{att.getName()}));
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
	
	
	private static void setNames(List<NameType> list, Set<Label> names,
			HashMap<String, Language> llookup){
		
		if (names == null){
			return;
		}
		for (Label lbl: names){
			NameType nt = new NameType();
			nt.setValue(lbl.getValue());
//			nt.setLanguageCode(llookup.get(new String(lbl.getLanguage().getUuid())).getCode());
			nt.setLanguageCode(llookup.get(lbl.getLanguage().getUuid().toString()).getCode());
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
//			lookup.put(new String(ll.getUuid()), ll);
			lookup.put(ll.getUuid().toString(), ll);
		}	
		return lookup;
		
	}
	
}
