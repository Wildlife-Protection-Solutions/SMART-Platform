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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.CategoryType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel;
import org.wcs.smart.internal.ca.datamodel.xml.generate.ListNode;
import org.wcs.smart.internal.ca.datamodel.xml.generate.NameType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.TreeNodeType;

/**
 * Validates an xml data model.  Validations includes: ensuring
 * keys are the correct format and unique as required, 
 * names are in the correct formats.
 * 
 * @author Emily
 *
 */
public class XmlDataModelValidator {

	private DataModel dm;
	
	public XmlDataModelValidator(DataModel dm){
		this.dm = dm;
	}
	
	/**
	 * Performs some validation on the imported data model keys and names
	 * @param dm
	 * @throws ParseException if error found in data model 
	 */
	public void validate() throws ParseException {
		// we need to do some validation here on keys and names
		if (dm.getAttributes() != null){
			List<DmObjectWrapper> attributes = new ArrayList<DmObjectWrapper>();
			for (AttributeType attribute : dm.getAttributes().getAttributes()) {
				DmObjectWrapper wrap = new DmObjectWrapper();
				wrap.setKeyId(attribute.getKey());
				attributes.add(wrap);
			}
			for (AttributeType attribute : dm.getAttributes().getAttributes()) {
				validate(attribute.getKey(), attribute.getNames(), attributes);
			
				if (Attribute.decodeAttributeType(attribute.getType()) == Attribute.AttributeType.LIST){
					List<DmObjectWrapper> list = new ArrayList<DmObjectWrapper>();
					for (ListNode li : attribute.getValues()){
						DmObjectWrapper wrap = new DmObjectWrapper();
						wrap.setKeyId(li.getKey());
						list.add(wrap);
					}
				
					for (ListNode li : attribute.getValues()){
						validate(li.getKey(), li.getNames(), list);
					}	
				}
			
				if (Attribute.decodeAttributeType(attribute.getType()) == Attribute.AttributeType.TREE){
					List<DmObjectWrapper> roots = new ArrayList<DmObjectWrapper>();
					for (TreeNodeType tn : attribute.getTrees()){
						DmObjectWrapper wrap = new DmObjectWrapper();
						wrap.setKeyId(tn.getKey());
						roots.add(wrap);
					}
					for (TreeNodeType tn : attribute.getTrees()){
						validate(tn, roots);
					}
				}
			}
		}
		
		if (dm.getCategories() != null){
			List<DmObjectWrapper> roots = new ArrayList<DmObjectWrapper>();
			for (CategoryType cat : dm.getCategories().getCategories()){
				DmObjectWrapper wrap = new DmObjectWrapper();
				wrap.setKeyId(cat.getKey());
				roots.add(wrap);
			}
		
			for (CategoryType cat : dm.getCategories().getCategories()){
				validate(cat, roots);
			}
		}
	}
	
	private void validate(TreeNodeType toValidate, List<DmObjectWrapper> siblings ) throws ParseException{
		validate(toValidate.getKey(), toValidate.getNames(), siblings);
		
		List<DmObjectWrapper> kids = new ArrayList<DmObjectWrapper>();
		for(TreeNodeType kid : toValidate.getChildrens()){
			DmObjectWrapper wrap = new DmObjectWrapper();
			wrap.setKeyId(kid.getKey());
			kids.add(wrap);
		}
		
		for(TreeNodeType kid : toValidate.getChildrens()){
			validate(kid, kids);
		}
	}
	
	private  void validate(CategoryType toValidate, List<DmObjectWrapper> siblings ) throws ParseException{
		validate(toValidate.getKey(), toValidate.getNames(), siblings);
		
		List<DmObjectWrapper> kids = new ArrayList<DmObjectWrapper>();
		for(CategoryType kid : toValidate.getCategories()){
			DmObjectWrapper wrap = new DmObjectWrapper();
			wrap.setKeyId(kid.getKey());
			kids.add(wrap);
		}
		
		for(CategoryType kid : toValidate.getCategories()){
			validate(kid, kids);
		}
	}
	
	private void validate(String key, List<NameType> names, List<DmObjectWrapper> siblings) throws ParseException{

		List<DmObjectWrapper> kids = new ArrayList<DmObjectWrapper>();
		kids.addAll(siblings);
		for (DmObjectWrapper wrap : kids){
			if (wrap.getKeyId().equals(key)){
				kids.remove(wrap);
				break;
			}
		}
		String error = DataModelManager.INSTANCE.validateKey(key, kids);
		if (error != null) {
			throw new ParseException(MessageFormat.format(
					Messages.XmlDataModelValidator_InvalidKey, new Object[] {
							key, error }), 0);
		}
	
		for (NameType nt : names) {
			Language l = new Language();
			l.setCode(nt.getLanguageCode());
			error = org.wcs.smart.ca.datamodel.DataModel.validateName(nt.getValue(), l);
			if (error != null) {
				throw new ParseException(MessageFormat.format(
					Messages.XmlDataModelValidator_InvalidName, new Object[] {
							nt.getValue(), error }), 0);
			}
			
		}
	}
	
	class DmObjectWrapper extends DmObject{
		public DmObjectWrapper(){
			super();
		}
	}
}


