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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
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
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.ca.datamodel.SimpleDataModel;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v12.AggregationType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v12.AttributeListType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v12.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v12.CategoryAttributeLink;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v12.CategoryType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v12.CategoryTypeList;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v12.DataModel;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v12.LanguageListType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v12.LanguageType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v12.ListNode;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v12.MinMaxType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v12.NameType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.v12.TreeNodeType;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;


/**
 * Converts a database data model to the xml representation 
 * of the data model.
 * 
 * @author egouge
 *
 */
public class DataModelToXmlConverter {
	
	public enum IconOption{
		NONE,
		CUSTOM,
		ALL;
		
		public String getFileType() {
			if (this == NONE) return "xml"; //$NON-NLS-1$
			return "zip"; //$NON-NLS-1$
		}
	}
	
	protected Set<Path> iconFiles; 
	protected IconOption includeIconFiles;
	
	protected org.wcs.smart.internal.ca.datamodel.xml.generate.v12.DataModel xml;
	protected SimpleDataModel dm;
	protected HashMap<String, Language> llookup;
	
	private Set<String> addedIcons;
	
	/**
	 * 
	 * @param includeIconFiles which if any icon files are to be included in the export
	 */
	public DataModelToXmlConverter(IconOption includeIconFiles) {
		iconFiles = new HashSet<>();
		this.includeIconFiles = includeIconFiles;
		addedIcons = new HashSet<>();
	}
	
	/**
	 * 
	 * @return list of icon files to include in export
	 */
	public Set<Path> getIconFiles(){
		return this.iconFiles;
	}
	
	/**
	 * Converts an smart model to xml model 
	 * @param dm smart data model
	 * @param monitor progress monitor
	 * @return xml data model
	 */
	public org.wcs.smart.internal.ca.datamodel.xml.generate.v12.DataModel convert(SimpleDataModel dm) {
		this.dm = dm;
		this.xml = new org.wcs.smart.internal.ca.datamodel.xml.generate.v12.DataModel();
		llookup = processLanguages();
		processAttributes();
		processCategories();
		return xml;
	}
	
	protected void processCategories(){
	
		if (dm.getCategories() != null){
			CategoryTypeList ctl = new CategoryTypeList();
			xml.setCategories(ctl);
			for (Category child : dm.getCategories()){
				processCategory(child, ctl.getCategory());
			}
		}
	}
	
	protected void processCategory(Category child, List<CategoryType> parentList){
		
		CategoryType ct = new CategoryType();
		setNames(ct.getNames(), child.getNames());
		ct.setIsactive(child.getIsActive());
		ct.setIsmultiple(child.getIsMultiple());
		ct.setKey(child.getKeyId());
		if (child.getIcon() != null) ct.setIconkey(child.getIcon().getKeyId());
		
		
		if (child.getAllAttributes() != null){
			for (CategoryAttribute map : child.getAllAttributes()){
				CategoryAttributeLink link = new CategoryAttributeLink();
				link.setAttributekey(map.getAttribute().getKeyId());
				link.setIsactive(map.getIsActive());
				link.setIsroot(map.getIsRoot());
				ct.getAttributes().add(link);
			}
		}
		if (child.getChildren() != null){
			for (Category c : child.getChildren()){
				processCategory(c, ct.getCategory());
			}
		}
		parentList.add(ct);
		processIcon(child);
				
	}
	
	
	protected void processAttributes(){
		
		AttributeListType atl = new AttributeListType();
		xml.setAttributes(atl);
		for (Attribute att : dm.getAttributes()){
			atl.getAttribute().add(processAttribute(att));
		}
		
	}
	
