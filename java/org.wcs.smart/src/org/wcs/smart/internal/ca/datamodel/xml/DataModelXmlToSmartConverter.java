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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Aggregation;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AggregationType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.CategoryAttributeLink;
import org.wcs.smart.internal.ca.datamodel.xml.generate.CategoryType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.LanguageType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.ListNode;
import org.wcs.smart.internal.ca.datamodel.xml.generate.NameType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.TreeNodeType;
import org.wcs.smart.ui.internal.ca.LanguageSelectionDialog;

/**
 * Converts a SMART XML data model to the database
 * data model.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DataModelXmlToSmartConverter {

	private HashMap<String, Language> langLookup;	
	private HashMap<String, Attribute> attributeLookUp;

	private List<Aggregation> aggs;
	
	private String useAsDefault = null;
	
	private org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel xmlDataModel;
	private ConservationArea targetCa = null;
	
	/**
	 * Converts xml data model into smart database data model
	 * 
	 * 
	 * Requires that ca.getlanguages is called or an open hibernate session
	 * with ca attached is available to lazily load languages
	 * <p>
	 * If synchronizing then there must be an active display.
	 * </p>
	 * <p>
	 * Synchronizing data model languages has two affects:
	 * </p>
	 * <p>
	 * 1. If the target conservation area default language is
	 * not in the data model it prompts the user to
	 * select which language to use in the data model as the same as the default language.
	 * This will cause the labels from that language to be copied into the
	 * default language labels (for the conservation area).
	 * </p>
	 * <p>
	 * 2. If a language in the data model does not exist in the conservation area the user
	 * is prompted to add the language to the target conservation area.
	 * </p> 
	 * @param is input stream of xml data model
	 * @param targetCa conservation area
	 * @param syncLanguages if the languages of the data model and conservation should be synchronized
	 * @return data model generated from xml data model
	 * 
	 *  
	 * @throws JAXBException
	 * @throws ParseException
	 */
	public DataModel convert(InputStream is, ConservationArea targetCa, boolean syncLanguages) throws JAXBException, ParseException {
		this.targetCa = targetCa;
		useAsDefault = null;	

		aggs = DataModel.getAggregations();

		//read xml file
		xmlDataModel = XmlSmartDataModelManager.readDataModel(is);
		
		// create new db data model
		getLanguages();  //this probably requires a db session
		
		if (syncLanguages){
			//here we check to ensure default ca lang
			useAsDefault = checkLanguage(xmlDataModel.getLanguages().getLanguages(), targetCa);
			if (useAsDefault == null){
				return null;
			}
		}
		
		DataModel dm = new DataModel(targetCa, Collections.<Category> emptyList(), Collections.<Attribute> emptyList());
				
		//read attributes
		attributeLookUp = getAttributes(xmlDataModel);
				
		//read categories
		if (xmlDataModel.getCategories() != null && xmlDataModel.getCategories().getCategories() != null ){			
			for (int i = 0; i < xmlDataModel.getCategories().getCategories().size(); i ++){
				Category newCategory = processCategory(xmlDataModel.getCategories().getCategories().get(i), i, null);
				dm.addRootCategories(newCategory);
			}
		}
		
		//there may be attributes linked to no categories
		for (Attribute attribute : attributeLookUp.values()){
			if (!dm.getAttributes().contains(attribute)){
				try{
					dm.addNewAttribute(attribute, null);
				}catch (Exception ex){
					SmartPlugIn.log(ex.getMessage(), ex);
				}
			}
		}
		
//		validate(dm);
		return dm;
	}
	
