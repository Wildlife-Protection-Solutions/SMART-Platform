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
import org.wcs.smart.internal.Messages;

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
		MessageDialog.openWarning(Display.getDefault().getActiveShell(), Messages.DataModelManager_Cancelled_DialogTitle, message);
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
		monitor.beginTask(Messages.DataModelManager_Progress_DeleteCategory + category.getFullCategoryName(), 1);
		monitor.subTask(Messages.DataModelManager_Progress_ValidatingDelete);
	
		try{
			if (!DeleteManager.canDelete(category, session)){
				return false;
			}
		}catch (Exception ex){
			SmartPlugIn.log(Messages.DataModelManager_Error_DeleteCategory + category.getFullCategoryName() , ex);
			cancelWork(Messages.DataModelManager_Error_DeleteCategory + category.getFullCategoryName() + ": " + ex.getLocalizedMessage()); //$NON-NLS-1$
			return false;
		}
		
		if (monitor.isCanceled()){
			cancelWork(Messages.DataModelManager_Cancelled_CategoryDelete + category.getFullCategoryName() );
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
		monitor.beginTask(Messages.DataModelManager_Progress_DeleteAttribute + attribute.getName(), 1);
		monitor.subTask(Messages.DataModelManager_Progress_ValidatingDelete);
		
		try{
			if (!DeleteManager.canDelete(attribute, session)){
				return false;
			}
		}catch (Exception ex){
			SmartPlugIn.log(Messages.DataModelManager_Error_DeleteAttribute + attribute.getName() , ex);
			cancelWork(Messages.DataModelManager_Error_DeleteAttribute + attribute.getName() + ": " + ex.getLocalizedMessage()); //$NON-NLS-1$
			return false;
		}
		
		monitor.worked(1);
		if (monitor.isCanceled()){
			cancelWork(Messages.DataModelManager_Cancelled_AttributeDelete + attribute.getName());
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
		String key = categoryAttribute.getCategory().getName() + "/" + categoryAttribute.getAttribute().getName(); //$NON-NLS-1$
		monitor.beginTask(Messages.DataModelManager_Progress_DeleteCatAtt + key, 1);
		monitor.subTask(Messages.DataModelManager_Progress_ValidatingDelete);
		
		try{
			if (!DeleteManager.canDelete(categoryAttribute, session)){
				return false;
			}
		}catch (Exception ex){
			SmartPlugIn.log(Messages.DataModelManager_Error_DeleteCatAtt , ex);
			cancelWork(Messages.DataModelManager_Error_DeleteCatAtt + ex.getLocalizedMessage());
			return false;
		}
		
		monitor.worked(1);
		if (monitor.isCanceled()){
			cancelWork(Messages.DataModelManager_Cancelled_DeleteCatAtt +  key );
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

		monitor.beginTask(Messages.DataModelManager_Progress_DeleteListItem + listItem.getName(), 1);
		monitor.subTask(Messages.DataModelManager_Progress_ValidatingDelete);
		try{
			if (!DeleteManager.canDelete(listItem, session)){
				return false;
			}
		}catch (Exception ex){
			SmartPlugIn.log(Messages.DataModelManager_Error_DeleteListItem + listItem.getName() , ex);
			cancelWork(Messages.DataModelManager_Error_DeleteListItem + listItem.getName() + ": " + ex.getLocalizedMessage()); //$NON-NLS-1$
			return false;
		}
		monitor.worked(1);
		if (monitor.isCanceled()){
			cancelWork(Messages.DataModelManager_Cancelled_DeleteListItem + listItem.getName() );
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
		monitor.beginTask(Messages.DataModelManager_Progress_DeleteTreeNode + node.getName(), 1);		
		monitor.subTask(Messages.DataModelManager_Progress_ValidatingDelete);
		
		try{
			if (!DeleteManager.canDelete(node, session)){
				return false;
			}
		}catch (Exception ex){
			SmartPlugIn.log(Messages.DataModelManager_Error_DeleteTreeNode + node.getName() , ex);
			cancelWork(Messages.DataModelManager_Error_DeleteTreeNode + node.getName() + ": " + ex.getLocalizedMessage()); //$NON-NLS-1$
			return false;
		}
		monitor.worked(1);
		if (monitor.isCanceled()){
			cancelWork(Messages.DataModelManager_Cancelled_DeleteTreeNode + node.getName() );
			return false;
		}
		return true;
	}
	
}