	protected AttributeType processAttribute(Attribute att) {
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
		
		processIcon(att);
		if (att.getAggregations() != null){
			for (Aggregation agg : att.getAggregations()){
				AggregationType agt = new AggregationType();
				agt.setAggregation(agg.getName());
				at.getAggregations().add(agt);
			}
		}
		
		
		if (att.getTree() != null){
			for (AttributeTreeNode child: att.getTree()){
				processTreeNode(child, at.getTree());
			}
		}
		if (att.getAttributeList() != null){
			for (AttributeListItem item : att.getAttributeList()){
				ListNode ln = new ListNode();
				ln.setKey(item.getKeyId());
				ln.setIsactive(item.getIsActive());
				if (item.getIcon() != null) ln.setIconkey(item.getIcon().getKeyId());
				setNames(ln.getNames(), item.getNames());
				
				at.getValues().add(ln);
				processIcon(item);
			}
		}
		
		setNames(at.getNames(), att.getNames());
		
		return at;
	}

	protected void processTreeNode(AttributeTreeNode node,
			List<TreeNodeType> parentList) {

		TreeNodeType tnt = new TreeNodeType();
		tnt.setKey(node.getKeyId());
		setNames(tnt.getNames(), node.getNames());
		tnt.setIsactive(node.getIsActive());
		parentList.add(tnt);
		if (node.getIcon() != null) tnt.setIconkey(node.getIcon().getKeyId());
		processIcon(node);
		if (node.getChildren() != null) {
			for (AttributeTreeNode child : node.getChildren()) {
				processTreeNode(child, tnt.getChildren());
			}
		}
		
	}
	
	
	protected void setNames(List<NameType> list, Set<Label> names){
		
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
	
	protected HashMap<String, Language> processLanguages(){
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
	
	
	protected void processIcon(DmObject object) {
		if (this.includeIconFiles == IconOption.NONE) return;
		
		if (object.getIcon() == null) return;
		
		boolean issystem = true;
		for (IconFile iconfile : object.getIcon().getFiles()) {
			if (!iconfile.isSystemIcon()) {
				issystem = false;
				break;
			}
		}
		//all icons are system icon so we don't need to export it 
		if (issystem && this.includeIconFiles == IconOption.CUSTOM) return;
		
		//already added
		if (addedIcons.contains(object.getIcon().getKeyId())) return;
		
		//add icons to data model
		org.wcs.smart.internal.ca.datamodel.xml.generate.v12.Icon xmlIcon = new org.wcs.smart.internal.ca.datamodel.xml.generate.v12.Icon();
		xml.getIcons().add(xmlIcon);
		xmlIcon.setIssystem(issystem);
		xmlIcon.setKey(object.getIcon().getKeyId());
		addedIcons.add(object.getIcon().getKeyId());
		//names
		setNames(xmlIcon.getNames(), object.getIcon().getNames());
		
		for (IconFile iconFile : object.getIcon().getFiles()) {
			org.wcs.smart.internal.ca.datamodel.xml.generate.v12.IconFile xmlIconFile = new org.wcs.smart.internal.ca.datamodel.xml.generate.v12.IconFile();
		
			xmlIconFile.setIconset( iconFile.getIconSet().getKeyId() );
			xmlIconFile.setFile(  iconFile.getAttachmentFile().getFileName().toString() );
			
			xmlIcon.getIcons().add(xmlIconFile);
		
			if ((this.includeIconFiles == IconOption.ALL) ||
					(this.includeIconFiles == IconOption.CUSTOM && !iconFile.isSystemIcon())){
				iconFiles.add(iconFile.getAttachmentFile());
			}
		}
		
	}
	
	/**
	 * Writes a data model to an xml file.
	 * <p>
	 * User is required to close output stream.
	 * </p>
	 * @param model
	 * @param file
	 * @throws JAXBException
	 * @throws IOException
	 */
	public static void writeDataModel(DataModel model, OutputStream file) throws JAXBException, IOException{
		JAXBContext context = JAXBContext.newInstance(org.wcs.smart.internal.ca.datamodel.xml.generate.v12.ObjectFactory.class);
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.marshal(model, file);
	}
}
