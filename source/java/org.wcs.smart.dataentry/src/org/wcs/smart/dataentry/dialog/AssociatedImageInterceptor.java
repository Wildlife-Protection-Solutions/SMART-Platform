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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
	
	//track files to delete/save; perform actions
	//after transaction has been committed
	//NOTE: we are interested only in last action for the object as it overwrites any previous activities
	private Map<UUID, IOperation> operations = new HashMap<>();
	
	protected boolean shouldIntercept(Object entity) {
		return (entity instanceof IImageAssociatedObject);
	}

	@Override
	public void afterTransactionCompletion(Transaction tx) {
		if (tx.wasCommitted()) {
			for (IOperation op : operations.values()) {
				op.execute();
			}
		}
		operations.clear();
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
		IOperation lastOp = operations.get(imgObject.getUuid());
		if (!(lastOp instanceof DeleteOperation)) {
			//in case this object was deleted other operations do not make sense
			operations.put(imgObject.getUuid(), new SaveOperation(imgObject));
		}
	}

	private void handleDelete(IImageAssociatedObject imgObject) {
		File file = new File(imgObject.getImagePersistenceLocation());
		operations.put(imgObject.getUuid(), new DeleteOperation(file));
	}

	/**
	 * Interface that represent any operation with associated image file.
	 * 
	 * @author elitvin
	 * @since 4.0.0
	 */
	private interface IOperation {
		public void execute();
	}

	
	/**
	 * Operation responsible for deleting an associated image file.
	 * 
	 * @author elitvin
	 * @since 4.0.0
	 */
	private class DeleteOperation implements IOperation {
		
		private File toDelete;
		
		public DeleteOperation(File toDelete) {
			this.toDelete = toDelete;
		}

		@Override
		public void execute() {
			if (toDelete.exists()) {
				try {
					toDelete.delete();
				} catch (Exception ex) {
					SmartPlugIn.log("Could not delete file: " + toDelete.toString(), ex); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * Operation responsible for saving an associated image file.
	 * Stores information about which file and where we need to copy.
	 * 
	 * @author elitvin
	 * @since 4.0.0
	 */
	private class SaveOperation implements IOperation {
		
		private IImageAssociatedObject imgObject;
		
		public SaveOperation(IImageAssociatedObject imgObject) {
			this.imgObject = imgObject;
		}

		@Override
		public void execute() {
			//need some from-to mapping; and also objects can be cleared
			File from = imgObject.getImageFile();
			File to = new File(imgObject.getImagePersistenceLocation());
			
			if (from == null || from.equals(IImageAssociatedObject.NULL_FILE)) {
				//image was cleared
				if (to.exists()) {
					try {
						to.delete();
					} catch (Exception ex) {
						SmartPlugIn.log("Could not delete cleared file: " + to.toString(), ex); //$NON-NLS-1$
					}
				}
			} else if (!from.equals(to)) {
				try {
					FileUtils.copyFile(from, to);
				} catch (IOException e) {
					SmartPlugIn.log("Could not copy file: " + from.toString() + " to " + to.toString(), e); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			
			//reset img object file.  If we copy into the system this call will
			//reset the object reference to the file copied in
			imgObject.setImageFile(null);
		}
	}
}
