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
package org.wcs.smart.connect.hibernate;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.type.Type;
import org.wcs.smart.cipher.EncryptUtils;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.connect.i18n.Messages;

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
public class AttachmentInterceptor extends EmptyInterceptor {

	private static final long serialVersionUID = 2710377589619179841L;

	private final Logger logger = Logger.getLogger(AttachmentInterceptor.class.getName());

	
	//track files to delete; only delete
	//after transaction has been committed
	protected List<Path> toDelete = new ArrayList<>();
	
	private Session session;
	private Locale locale;
	
	public AttachmentInterceptor() {
	}

	public void setSession(Session session, Locale l) {
		this.session = session;
		this.locale = l;
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
					logger.log(Level.WARNING, "Could not delete file: " + f.toString(), ex); //$NON-NLS-1$
				}
				//if there are no more files then we delete the directory too
				try {
					if (Files.list(f.getParent()).count() == 0) {
						FileUtils.deleteDirectory(f.getParent().toAbsolutePath().normalize().toFile());
					}
				}catch (Exception ex) {
					logger.log(Level.WARNING, "Could not delete empty directory: " + f.toString(), ex); //$NON-NLS-1$
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
				logger.log(Level.WARNING, "Unable to delete attachment", ex); //$NON-NLS-1$
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
    				try {
						Files.createDirectories(to.getParent());
					} catch (IOException e) {
						logger.log(Level.SEVERE, e.getMessage(),e);
					}
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
    			if (attachment.isEncrypted()) {
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
    	return Messages.getString("AttachmentInterceptor_AttachmentError", locale); //$NON-NLS-1$
    }
}
