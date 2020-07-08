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

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.type.Type;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cipher.EncryptUtils;
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
	protected List<Path> toDelete = new ArrayList<>();
	
	//if attachment files are encrypted when they are copied from
	//the source
	protected boolean encryptFiles = true;
	
	public AttachmentInterceptor() {
		this(true);
	}
	
	/**
	 * 
	 * In most cases users should use AttachmentInterceptor().  There are only a few special cases
	 * when copying files within the filestore that this option should be used.  Attachments that are 
	 * identified as not encryptable (see attachment.isEncrypted()) will never be encrypted irregardless of this setting 
	 * 
	 * @param encryptFiles if the files are encrypted when they are copied from the source location
	 */
	public AttachmentInterceptor(boolean encryptFiles) {
		this.encryptFiles = encryptFiles;
	}
	
	protected boolean shouldIntercept(Object entity) {
		return (entity instanceof ISmartAttachment);
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
				//if there are no more files then we delete the directory too
				try {
					if (Files.list(f.getParent()).count() == 0) {
						SmartUtils.deleteDirectory(f.getParent());
					}
				}catch (Exception ex) {
					SmartPlugIn.log("Could not delete empty directory: " + f.toString(), ex); //$NON-NLS-1$
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
				toDelete.add(attachment.getAttachmentFile());
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
    			Path to = attachment.getAttachmentFile();
    			if (!Files.exists(to.getParent())){
    				SmartUtils.createDirectory(to.getParent());
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
    			
    			while(Files.exists(to)){
    				Matcher m = p.matcher(basename);
    				if (m.matches()){
    					basename = m.group(1) + "_" + (Integer.parseInt(m.group(2)) +1 ); //$NON-NLS-1$
    				}else{
    					basename = basename + "_" + (counter++); //$NON-NLS-1$
    				}
    				to = to.getParent().resolve(basename + extension);
    			}
    			if (encryptFiles && attachment.isEncrypted()) {
	    			try {
	    				EncryptUtils.encryptFile(attachment.getCopyFromLocation(), to, attachment);
	    			}catch (Exception ex) {
	    				throw new RuntimeException(getExceptionErrorMessage() + ": " + ex.getMessage(), ex); //$NON-NLS-1$
	    			}
    			}else {
    				//just copy files
    				try {
						Files.copy(attachment.getCopyFromLocation(), to);
					} catch (IOException e) {
	    				throw new RuntimeException(getExceptionErrorMessage() + ": " + e.getMessage(), e); //$NON-NLS-1$

					}
    			}
    			
    			//state is what is written to db and should be updated
    			attachment.setFilename(to.getFileName().toString());
    			for (int i = 0; i < propertyNames.length;i++){
    				if (propertyNames[i].equals("filename")){ //$NON-NLS-1$
    					state[i] = to.getFileName().toString();
    				}
    			}
    			attachment.setCopyFromLocation(null);
    			attachment.computeFileLocation(to);
    			
    		}
    		
    	}
    	
    	return true;
    }

    
    protected String getExceptionErrorMessage() {
    	return Messages.AttachmentInterceptor_Error_CannotCopyFile;
    }
}
