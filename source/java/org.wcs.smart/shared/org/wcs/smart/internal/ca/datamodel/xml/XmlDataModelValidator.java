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
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.ca.datamodel.SimpleDataModel;


/**
 * Validates an xml data model.  Validations includes: ensuring
 * keys are the correct format and unique as required, 
 * names are in the correct formats.
 * 
 * @author Emily
 *
 */
public class XmlDataModelValidator {

	public static enum I18NMessages{
		INVALID_KEY,
		INVALID_NAME
	}
	
	private SimpleDataModel dm;
	private Locale locale;
	
	private ParseException exception;
	
	public XmlDataModelValidator(SimpleDataModel dm, Locale locale){
		this.dm = dm;
		this.locale = locale;
	}
	
	/**
	 * Performs some validation on the imported data model keys and names
	 * @param dm
	 * @throws ParseException if error found in data model 
	 */
	public void validate() throws ParseException {
		// we need to do some validation here on keys and names
		exception = null;
		if (dm.getAttributes() != null){

			for (Attribute attribute : dm.getAttributes()) {
				validate(attribute, dm.getAttributes());
			
				if (attribute.getType() == AttributeType.LIST) {
					for (AttributeListItem li : attribute.getAttributeList()) {
						validate(li, attribute.getAttributeList());
					}
				}
			
				if (attribute.getType() == AttributeType.TREE) {
					for (AttributeTreeNode node : attribute.getTree()) {
						node.accept(n->{
							try {
								validate((DmObject)n, n.getParent() == null? attribute.getTree() : n.getParent().getChildren());
							}catch (ParseException ex) {
								exception = ex;
								return false;
							}
							return true;
						});
						if (exception != null) throw exception;
					}
				}
			}
		}
		
		if (dm.getCategories() != null){
			
			for (Category cat : dm.getCategories()){
				cat.accept(c->{
					try {
						validate(c, c.getParent() == null ? dm.getCategories() : c.getParent().getChildren());
					}catch (ParseException ex) {
						exception = ex;
						return false;
					}
					return true;
				});
				if (exception != null) throw exception;
			}
		}
	}
	
	private String getLabel(I18NMessages message) {
		return SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(message, locale);
	}
	
	private void validate(DmObject item, Collection<? extends DmObject> siblings) throws ParseException{

		List<DmObject> kids = new ArrayList<DmObject>(siblings);
		for (DmObject wrap : kids){
			if (wrap.getKeyId().equals(item.getKeyId())){
				kids.remove(wrap);
				break;
			}
		}
		String error = SimpleDataModel.validateKey(item.getKeyId(), kids, locale);
		if (error != null) {
			throw new ParseException(MessageFormat.format(getLabel(I18NMessages.INVALID_KEY), 
					new Object[] {item.getKeyId(), error }), 0);
		}
	
		for (Label nt : item.getNames()) {
			error = SimpleDataModel.validateName(nt.getValue(), nt.getLanguage(), locale);
			if (error != null) {
				throw new ParseException(MessageFormat.format(getLabel(I18NMessages.INVALID_NAME), new Object[] {nt.getValue(), error }), 0);
			}
			
		}
	}
	
}


