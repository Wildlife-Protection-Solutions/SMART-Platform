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

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.hibernate.EmptyInterceptor;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.hibernate.type.Type;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.IImageAssociatedObject;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.SmartUtils;

/**
 * An interceptor for images associated with some of the objects.
 * Interceptor copies the file into the filestore  if necessary or
 * deletes the file from the filestore.
 * 
 * Must be applied to any hibernate session that modifies entity with associated image
 * in order for image files to be saved/removed correctly from the data store.
 * 
 * This interceptor also deals with saving the CmAttribute help images to 
 * the filestore.
 * 
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class AssociatedImageInterceptor extends EmptyInterceptor {

	private static final long serialVersionUID = -4670080959693764999L;
	
	//track files to delete/save; perform actions
	//after transaction has been committed
	//NOTE: we are interested only in last action for the object as it overwrites any previous activities
	private List<IOperation> operations = new ArrayList<>();
	
	protected boolean shouldIntercept(Object entity) {
		return (entity instanceof IImageAssociatedObject);
	}

	@Override
	public void afterTransactionCompletion(Transaction tx) {
		if (tx.getStatus() == TransactionStatus.COMMITTED) {
			for (IOperation op : operations) {
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
		if (node.getCmAttributes() != null) {
			for (CmAttribute a : node.getCmAttributes()) {
				handleDelete(a);
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
		if (entity instanceof CmAttributeOption) {
			handleSaveOrUpdate( ((CmAttributeOption)entity).getCmAttribute());
		}
		return false;
	}

	
	private void handleSaveOrUpdate(IImageAssociatedObject imgObject) {
		operations.add(new SaveOperation(imgObject));
	}

	private void handleDelete(IImageAssociatedObject imgObject) {
		Path file = Paths.get(imgObject.getImagePersistenceLocation());
		
		Object t = imgObject;
		if (t instanceof CmAttributeOption) {
			t = ((CmAttributeOption)t).getCmAttribute();
		}
		if (t instanceof CmAttribute) {
			Path helpImage = ((CmAttribute) t).getHelpImage();
			if (helpImage != null && !helpImage.equals(((CmAttribute) t).getImportHelpFile()))
				operations.add(new DeleteOperation(helpImage));
		}
		
		operations.add(new DeleteOperation(file));
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
		
		private Path toDelete;
		
		public DeleteOperation(Path toDelete) {
			this.toDelete = toDelete;
		}

		@Override
		public void execute() {
			if (Files.exists(toDelete)) {
				try {
					Files.delete(toDelete);
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
			Path from = imgObject.getImageFile();
			Path to = Paths.get(imgObject.getImagePersistenceLocation());
			
			if (from == null || from.equals(IImageAssociatedObject.NULL_FILE)) {
				//find all files that start with "to" and delete them
				//(changed for 6.1 when we support multiple image formats)
				if (!Files.exists(to.getParent())) {
					SmartUtils.createDirectory(to.getParent());
				}
				
				try {
					Files.list( to.getParent() ).forEach(file ->{
						if (file.getFileName().toString().startsWith(to.getFileName().toString() + ".")) { //$NON-NLS-1$
							try {
								Files.delete(file);
							}catch (Exception ex) {
								SmartPlugIn.log("Could not delete cleared file: " + to.toString(), ex); //$NON-NLS-1$	
							}
						}
					});
				} catch (IOException e) {
					SmartPlugIn.log(e.getMessage(),e);
				}
			} else if (Files.exists(from) && !from.equals(to)) {
				//delete any existing files for this node before copying over new files 
				//from 6.1 to support image formats
				String fName = to.getFileName().toString();
				int index = fName.lastIndexOf('.');
				fName = fName.substring(0, index);
				
				if (!Files.exists(to.getParent())) {
					SmartUtils.createDirectory(to.getParent());
				}
				final String fName2 = fName;
				try {
					Files.list( to.getParent() ).forEach(file ->{
						if (file.getFileName().toString().startsWith(fName2 + ".")) { //$NON-NLS-1$
							try {
								Files.delete(file);
							}catch (Exception ex) {
								SmartPlugIn.log("Could not delete cleared file: " + to.toString(), ex); //$NON-NLS-1$	
							}
						}
					});
					
					
					Files.copy(from, to);
				} catch (IOException e) {
					SmartPlugIn.log("Could not copy file: " + from.toString() + " to " + to.toString(), e); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			
			//reset img object file.  If we copy into the system this call will
			//reset the object reference to the file copied in
			imgObject.resetImageFile();
			
			
			
			if (imgObject instanceof CmAttribute || imgObject instanceof CmAttributeOption) {
				//DO the same for the help image
				CmAttribute att = null;
				if (imgObject instanceof CmAttributeOption) {
					att = ((CmAttributeOption)imgObject).getCmAttribute();
				}else {
					att = (CmAttribute)imgObject;
				}

				if (att.getImportHelpFile() != null) {
					//delete all existinf iles
					deleteFiles(Paths.get(att.getNode().getModel().getFileDataStoreLocation()), att.getHelpImageFileRootName());
					//copy import to image path
					try {
						Path imagePath = Paths.get(att.getNode().getModel().getFileDataStoreLocation())
								.resolve(att.getHelpImageFileRootName() + "." + att.getHelpFormat()); //$NON-NLS-1$
						if (!Files.exists(imagePath.getParent())) Files.createDirectories(imagePath.getParent());
						Files.copy(att.getImportHelpFile(), imagePath, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						SmartPlugIn.log("Could not copy file: " + from.toString() + " to " + to.toString(), e); //$NON-NLS-1$ //$NON-NLS-2$
					}
					att.setImportHelpFile(null);
				}else if (att.getHelpFormat() == null) {
					//no image; we want to delete any existing images that may be set for the node
					deleteFiles(Paths.get(att.getNode().getModel().getFileDataStoreLocation()), att.getHelpImageFileRootName());
				}
			}
		}
	}
	
	private void deleteFiles(Path searchPath, String filename) {
		
		if (Files.exists(searchPath)) {
			//delete any files in dir searchPath with the name fname
			List<Path> toDelete = new ArrayList<>();
			try(Stream<Path> kids = Files.list(searchPath)){
				kids.forEach(kid->{
					if (SharedUtils.getFilenameWithoutExtension(kid.getFileName().toString()).equals(filename)) {
						toDelete.add(kid);
					}
				});
			} catch (IOException e) {
				SmartPlugIn.log("Could not list files in directory: " + searchPath.toString(), e); //$NON-NLS-1$ 
			}
			for (Path d : toDelete) {
				try {
					Files.deleteIfExists(d);
				} catch (IOException e) {
					SmartPlugIn.log("Could not delete file:" + d.toString(), e); //$NON-NLS-1$ 
				}
			}
		}
	}
}
