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
package org.wcs.smart.ca.datamodel;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.advisors.DeleteManager;

/**
 * A class for managing the data model.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class DataModelManager {

	/*
	 * Singlegton instance
	 */
	private static DataModelManager INSTANCE = null;
	
	/**
	 * @return the local data model manager
	 */
	public static DataModelManager getInstance(){
		if (INSTANCE == null){
			INSTANCE = new DataModelManager();
		}
		return INSTANCE;
	}
	
	/*
	 * Registered change listeners
	 */
	private List<IDataModelListener> changeListeners = new ArrayList<IDataModelListener>();

	
	/**
	 * Creates a new data model manager
	 */
	private DataModelManager(){
		
	}
	
	
	/**
	 * These listeners are fired when the data model
	 * is saved to the database.
	 * 
	 * @param listener
	 */
	public void addChangeListener(IDataModelListener listener){
		changeListeners.add(listener);
	}
	
	/**
	 * These listeners are fired when the data model
	 * is saved to the database.
	 * 
	 * @param listener
	 */
	public void removeChangeListener(IDataModelListener listener){
		changeListeners.remove(listener);
	}
	
	/**
	 * Fire when the data model is saved to the database.
	 */
	public void fireChangeListeners(){
		for (IDataModelListener listener : changeListeners){
			listener.modified();
		}
	}
	
	private void cancelWork(String message){
		MessageDialog.openWarning(Display.getDefault().getActiveShell(), "Cancelled", message);
	}
	
	/**
	 * Verifies that a given category
	 * can be deleted from the data model by
	 * validating with all registered advisors.
	 * 
	 * @param category
	 * @param monitor
	 * @param session
	 * 
	 * @return <code>true</code> if category is been validated and can be deleted. <code>false</code> if should
	 * not be removed.
	 */
	public boolean validateDelete(Category category, IProgressMonitor monitor, Session session){
		monitor.beginTask("Delete Category : " + category.getFullCategoryName(), 1);
		monitor.subTask("Verifying delete");
	
		try{
			if (!DeleteManager.canDelete(category, session)){
				return false;
			}
		}catch (Exception ex){
			SmartPlugIn.log("Error verifying delete of category " + category.getFullCategoryName() , ex);
			cancelWork("Error verifying delete of category " + category.getFullCategoryName() + ": " + ex.getMessage());
			return false;
		}
		
		if (monitor.isCanceled()){
			cancelWork("Delete cancelled. Category " + category.getFullCategoryName() + " not deleted.");
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * Verifies that a given attribute
	 * can be deleted from the data model by
	 * validating with all registered advisors.
	 * 
	 * @param attribute
	 * @param monitor
	 * @param session
	 * 
	 * @return <code>true</code> if attribute is been validated and can be deleted. <code>false</code> if should
	 * not be removed.
	 */
	public boolean validateDelete(Attribute attribute, IProgressMonitor monitor, Session session){
		monitor.beginTask("Delete Attribute: " + attribute.getName(), 1);
		monitor.subTask("Verifying delete");
		
		try{
			if (!DeleteManager.canDelete(attribute, session)){
				return false;
			}
		}catch (Exception ex){
			SmartPlugIn.log("Error verifying delete of attribute " + attribute.getName() , ex);
			cancelWork("Error verifying delete of attribute " + attribute.getName() + ": " + ex.getMessage());
			return false;
		}
		
		monitor.worked(1);
		if (monitor.isCanceled()){
			cancelWork("Delete cancelled. Attribute " + attribute.getName() + " not deleted.");
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * Verifies that a given category/attribute relationship
	 * can be deleted from the data model by
	 * validating with all registered advisors.
	 * 
	 * @param category
	 * @param monitor
	 * @param session
	 * 
	 * @return <code>true</code> if category/attribute is been validated 
	 * and can be deleted. <code>false</code> if should
	 * not be removed.
	 */
	public boolean validateDelete(CategoryAttribute categoryAttribute, IProgressMonitor monitor, Session session){
		String key = categoryAttribute.getCategory().getName() + "/" + categoryAttribute.getAttribute().getName();
		monitor.beginTask("Delete Category/Attribute: " + key, 1);
		monitor.subTask("Verifying delete");
		
		try{
			if (!DeleteManager.canDelete(categoryAttribute, session)){
				return false;
			}
		}catch (Exception ex){
			SmartPlugIn.log("Error verifying delete of category/attribute" , ex);
			cancelWork("Error verifying delete of categoryAttribute: " + ex.getMessage());
			return false;
		}
		
		monitor.worked(1);
		if (monitor.isCanceled()){
			cancelWork("Delete cancelled. Category/Attribute " +  key + " not deleted.");
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * Verifies that a given attribute list item
	 * can be deleted from the data model by
	 * validating with all registered advisors.
	 * 
	 * @param category
	 * @param monitor
	 * @param session
	 * 
	 * @return <code>true</code> if attribute list item is been validated 
	 * and can be deleted. <code>false</code> if should
	 * not be removed.
	 */
	public boolean validateDelete(AttributeListItem listItem, IProgressMonitor monitor, Session session){

		monitor.beginTask("Delete Attribute List Item: " + listItem.getName(), 1);
		monitor.subTask("Verifying delete");
		try{
			if (!DeleteManager.canDelete(listItem, session)){
				return false;
			}
		}catch (Exception ex){
			SmartPlugIn.log("Error verifying delete of listitem " + listItem.getName() , ex);
			cancelWork("Error verifying delete of listItem " + listItem.getName() + ": " + ex.getMessage());
			return false;
		}
		monitor.worked(1);
		if (monitor.isCanceled()){
			cancelWork("Delete cancelled. Attribute list item " + listItem.getName() + " not deleted.");
			return false;
		}
		
		return true;
	}
	
	
	/**
	 * Verifies that a given attribute node 
	 * can be deleted from the data model by
	 * validating with all registered advisors.
	 * 
	 * @param category
	 * @param monitor
	 * @param session
	 * 
	 * @return <code>true</code> if attribute node is been validated 
	 * and can be deleted. <code>false</code> if should
	 * not be removed.
	 */
	public boolean validateDelete(AttributeTreeNode node, IProgressMonitor monitor, Session session){
		monitor.beginTask("Delete Attribute Tree Node: " + node.getName(), 1);		
		monitor.subTask("Verifying delete");
		
		try{
			if (!DeleteManager.canDelete(node, session)){
				return false;
			}
		}catch (Exception ex){
			SmartPlugIn.log("Error verifying delete of tree node " + node.getName() , ex);
			cancelWork("Error verifying delete of tree node " + node.getName() + ": " + ex.getMessage());
			return false;
		}
		monitor.worked(1);
		if (monitor.isCanceled()){
			cancelWork("Delete cancelled. Attribute tree node " + node.getName() + " not deleted.");
			return false;
		}
		return true;
	}
	
}
