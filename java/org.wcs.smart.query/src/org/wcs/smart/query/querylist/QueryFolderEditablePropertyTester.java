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
package org.wcs.smart.query.querylist;

import org.eclipse.core.expressions.PropertyTester;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.QueryInput;

/**
 * A propterty testers that tests QueryFolder's and QueryInput's
 * to determine if the given user has permission to
 * rename, delete, or edit the given 
 * item.
 * <p>
 * Valid operators include: rename, delete, newfolder
 * 
 * @author egouge
 * @since 1.0.0
 */
public class QueryFolderEditablePropertyTester extends PropertyTester {

	public static final String NEWFOLDER_OP = "newfolder";
	public static final String DELETE_OP = "delete";
	public static final String RENAME_OP = "rename";

	public QueryFolderEditablePropertyTester() {
	}

	/**
	 * @see org.eclipse.core.expressions.IPropertyTester#test(java.lang.Object, java.lang.String, java.lang.Object[], java.lang.Object)
	 */
	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		
		if (args.length != 1){
			return false;
		}
		
		String operator = (String)args[0];
		if (receiver instanceof QueryFolder){
			return canModify((QueryFolder)receiver, operator);
		}else if (receiver instanceof QueryInput){
			return canModify((QueryInput) receiver, operator);
		}
		return false;
	}

	
	
	public static boolean canModify(QueryInput input, String operator){
		if (operator.equals(DELETE_OP) || operator.equals(RENAME_OP)){
			if (!input.isShared() ){
				//always rename or delete your own queries
				return true;
			}else if (input.isShared()){
				//only rename or deletable if admin or manager
				if (QueryHibernateManager.canModifyCaQueries()){
					return true;
				}
				return false;
			}
		}
		return false;
	}
	
	public static boolean canModify(QueryFolder folder, String operator){
		if (operator.equals(RENAME_OP) || operator.equals(DELETE_OP)){
			if (folder.isRootFolder()){
				return false;
			}
			//if (((QueryFolder) receiver).getEmployee() != null && ((QueryFolder) receiver).getEmployee().equals(SmartDB.getCurrentEmployee())){
			if (folder.getEmployee() != null){
				return true;
			}else if (folder.getEmployee() == null ){
				//conservation area level folder; only rename or deletable if admin or manager
				if (QueryHibernateManager.canModifyCaQueries()){
					return true;
				}
			}
			
		}else if (operator.equals(NEWFOLDER_OP)){
			if (folder.getEmployee() != null){
				return true;
			}else if (folder.getEmployee() == null ){
				//conservation area level folder; only rename or deletable if admin or manager
				if (QueryHibernateManager.canModifyCaQueries()){
					return true;
				}
			}
			
		}
		
		return false;
	}
}