//	/**
//	 * Performs some validation on the imported data model keys and names
//	 * @param dm
//	 * @throws ParseException 
//	 */
//	private void validate(DataModel dm) throws ParseException {
//		// we need to do some validation here on keys and names
//		for (Attribute attribute : dm.getAttributes()) {
//			validate(attribute, dm.getAttributes());
//			
//			if (attribute.getType() == org.wcs.smart.ca.datamodel.Attribute.AttributeType.LIST){
//				for (AttributeListItem li : attribute.getAttributeList()){
//					validate(li, attribute.getAttributeList());
//				}	
//			}
//			
//			if (attribute.getType() == org.wcs.smart.ca.datamodel.Attribute.AttributeType.TREE){
//				List<AttributeTreeNode> toCheck = new ArrayList<AttributeTreeNode>();
//				toCheck.addAll(attribute.getTree());
//				while (toCheck.size() > 0) {
//					AttributeTreeNode tn = toCheck.remove(0);
//					List<AttributeTreeNode> siblings = null;
//					if (tn.getParent() != null) {
//						siblings = tn.getParent().getChildren();
//					}else{
//						siblings = attribute.getTree();
//					}
//					if (siblings == null) {
//						siblings = new ArrayList<AttributeTreeNode>();
//					}
//					if (tn.getChildren() != null) {
//						toCheck.addAll(tn.getChildren());
//					}
//					validate(tn, siblings);
//				}
//			}
//		}
//		List<Category> toCheck = new ArrayList<Category>();
//		toCheck.addAll(dm.getCategories());
//		while (toCheck.size() > 0) {
//			Category c = toCheck.remove(0);
//			List<Category> siblings = null;
//			if (c.getParent() != null) {
//				siblings = c.getParent().getChildren();
//			}else{
//				siblings = dm.getCategories();
//			}
//			if (siblings == null) {
//				siblings = new ArrayList<Category>();
//			}
//			if (c.getChildren() != null) {
//				toCheck.addAll(c.getChildren());
//			}
//			validate(c, siblings);
//		}
//	}
//	
//	private void validate(DmObject object, List<? extends DmObject> siblings) throws ParseException{
//		String error = DataModel.validateKey(object.getKeyId(), siblings);
//		if (error != null) {
//			throw new ParseException(MessageFormat.format(
//					"Data model object key {0} is invalid. {1}", new Object[] {
//							object.getKeyId(), error }), 0);
//		}
//		for (org.wcs.smart.ca.Label l : object.getNames()) {
//			error = DataModel.validateName(l.getValue(), l.getLanguage());
//			if (error != null) {
//				throw new ParseException(MessageFormat.format(
//						"Data model object name {0} is invalid. {1}", new Object[] {
//								l.getValue(), error }), 0);
//			}
//		}
//	}
//	
	/**
	 * Returns the language code to use as the default language.
	 * <p>
	 * Any labels with the selected language code should be applied 
	 * to the default language of the conservation area.
	 * </p>
	 * @param xmlLanguages
	 * @param targetCa
	 * @return null if should not continue (no default language found); otherwise
	 * the language code to use as the default ca language
	 */
	public static String checkLanguage(List<LanguageType> xmlLanguages, final ConservationArea targetCa){
		//here we check to ensure default ca lang
		for (LanguageType lt : xmlLanguages){
			if (lt.getCode().equals(targetCa.getDefaultLanguage().getCode())){
				return lt.getCode();
			}
		}
		
		final String[] values = new String[xmlLanguages.size()];
		for (int i = 0; i < values.length; i ++){
			values[i] = xmlLanguages.get(i).getCode();
		}
		final String[] selected = new String[1];
		selected[0] = null;
		Display.getDefault().syncExec(new Runnable(){

			@Override
			public void run() {
				LanguageSelectionDialog sd = new LanguageSelectionDialog(Display.getDefault().getActiveShell(), targetCa, values);
				if (sd.open() != IDialogConstants.OK_ID){
					selected[0] = null;
				}else{
					selected[0] = (String)((StructuredSelection)sd.getSelection()).getFirstElement();
				}
				
			}});
		
		
		return selected[0];
	}
	
	/**
	 * Converts an xml data model file into a SMART
	 * datamodel file.
	 * 
	 * @param file The xml data model
	 * @return SMART db datamodel
	 * 
	 * @throws JAXBException
	 * @throws ParseException
	 * @throws IOException 
	 */
	public DataModel convert(File file, ConservationArea targetCa, boolean synchronizeLang) throws JAXBException, ParseException, IOException {
		try(FileInputStream is = new FileInputStream(file)){
			return convert(is, targetCa, synchronizeLang);
		}
	}
	
	/*
	 * Initializes the language lookup list from the conservation area.
	 */
	private void getLanguages(){
		langLookup = new HashMap<String, Language>();
		for (Language lang : this.targetCa.getLanguages()) {
			langLookup.put(lang.getCode(), lang);
		}
	}

	/*
	 * Processes an xml category
	 */
	private Category processCategory(CategoryType xmlCat, int order, Category parent) throws ParseException{
		
		Category newCategory = new Category();
		newCategory.setCategoryOrder(order);
		newCategory.setConservationArea(this.targetCa);
		newCategory.setIsMultiple(xmlCat.isIsmultiple());
		newCategory.setParent(parent);
		newCategory.setKeyId(xmlCat.getKey());
		newCategory.updateHkey();
		newCategory.setUuid(null);	//to be generated by system
		newCategory.setIsActive(xmlCat.isIsactive());
		
		/* Names */
		updateNames(newCategory, xmlCat.getNames());
		
		/* Attributes */
		List<CategoryAttributeLink> attributes = xmlCat.getAttributes();
		if (attributes != null && attributes.size() > 0){
			newCategory.setAttributes(new ArrayList<CategoryAttribute>());
		
			for (int i = 0; i < attributes.size(); i ++){
				CategoryAttributeLink attribute = attributes.get(i);
				Attribute att = attributeLookUp.get(attribute.getAttributekey());
				if (att == null){
					throw new ParseException(MessageFormat.format(Messages.DataModelXmlToSmartConverter_Error_CategoryAttributeNotFound, new Object[]{ attribute, xmlCat.getKey()}), 0);
				}
				CategoryAttribute ca = new CategoryAttribute(newCategory, att);
				ca.setOrder(i);
				newCategory.getAttributes().add(ca);
				ca.setIsActive(attribute.isIsactive());
			}
		}
		/* Children */
		List<CategoryType> children = xmlCat.getCategories();
		if (children != null && children.size() > 0){
			newCategory.setChildren(new ArrayList<Category> ());
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
	private HashMap<String, Attribute> getAttributes(org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel dataModel) throws ParseException{
		HashMap<String, Attribute> attributeLookUp = new HashMap<String, Attribute>();
		
		if (dataModel.getAttributes() == null || dataModel.getAttributes().getAttributes() == null || dataModel.getAttributes().getAttributes().size() == 0){
			//not attributes
			return attributeLookUp;
		}
		List<AttributeType> atts = dataModel.getAttributes().getAttributes();
		for (AttributeType xmlAtt : atts) {
			
			Attribute newAttribute = new Attribute();
			
			newAttribute.setConservationArea(this.targetCa);
			newAttribute.setIsRequired(xmlAtt.isIsrequired());
			newAttribute.setKeyId(xmlAtt.getKey());
			
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
			if (xmlAtt.getValues() != null){
				List<ListNode> items = xmlAtt.getValues();
				if (items.size() > 0){
					newAttribute.setAttributeList(new ArrayList<AttributeListItem>());
				}
				for (int i = 0; i < items.size(); i ++){
					ListNode item = items.get(i);
					AttributeListItem newItem = new AttributeListItem();
					newItem.setKeyId(item.getKey());
					newItem.setListOrder(i);
					updateNames(newItem, item.getNames());
					newItem.setUuid(null);
					newItem.setAttribute(newAttribute);
					newItem.setIsActive(item.isIsactive());
					newAttribute.getAttributeList().add(newItem);
					
				}
			}
			
			/* Tree List */
			List<TreeNodeType> rootNodes = xmlAtt.getTrees();
			if (rootNodes != null && rootNodes.size() > 0){
				newAttribute.setTree(new ArrayList<AttributeTreeNode>());
				for (int i = 0; i < rootNodes.size(); i ++){
					AttributeTreeNode newNode = processAttributeTreeNode(null, newAttribute, rootNodes.get(i));
					newNode.setNodeOrder(i);
					newAttribute.getTree().add(newNode);
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

			newAttribute.setType(parseAttributeType(xmlAtt.getType()));
			newAttribute.setUuid(null);	
			attributeLookUp.put(xmlAtt.getKey(), newAttribute);
		}
		return attributeLookUp;
	}
	
	private AttributeTreeNode processAttributeTreeNode(AttributeTreeNode parent, Attribute parentAttribute, TreeNodeType xmlNode) {		
		AttributeTreeNode newAttributeTreeNode = new AttributeTreeNode();
		newAttributeTreeNode.setAttribute(parentAttribute);
		newAttributeTreeNode.setKeyId(xmlNode.getKey());
		updateNames(newAttributeTreeNode, xmlNode.getNames());
		newAttributeTreeNode.setParent(parent);
		newAttributeTreeNode.setUuid(null);
		newAttributeTreeNode.setIsActive(xmlNode.isIsactive());
		if (xmlNode.getChildrens() != null && xmlNode.getChildrens().size() > 0) {
			newAttributeTreeNode.setChildren(new ArrayList<AttributeTreeNode>());
			for (int i = 0; i < xmlNode.getChildrens().size(); i ++){			
				AttributeTreeNode newChild = processAttributeTreeNode(newAttributeTreeNode, parentAttribute, xmlNode.getChildrens().get(i));
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
		throw new ParseException(Messages.DataModelXmlToSmartConverter_Error_AggregationNotFound + name, 0);
	}
	
	/*
	 * Determines attribute type
	 */
	private Attribute.AttributeType parseAttributeType(String xmlType) throws ParseException{
		Attribute.AttributeType newtype = Attribute.decodeAttributeType(xmlType);
		if (newtype != null){
			return newtype;
		}
		throw new ParseException(Messages.DataModelXmlToSmartConverter_Error_AttributeTypeNotFound + xmlType, 0);
	}
	
	/*
	 * updates the names associated with a data model object
	 */
	private void updateNames(DmObject dmobject, List<NameType> names){
		for (NameType nameType : names) {
			String code = nameType.getLanguageCode();
			String value = nameType.getValue();
			
			Language lang = langLookup.get(code);
			//if language not found we ignore; this should be dealt with earlier
			if (lang != null){
				dmobject.updateName(lang, value);
			}
			
			if (useAsDefault != null && useAsDefault.equals(nameType.getLanguageCode())){
				dmobject.updateName(targetCa.getDefaultLanguage(), value);
				
			}
		}
		
	}
}
