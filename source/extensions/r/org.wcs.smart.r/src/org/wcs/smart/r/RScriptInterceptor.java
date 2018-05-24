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
package org.wcs.smart.r;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.type.Type;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.SessionInterceptor;
import org.wcs.smart.r.model.RScript;

/**
 * An interceptor for attachment that copies the file into the filestore
 * if necessary or deletes the file from the filestore.
 * 
 * Must be applied to any hibernate session that modifies entity with attachments
 * in order for attachment files to be saved/removed correctly from the data store.
 *  
 * @author Emily
 * @since 1.0.0
 */
public class RScriptInterceptor extends SessionInterceptor {

	private static final long serialVersionUID = 2710377589619179841L;

	//track files to delete; only delete
	//after transaction has been committed
	protected List<Path> toDelete = new ArrayList<Path>();

	
	public RScriptInterceptor() {
	}

	
	protected boolean shouldIntercept(Object entity) {
		return (entity instanceof RScript);
	}
	
	
	@Override
	public void afterTransactionCompletion(Transaction tx){
		if (tx.getStatus() == TransactionStatus.COMMITTED){
			for (Path f : toDelete){
				try{
					Files.delete(f);
				}catch (Exception ex){
					SmartPlugIn.log("Could not delete file: " + f.toString(), ex); //$NON-NLS-1$
				}
			}
			toDelete.clear();
		}
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
    		RScript attachment = (RScript) entity;
    		try {
				toDelete.add(RScriptManager.INSTANCE.getScriptPath(attachment));
			} catch (Exception ex) {
				SmartPlugIn.log("Unable to delete attachment", ex); //$NON-NLS-1$
			}
    	}
    	
    }

    
    /**
	 * When a object is saved it also saves the file on disk.
	 */
	@Override
    public boolean onSave(Object entity,
            Serializable id,
            Object[] state,
            String[] propertyNames,
            Type[] types) {
    	
    	if (shouldIntercept(entity)) {
    		RScript attachment = (RScript) entity;
    		if (attachment.getImportFile() == null) return true;
    		
    		Path toFile = RScriptManager.INSTANCE.computeScriptFileName(attachment.getImportFile(), attachment.getConservationArea());
    		//just copy files
   			try {
   	    		Files.createDirectories(toFile.getParent());
				Files.copy(attachment.getImportFile(), toFile);
			} catch (IOException e) {
    			throw new RuntimeException("Error importing R script from filestore: " + e.getMessage(), e); //$NON-NLS-1$
   			}
   			attachment.setFilename(toFile.getFileName().toString());	
   			for (int i = 0; i < propertyNames.length;i++){
   				if (propertyNames[i].equals("filename")){ //$NON-NLS-1$
   					state[i] = toFile.getFileName().toString();
   				}
   			}
   			attachment.setImportFile(null);
    		
    	}
    	
    	return true;
    }

}
