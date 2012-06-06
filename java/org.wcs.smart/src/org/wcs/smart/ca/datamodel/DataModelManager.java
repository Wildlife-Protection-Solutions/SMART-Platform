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
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;

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
	
	/*
	 * Registered advisors 
	 */
	private List<IDataModelAdvisor> advisors = new ArrayList<IDataModelAdvisor>();
	
	
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
	
	/**
	 * Adds a data model advisor
	 * @param advisor 
	 */
	public void addDataModelAdvisor(IDataModelAdvisor advisor){
		this.advisors.add(advisor);
	}
	
	/**
	 * Removes a data model advisor
	 * @param advisor
	 */
	public void removeDataModelAdvisor(IDataModelAdvisor advisor){
		this.advisors.remove(advisor);
	}
	
	/**
	 * @return an unmodifiable list of registered advisors
	 */
	public List<IDataModelAdvisor> getAdvisors(){
		return Collections.unmodifiableList(advisors);
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
		monitor.beginTask("Delete Category : " + category.getFullCategoryName(), advisors.size() + 1);
		monitor.subTask("Verifying delete");
	
		for (IDataModelAdvisor advisor : advisors){
			String canDelete = advisor.canDelete(category, session);
			if (canDelete != null){
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Data Model Delete Error", "Category '" + category.getFullCategoryName() + "' could not be deleted.  Please resolve the following errors and try again:\n\n  -" + canDelete);
				return false;
			}
			monitor.worked(1);
			if (monitor.isCanceled()){
				cancelWork("Delete cancelled. Category " + category.getFullCategoryName() + " not deleted.");
				return false;
			}
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
		monitor.beginTask("Delete Attribute: " + attribute.getName(), advisors.size() + 1);
		monitor.subTask("Verifying delete");
		for (IDataModelAdvisor advisor : advisors){
			String canDelete = advisor.canDelete(attribute, session);
			if (canDelete != null){
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Data Model Delete Error", "Attribute '" + attribute.getName() + "' could not be deleted.  Please resolve the following errors and try again:\n\n  -" + canDelete);
				return false;
			}
			monitor.worked(1);
			if (monitor.isCanceled()){
				cancelWork("Delete cancelled. Attribute " + attribute.getName() + " not deleted.");
				return false;
			}
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
		monitor.beginTask("Delete Category/Attribute: " + key, advisors.size() + 1);
		monitor.subTask("Verifying delete");
		for (IDataModelAdvisor advisor : advisors){
			String canDelete = advisor.canDelete(categoryAttribute, session);
			if (canDelete != null){
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Data Model Delete Error", "Category/Attribute '" + key + "' could not be deleted.  Please resolve the following errors and try again:\n\n  -" + canDelete);
				return false;
			}
			monitor.worked(1);
			
			if (monitor.isCanceled()){
				cancelWork("Delete cancelled. Category/Attribute " +  key + " not deleted.");
				return false;
			}
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

		monitor.beginTask("Delete Attribute List Item: " + listItem.getName(), advisors.size() + 1);
		monitor.subTask("Verifying delete");
		for (IDataModelAdvisor advisor : advisors){
			String canDelete = advisor.canDelete(listItem, session);
			if (canDelete != null){
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Data Model Delete Error", "Attribute list item '" + listItem.getName() + "' could not be deleted.  Please resolve the following errors and try again:\n\n  -" + canDelete);
				return false;
			}
			monitor.worked(1);
			if (monitor.isCanceled()){
				cancelWork("Delete cancelled. Attribute list item " + listItem.getName() + " not deleted.");
				return false;
			}
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
		monitor.beginTask("Delete Attribute Tree Node: " + node.getName(), advisors.size() + 1);
		monitor.subTask("Verifying delete");
		for (IDataModelAdvisor advisor : advisors){
			String canDelete = advisor.canDelete(node, session);
			if (canDelete != null){
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Data Model Delete Error", "Attribute tree node '" + node.getName() + "' could not be deleted.  Please resolve the following errors and try again:\n\n  -" + canDelete);
				return false;
			}
			monitor.worked(1);
			if (monitor.isCanceled()){
				cancelWork("Delete cancelled. Attribute tree node " + node.getName() + " not deleted.");
				return false;
			}
		}
		return true;
	}
	
}
