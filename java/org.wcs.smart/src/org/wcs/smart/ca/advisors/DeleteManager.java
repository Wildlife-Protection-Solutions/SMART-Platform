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
package org.wcs.smart.ca.advisors;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.hibernate.Session;
import org.hibernate.proxy.HibernateProxyHelper;

/**
 * Delete manager for deleting items from the database.
 * <p>
 * This manager checks all registered deleteAdvisor extension points
 * of the same type of object to be deleted.  
 * </p>
 * <p>
 * This does not delete the item from the database.
 * </p>
 * @author Emily
 *
 */
public class DeleteManager {

	public static final String DELETE_ADVIOR_EXTENSION_ID = "org.wcs.smart.ca.deleteAdvisor"; //$NON-NLS-1$
	
	
	/**
	 * Each registered advisor of the class of the provided object is run and if
	 * one fails, and exception is thrown with an error message that describes the problem.
	 * Otherwise true is returned.
	 *  
	 * @param x object to delete
	 * @param session open database session
	 * @return <code>true</code> if object can be deleted, <code>false</code> if not 
	 * @throws Exception if item can no be deleted
	 */
	public static boolean canDelete(Object x, Session session) throws Exception{
		Class<?> clazzz = HibernateProxyHelper.getClassWithoutInitializingProxy(x);
		
		//find all extension points for the given class
		List<IDeleteAdvisor> items = new ArrayList<IDeleteAdvisor>();
		if (Platform.getExtensionRegistry() != null){
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(DELETE_ADVIOR_EXTENSION_ID);
			
			for (IConfigurationElement e : config) {
				Class<?> c = Class.forName( e.getAttribute("class") ); //$NON-NLS-1$
				if (c == clazzz){
					//matching type add this for processing
					items.add((IDeleteAdvisor)e.createExecutableExtension("advisor")); //$NON-NLS-1$
				}
			}
		}
		boolean transaction = false;
		if (!session.getTransaction().isActive()){
			session.beginTransaction();
			transaction = true;
		}
		try{
			for (IDeleteAdvisor advisor : items){
				String error = advisor.canDelete(x, session);
				if (error != null){
					throw new Exception(error);
				}
			}
		}finally{
			//advisor are not to change the database state
			if (transaction){
				session.getTransaction().rollback();
			}
		}
		return true;
	}
}
