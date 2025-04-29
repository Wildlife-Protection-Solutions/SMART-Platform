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
package org.wcs.smart.internal.ca.datamodel.xml.generate.v11;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.ca.datamodel.SimpleDataModel;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.internal.ca.datamodel.xml.IXmlToDataModelConverter;
import org.wcs.smart.internal.ca.datamodel.xml.XmlDataModelValidator;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

/**
 * Converts a SMART XML data model to the database
 * data model.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DataModelXmlToSimpleDataModelConverter implements IXmlToDataModelConverter{
	
	private HashMap<String, Language> langLookup;	
	private HashMap<String, Attribute> attributeLookUp;
	private List<Aggregation> aggs;
	private Locale l = null;
	private org.wcs.smart.internal.ca.datamodel.xml.generate.v11.DataModel xmlDataModel;
	private Collection<Icon> existingIcon;
	private Collection<IconSet> existingIconSets;
	private Path importDirectory;
	
	private Collection<Icon> newIcons;
	
	/**
	 * Converts xml data model into smart database data model
	 * with no target Conservation Area.	 * 
	 * 
	 *  @param is xml input stream
	 *  @param icons set of icons to map xml items to (can be null if no icons)
	 *  @param l current locale
	 * @throws JAXBException
	 * @throws ParseException
	 */
	public SimpleDataModel convert(InputStream is, Collection<Icon> icons, 
			Collection<IconSet> iconSets, Path importDirectory, Locale l) throws JAXBException, ParseException {
		this.l = l;
		this.existingIcon = icons;
		this.existingIconSets = iconSets;
		this.importDirectory = importDirectory;
		
		this.newIcons = new ArrayList<>();
		if (this.existingIcon == null) this.existingIcon = Collections.emptySet();
		langLookup = new HashMap<String, Language>();	
		aggs = new ArrayList<Aggregation>();

		//read xml file
		xmlDataModel = readDataModel(is, l);
		
		SimpleDataModel dm = new SimpleDataModel(null, Collections.<Category> emptyList(), Collections.<Attribute> emptyList());
		
		//read attributes
		attributeLookUp = getAttributes();
				
		//read categories
		if (xmlDataModel.getCategories() != null && xmlDataModel.getCategories().getCategory() != null ){			
			for (int i = 0; i < xmlDataModel.getCategories().getCategory().size(); i ++){
				Category newCategory = processCategory(xmlDataModel.getCategories().getCategory().get(i), i, null);
				dm.getCategories().add(newCategory);
			}
		}
		
		//attributes
		for (Attribute attribute : attributeLookUp.values()){
			if (!dm.getAttributes().contains(attribute)){
				dm.getAttributes().add(attribute);
			}
		}
		
		//need to create sub cat/attribute links for all attributes
		//this is new in 8.1.0
		List<Category> toProcess = new ArrayList<>();
		toProcess.addAll(dm.getCategories());
		List<Category> kids = new ArrayList<>();
		while(!toProcess.isEmpty()) {		
			Category working = toProcess.remove(0);
			toProcess.addAll(working.getChildren());
			for (CategoryAttribute ca : working.getRootAttributes()) {
				kids.clear();
				kids.addAll(ca.getCategory().getChildren());
				while(!kids.isEmpty()) {
					Category kid = kids.remove(0);
					kids.addAll(kid.getChildren());
					
					CategoryAttribute kidca = new CategoryAttribute();
					kidca.setCategory(kid);
					kidca.setAttribute(ca.getAttribute());
					kidca.setIsActive(ca.getIsActive());
					kidca.setIsRoot(false);
					kidca.setOrder(kid.getAllAttributes().size()+1);
					
					//insert before first root category
					int index = -1;
					for (int i = 0; i < kid.getAllAttributes().size(); i ++) {
						if (kid.getAllAttributes().get(i).getIsRoot()) {
							index = i;
							break;
						}
					}
					if (index < 0) {
						kid.getAllAttributes().add(kidca);
					}else {
						kid.getAllAttributes().add(index, kidca);
					}
				}
			}
		}
		//rest orders
		toProcess.clear();
		toProcess.addAll(dm.getCategories());
		while(!toProcess.isEmpty()) {		
			Category working = toProcess.remove(0);
			toProcess.addAll(working.getChildren());
			for (int i = 0; i < working.getAllAttributes().size(); i ++) {
				working.getAllAttributes().get(i).setOrder(i+1);
			}
		}
		
		XmlDataModelValidator validation = new XmlDataModelValidator(dm, l);
		validation.validate();
		
		return dm;
	}

	private DataModel readDataModel(InputStream file, Locale locale) throws JAXBException, ParseException{
		
		JAXBContext context = JAXBContext.newInstance(ObjectFactory.class);
		Unmarshaller un = context.createUnmarshaller();	
		@SuppressWarnings("unchecked")
		JAXBElement<DataModel> o = (JAXBElement<DataModel>) un.unmarshal(file);
		DataModel x = o.getValue();

		return x;
	}
	
	private void processIcon(DmObject object, String iconKey) {
		if (iconKey == null) return;
		
		//search existing icons for key
		for (Icon i : existingIcon) {
			if (i.getKeyId().equalsIgnoreCase(iconKey)) {
				object.setIcon(i);
				return;
			}
		}
		
		//check imported icons
		for (Icon i : newIcons) {
			if (i.getKeyId().equalsIgnoreCase(iconKey)) {
				object.setIcon(i);
				return;
			}
		}
		
		//nothing to import; could generate a warning
		if (importDirectory == null) return;
		
		//import the icon
		for (org.wcs.smart.internal.ca.datamodel.xml.generate.v11.Icon xmlIcon : xmlDataModel.getIcons()) {
			if (xmlIcon.getKey().equalsIgnoreCase(iconKey)) {
				//import this icon
				Icon newIcon = new Icon();
				newIcon.setKeyId(xmlIcon.getKey());
				newIcon.setFiles(new ArrayList<>());
				updateNames(newIcon, xmlIcon.getNames());
				
				for(org.wcs.smart.internal.ca.datamodel.xml.generate.v11.IconFile xmlIconFile : xmlIcon.getIcons()) {
					IconFile iconFile = new IconFile();
					iconFile.setIcon(newIcon);
					
					Path importFile = importDirectory.resolve(xmlIconFile.getFile());
					if (!Files.exists(importFile)) continue;
						
					iconFile.setCopyFromLocation(importFile);
					iconFile.setFilename(xmlIconFile.getFile());
					
					IconSet set = null;
					for (IconSet s : existingIconSets) {
						if (s.getKeyId().equalsIgnoreCase(xmlIconFile.getIconset())) {
							set = s;
							break;
						}
					}
					if (set == null) continue;
						
					iconFile.setIconSet(set);
					newIcon.getFiles().add(iconFile);
				}
				
				newIcons.add(newIcon);
				object.setIcon(newIcon);
			}
		}
	}
	
	
	private String getErrorMessage(I18NMessages msg) {
		return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(msg, l);
	}
	

	/*
	 * Processes an xml category
	 */
	private Category processCategory(CategoryType xmlCat, int order, Category parent) throws ParseException{
		
		Category newCategory = new Category();
		newCategory.setCategoryOrder(order);
		newCategory.setConservationArea(null);
		newCategory.setIsMultiple(xmlCat.isIsmultiple());
		newCategory.setParent(parent);
		newCategory.setKeyId(xmlCat.getKey());
		newCategory.updateHkey();
		newCategory.setUuid(null);	//to be generated by system
		newCategory.setIsActive(xmlCat.isIsactive());
		newCategory.setChildren(new ArrayList<>());
		newCategory.setAllAttributes(new ArrayList<>());
		
		processIcon(newCategory, xmlCat.getIconkey());
		
		/* Names */
		updateNames(newCategory, xmlCat.getNames());
		
		/* Attributes */
		List<CategoryAttributeLink> attributes = xmlCat.getAttributes();
		if (attributes != null && attributes.size() > 0){
		
			for (int i = 0; i < attributes.size(); i ++){
				CategoryAttributeLink attribute = attributes.get(i);
				Attribute att = attributeLookUp.get(attribute.getAttributekey());
				if (att == null){
					throw new ParseException(MessageFormat.format(getErrorMessage(I18NMessages.ATTRIBUTE_NOT_FOUND_ERROR), new Object[]{ attribute, xmlCat.getKey()}), 0);
				}
				CategoryAttribute ca = new CategoryAttribute(newCategory, att);
				ca.setOrder(i+1);
				ca.setIsRoot(true);
				ca.setIsActive(attribute.isIsactive());
				newCategory.getAllAttributes().add(ca);
				
			}
		}
		/* Children */
		List<CategoryType> children = xmlCat.getCategory();
		if (children != null && children.size() > 0){
			for (int i = 0; i < children.size(); i ++){
				Category newChild = processCategory(children.get(i), i, newCategory);
				newCategory.getChildren().add(newChild);
			}
		}
		return newCategory;
		
	}
	
	/*
	 * reads all attributes from the xml data model file
	 */
	private HashMap<String, Attribute> getAttributes() throws ParseException{
		HashMap<String, Attribute> attributeLookUp = new HashMap<String, Attribute>();
		
		if (xmlDataModel.getAttributes() == null || 
				xmlDataModel.getAttributes().getAttribute() == null || 
				xmlDataModel.getAttributes().getAttribute().size() == 0){
			//not attributes
			return attributeLookUp;
		}
		List<AttributeType> atts = xmlDataModel.getAttributes().getAttribute();
		for (AttributeType xmlAtt : atts) {
			
			Attribute newAttribute = new Attribute();
			newAttribute.setAggregations(new ArrayList<>());
			newAttribute.setConservationArea(null);
			newAttribute.setIsRequired(xmlAtt.isIsrequired());
			newAttribute.setKeyId(xmlAtt.getKey());
			newAttribute.setType(parseAttributeType(xmlAtt.getType()));
			
			processIcon(newAttribute, xmlAtt.getIconkey());
			
			/* Names */
			updateNames(newAttribute, xmlAtt.getNames());
			
			/* QA Fields */
			if (xmlAtt.getQaMinmax() != null){
				if (xmlAtt.getQaMinmax().getMinValue() != null){
					newAttribute.setMinValue(xmlAtt.getQaMinmax().getMinValue());	
				}
				if (xmlAtt.getQaMinmax().getMaxValue() != null){
					newAttribute.setMaxValue(xmlAtt.getQaMinmax().getMaxValue());
				}
			}	
			if (xmlAtt.getQaRegex() != null){
				newAttribute.setRegex(xmlAtt.getQaRegex());
			}
			
			/* Attribute List */
			if (newAttribute.getType() == org.wcs.smart.ca.datamodel.Attribute.AttributeType.LIST || 
					newAttribute.getType() == org.wcs.smart.ca.datamodel.Attribute.AttributeType.MLIST ) {
				newAttribute.setAttributeList(new ArrayList<AttributeListItem>());
				if (xmlAtt.getValues() != null){
					List<ListNode> items = xmlAtt.getValues();
					for (int i = 0; i < items.size(); i ++){
						ListNode item = items.get(i);
						AttributeListItem newItem = new AttributeListItem();
						newItem.setKeyId(item.getKey());
						newItem.setListOrder(i);
						updateNames(newItem, item.getNames());
						newItem.setUuid(null);
						newItem.setAttribute(newAttribute);
						newItem.setIsActive(item.isIsactive());
						processIcon(newItem, item.getIconkey());
						newAttribute.getAttributeList().add(newItem);		
					}
				}
			}
			
			/* Tree List */
			if (newAttribute.getType() == org.wcs.smart.ca.datamodel.Attribute.AttributeType.TREE) {
				newAttribute.setTree(new ArrayList<AttributeTreeNode>());
				List<TreeNodeType> rootNodes = xmlAtt.getTree();
				if (rootNodes != null){
					for (int i = 0; i < rootNodes.size(); i ++){
						AttributeTreeNode newNode = processAttributeTreeNode(null, newAttribute, rootNodes.get(i));
						newNode.setNodeOrder(i);
						newAttribute.getTree().add(newNode);
					}
				}
			}
			
			
			/* Aggregations */
			List<AggregationType> aggs = xmlAtt.getAggregations();
			if (aggs != null && aggs.size() > 0){
				newAttribute.setAggregations(new ArrayList<Aggregation>());
				for (AggregationType agg : aggs){
					newAttribute.getAggregations().add(lookUpAggregation(agg.getAggregation()));
				}
			}

			
			newAttribute.setUuid(null);	
			attributeLookUp.put(xmlAtt.getKey(), newAttribute);
		}
		return attributeLookUp;
	}
	
	private AttributeTreeNode processAttributeTreeNode(AttributeTreeNode parent, Attribute parentAttribute, TreeNodeType xmlNode) {		
		AttributeTreeNode newAttributeTreeNode = new AttributeTreeNode();
		newAttributeTreeNode.setAttribute(parentAttribute);
		newAttributeTreeNode.setKeyId(xmlNode.getKey());
		processIcon(newAttributeTreeNode, xmlNode.getIconkey());
		updateNames(newAttributeTreeNode, xmlNode.getNames());
		newAttributeTreeNode.setParent(parent);
		newAttributeTreeNode.setUuid(null);
		newAttributeTreeNode.setIsActive(xmlNode.isIsactive());
		if (xmlNode.getChildren() != null && xmlNode.getChildren().size() > 0) {
			newAttributeTreeNode.setChildren(new ArrayList<AttributeTreeNode>());
			for (int i = 0; i < xmlNode.getChildren().size(); i ++){			
				AttributeTreeNode newChild = processAttributeTreeNode(newAttributeTreeNode, parentAttribute, xmlNode.getChildren().get(i));
				newChild.setNodeOrder(i);
				newAttributeTreeNode.getChildren().add(newChild);
			}
		}
		newAttributeTreeNode.updateHkey();
		return newAttributeTreeNode;
	}
	
	/*
	 * Looks up aggregation from list
	 */
	private Aggregation lookUpAggregation(String name) throws ParseException{
		
		for (Aggregation agg : aggs){
			if (agg.getName().equals(name)){
				return agg;
			}
		}
		Aggregation agg = new Aggregation();
		agg.setName(name);
		aggs.add(agg);
		return agg;
	}
	
	/*
	 * Determines attribute type
	 */
	private Attribute.AttributeType parseAttributeType(String xmlType) throws ParseException{
		Attribute.AttributeType newtype = Attribute.decodeAttributeType(xmlType);
		if (newtype != null){
			return newtype;
		}
		throw new ParseException(MessageFormat.format(getErrorMessage(I18NMessages.ATTRIBUTE_TYPE_NOT_SUPPORTED), xmlType), 0);
	}
	
	/*
	 * updates the names associated with a data model object
	 */
	private void updateNames(NamedItem dmobject, List<NameType> names){
		for (NameType nameType : names) {
			String code = nameType.getLanguageCode();
			String value = nameType.getValue();
			
			Language lang = langLookup.get(code);
			if (lang == null) {
				lang = new Language();
				lang.setCode(code);
				langLookup.put(lang.getCode(), lang);
				
			}
			//if language not found we ignore; this should be dealt with earlier
			dmobject.updateName(lang, value);
		}	
	}
	
}
