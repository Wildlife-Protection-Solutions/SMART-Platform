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
package org.wcs.smart.dataentry.dialog;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.IImageAssociatedObject;

/**
 * An interceptor for images associated with some of the objects.
 * Interceptor copies the file into the filestore  if necessary or
 * deletes the file from the filestore.
 * 
 * Must be applied to any hibernate session that modifies entity with associated image
 * in order for image files to be saved/removed correctly from the data store.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class AssociatedImageInterceptor extends EmptyInterceptor {

	private static final long serialVersionUID = -4670080959693764999L;
	
	//track files to delete/save; only delete
	//after transaction has been committed
	protected List<File> toDelete = new ArrayList<>();
	protected List<FilePair> toSave = new ArrayList<>();
	
	
	protected boolean shouldIntercept(Object entity) {
		return (entity instanceof IImageAssociatedObject);
	}

	@Override
	public void afterTransactionCompletion(Transaction tx) {
		if (tx.wasCommitted()){
			for (File f : toDelete){
				try{
					f.delete();
				}catch (Exception ex){
					SmartPlugIn.log("Could not delete file: " + f.toString(), ex); //$NON-NLS-1$
				}
			}
			toDelete.clear();
			for (FilePair fp : toSave) {
				try {
					FileUtils.copyFile(fp.from, fp.to);
				} catch (IOException e) {
					SmartPlugIn.log("Could not copy file: " + fp.from.toString() + " to " + fp.to.toString(), e); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			toSave.clear();
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
    		if (entity instanceof CmNode) {
    			deleteCmNode((CmNode)entity);
    		} else if (entity instanceof CmAttributeTreeNode) {
    			deleteCmTreeNode((CmAttributeTreeNode)entity);
    		} else {
    			handleDelete((IImageAssociatedObject)entity);
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
    	return onSaveOrUpdate(entity);
    }

    /**
	 * When a object is updated it also saves or deletes the file on disk if needed.
	 */
	@Override
	public boolean onFlushDirty(Object entity, Serializable id,
			Object[] currentState, Object[] previousState,
			String[] propertyNames, Type[] types) {
    	return onSaveOrUpdate(entity);
	}

	private void deleteCmNode(CmNode node) {
		handleDelete(node);
		for (CmNode n : node.getChildren()) {
			deleteCmNode(n);
		}
		for (CmAttribute a : node.getCmAttributes()) {
			for (CmAttributeListItem li : a.getList()) {
				handleDelete(li);
			}
			for (CmAttributeTreeNode tn : a.getTree()) {
				deleteCmTreeNode(tn);
			}
		}
	}
	
	private void deleteCmTreeNode(CmAttributeTreeNode tn) {
		handleDelete(tn);
		for (CmAttributeTreeNode child : tn.getChildren()) {
			deleteCmTreeNode(child);
		}
	}

	private boolean onSaveOrUpdate(Object entity) {
		if (shouldIntercept(entity)) {
			handleSaveOrUpdate((IImageAssociatedObject)entity);
		}
		return false;
	}

	
	private void handleSaveOrUpdate(IImageAssociatedObject imgObject) {
		//need some from-to mapping; and also objects can be cleared
		File from = imgObject.getImageFile();
		File to = new File(imgObject.getImagePersistenceLocation());
		if (from == null || from.equals(IImageAssociatedObject.NULL_FILE)) {
			if (to.exists()) {
				toDelete.add(to);
			}
		} else if (!from.equals(to)) {
			toSave.add(new FilePair(from, to));
		}
	}

	private void handleDelete(IImageAssociatedObject imgObject) {
		File file = new File(imgObject.getImagePersistenceLocation());
		if (file.exists()) {
			toDelete.add(file);
		}
	}
	
	/**
	 * Stores information about which file and where we need to copy.
	 * 
	 * @author elitvin
	 * @since 4.0.0
	 */
	private class FilePair {
		File from;
		File to;
		FilePair(File from, File to) {
			this.from = from;
			this.to = to;
		}
	}
}
