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
package org.wcs.smart.common.attachment;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.SessionInterceptor;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * An interceptor for attachment that copies the file into the filestore
 * if necessary or deletes the file from the filestore.
 * 
 * Must be applied to any hibernate session that modifies entity with attachments
 * in order for attachment files to be saved/removed correctly from the data store.
 *  
 * @author elitvin
 * @author Emily
 * @since 1.0.0
 */
public class AttachmentInterceptor extends SessionInterceptor {

	private static final long serialVersionUID = 2710377589619179841L;

	//track files to delete; only delete
	//after transaction has been committed
	protected List<File> toDelete = new ArrayList<File>();
	
	protected boolean shouldIntercept(Object entity) {
		return (entity instanceof ISmartAttachment);
	}
	
	
	@Override
	public void afterTransactionCompletion(Transaction tx){
		if (tx.wasCommitted()){
			for (File f : toDelete){
				try{
					f.delete();
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
    		ISmartAttachment attachment = (ISmartAttachment) entity;
    		try {
    			attachment.computeFileLocation(session);
				toDelete.add(attachment.getAttachmentFile().getCanonicalFile());
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
    		ISmartAttachment attachment = (ISmartAttachment) entity;
    		
    		if (attachment.getCopyFromLocation() != null){
    			try {
					attachment.computeFileLocation(session);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
    			File to = attachment.getAttachmentFile();
    			if (!to.getParentFile().exists()){
    				SmartUtils.createDirectory(to.getParentFile());
    			}
    			
    			String name = attachment.getFilename();
    			int pos = name.indexOf('.');
    			String basename = name;
    			String extension = ""; //$NON-NLS-1$
    			if (pos > 0){
    				basename = name.substring(0, pos);
    				extension = name.substring(pos);
    			}
    			
    			Pattern p = Pattern.compile("(.*)_(\\d+)"); //$NON-NLS-1$
    			int counter = 1;
    			
    			while(to.exists()){
    				Matcher m = p.matcher(basename);
    				if (m.matches()){
    					basename = m.group(1) + "_" + (Integer.parseInt(m.group(2)) +1 ); //$NON-NLS-1$
    				}else{
    					basename = basename + "_" + (counter++); //$NON-NLS-1$
    				}
    				to = new File(to.getParentFile(), basename + extension);
    			}
    			if (!SmartUtils.copyFile(attachment.getCopyFromLocation(), to)){
    				throw new RuntimeException(getExceptionErrorMessage());
    			}else{
    				//state is what is written to db and should be updated
    				attachment.setFilename(to.getName());
    				for (int i = 0; i < propertyNames.length;i++){
    					if (propertyNames[i].equals("filename")){ //$NON-NLS-1$
    						state[i] = to.getName();
    					}
    				}
    				attachment.setCopyFromLocation(null);
    				attachment.computeFileLocation(to);
    			}
    		}
    		
    	}
    	
    	return true;
    }

    
    protected String getExceptionErrorMessage() {
    	return Messages.AttachmentInterceptor_Error_CannotCopyFile;
    }
}
