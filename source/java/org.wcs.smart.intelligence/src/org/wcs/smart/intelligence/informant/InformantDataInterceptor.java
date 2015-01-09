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
package org.wcs.smart.intelligence.informant;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.intelligence.model.Informant;

/**
 * Hibernate intercepter for encrypted data.
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class InformantDataInterceptor extends EmptyInterceptor {

	private static final long serialVersionUID = 6316391720540775230L;

	//track files to delete; only delete
	//after transaction has been committed
	protected List<File> toDelete = new ArrayList<File>();
	
	protected boolean shouldIntercept(Object entity) {
		return (entity instanceof Informant);
	}
	
	@Override
	public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
    	if (shouldIntercept(entity)) {
    		Informant i = (Informant) entity;
    		File file = i.getDataFile();
    		PersistentManager.toFile(file, i.getEncryptedData());
    	}
    	return true;
	}

	/**
	 * When a parent object is deleted it also deletes the file on disk.
	 */
	@Override
    public void onDelete(Object entity,
            Serializable id,
            Object[] state,
            String[] propertyNames,
            Type[] types) {
    	
    	if (shouldIntercept(entity)) {
    		Informant i = (Informant) entity;
    		toDelete.add(i.getDataFile());
    	}
    	
    }

	@Override
	public void afterTransactionCompletion(Transaction tx){
		if (tx.wasCommitted()){
			for (File f : toDelete){
				try{
					if (f.exists()) {
						f.delete();
					}
				}catch (Exception ex){
					SmartPlugIn.log("Could not delete file: " + f.toString(), ex); //$NON-NLS-1$
				}
			}
			toDelete.clear();
		}
	}
	
}
