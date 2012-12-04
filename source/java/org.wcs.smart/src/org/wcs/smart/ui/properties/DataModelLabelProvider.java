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
package org.wcs.smart.ui.properties;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

/**
 * Label provided for data model tree
 * @author Emily
 * @since 1.0.0
 */
public class DataModelLabelProvider extends LabelProvider implements IColorProvider {
	
	/**
	 * Image descriptor for category icon
	 */
	public static final String CATEGORY_ICON = "org.wsc.smart.datamodel.CATEGORY_ICON"; //$NON-NLS-1$
	/**
	 * Image descriptor for attribute text icon
	 */
	private static final String ATTRIBUTE_TEXT_ICON= "org.wsc.smart.datamodel.ATTRIBUTE_TEXT_ICON"; //$NON-NLS-1$
	
	/**
	 * Image descriptor for attribute boolean icon
	 */
	private static final String ATTRIBUTE_BOOLEAN_ICON = "org.wsc.smart.datamodel.ATTRIBUTE_BOOLEAN_ICON"; //$NON-NLS-1$
	/**
	 * Image descriptor for attribute number icon
	 */
	public static final String ATTRIBUTE_NUMBER_ICON= "org.wsc.smart.datamodel.ATTRIBUTE_NUMBER_ICON"; //$NON-NLS-1$
	/**
	 * Image descriptor for attribute list icon
	 */
	public static final String ATTRIBUTE_LIST_ICON= "org.wsc.smart.datamodel.ATTRIBUTE_LIST_ICON"; //$NON-NLS-1$
	/**
	 * Image descriptor for attribute tree icon
	 */
	private static final String ATTRIBUTE_TREE_ICON= "org.wsc.smart.datamodel.ATTRIBUTE_TREE_ICON"; //$NON-NLS-1$
	/**
	 * Image descriptor for data model icon
	 */
	public static final String DATA_MODEL_ICON= "org.wsc.smart.datamodel.DATAMODEL_ICON"; //$NON-NLS-1$
	
	private static final Color BLACK = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
	private static final Color GRAY = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY);

	
	/**
	 * Get image descriptors for the clear button.
	 */
	static {
		ImageDescriptor descriptor = AbstractUIPlugin
				.imageDescriptorFromPlugin(SmartPlugIn.PLUGIN_ID,
						"images/icons/obj16/category_obj.gif"); //$NON-NLS-1$
						//"$nl$/icons/obj16/category_obj.gif"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(CATEGORY_ICON, descriptor);
		}
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
				SmartPlugIn.PLUGIN_ID, "images/icons/obj16/attribute_text.png"); //$NON-NLS-1$
		//"$nl$/icons/obj16/attribute_obj.gif"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(ATTRIBUTE_TEXT_ICON, descriptor);
		}
		
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
				SmartPlugIn.PLUGIN_ID, "images/icons/obj16/attribute_number.png"); //$NON-NLS-1$
		//"$nl$/icons/obj16/attribute_obj.gif"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(ATTRIBUTE_NUMBER_ICON, descriptor);
		}
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
				SmartPlugIn.PLUGIN_ID, "images/icons/obj16/attribute_boolean.png"); //$NON-NLS-1$
		//"$nl$/icons/obj16/attribute_obj.gif"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(ATTRIBUTE_BOOLEAN_ICON, descriptor);
		}
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
				SmartPlugIn.PLUGIN_ID, "images/icons/obj16/attribute_list.png"); //$NON-NLS-1$
		//"$nl$/icons/obj16/attribute_obj.gif"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(ATTRIBUTE_LIST_ICON, descriptor);
		}
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
				SmartPlugIn.PLUGIN_ID, "images/icons/obj16/attribute_tree.png"); //$NON-NLS-1$
		//"$nl$/icons/obj16/attribute_obj.gif"); //$NON-NLS-1$
		if (descriptor != null) {
			JFaceResources.getImageRegistry().put(ATTRIBUTE_TREE_ICON, descriptor);
		}
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(SmartPlugIn.PLUGIN_ID,"images/icons/smart16.gif"); //$NON-NLS-1$
		if (descriptor != null){
			JFaceResources.getImageRegistry().put(DATA_MODEL_ICON, descriptor);
		}
	}
	
	private Language currentLang = null;
	
	/**
	 * Creates new data model provided.  In this case
	 * the current system language is used.
	 * 
	 */
	public DataModelLabelProvider(){
	}
	
	/**
	 * Creates new data model provided
	 * @param lang the working language
	 */
	public DataModelLabelProvider(Language lang){
		this.currentLang = lang;
	}
	/**
	 * Update the language
	 * @param lang new language
	 */
	public void setLanguage(Language lang){
		this.currentLang = lang;
	}
	
	@Override
	public String getText(Object element) {
		if (element instanceof DataModelContentProvider.RootNode){
			return Messages.DataModelLabelProvider_RootNode_Label;
		}
		if (element instanceof CategoryAttribute){
			element = ((CategoryAttribute)element).getAttribute();
		}
		
		if (element instanceof DmObject){
			DmObject obj = (DmObject)element;
			if (currentLang != null){
				String x = obj.findNameNull(currentLang);
				if (x==null){
					x = obj.getName();
					if (x == null){
						x = obj.findName(SmartDB.getCurrentConservationArea().getDefaultLanguage());
					}
				}
				return x;
			}else{
				return obj.getName();
			}
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public Image getImage(Object element) {
		if (element instanceof Category) {
			return JFaceResources.getImageRegistry().get(CATEGORY_ICON);
		}else if (element instanceof CategoryAttribute){
			CategoryAttribute ca = (CategoryAttribute)element;
			return getAttributeImage(ca.getAttribute());
		}else if (element instanceof Attribute){
			return getAttributeImage((Attribute)element);
			
		}else if (element instanceof DataModelContentProvider.RootNode){
			return JFaceResources.getImageRegistry().get(DATA_MODEL_ICON);
		}
		return null;
	}
	
	private Image getAttributeImage(Attribute att){
		if (att.getType()== Attribute.AttributeType.BOOLEAN){
			return JFaceResources.getImageRegistry().get(ATTRIBUTE_BOOLEAN_ICON);
		}else if (att.getType() == Attribute.AttributeType.TEXT){
			return JFaceResources.getImageRegistry().get(ATTRIBUTE_TEXT_ICON);
		}else if (att.getType()== Attribute.AttributeType.LIST){
			return JFaceResources.getImageRegistry().get(ATTRIBUTE_LIST_ICON);
		}else if (att.getType()== Attribute.AttributeType.NUMERIC){
			return JFaceResources.getImageRegistry().get(ATTRIBUTE_NUMBER_ICON);
		}else if (att.getType()== Attribute.AttributeType.TREE){
			return JFaceResources.getImageRegistry().get(ATTRIBUTE_TREE_ICON);
		}
		return null;
	}
	
	@Override
	public Color getForeground(Object element) {
		boolean active = true;
		if (element instanceof Category){
			active = ((Category) element).getIsActive();
		}else if (element instanceof CategoryAttribute){
			active = ((CategoryAttribute)element).getIsActive();
		}
		if (active){
			return BLACK;
		}else{
			return GRAY;
		}
	}
	
	@Override
	public Color getBackground(Object element) {
		return null;
	}

}
