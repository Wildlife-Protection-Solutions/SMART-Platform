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
package org.wcs.smart.common.folder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.TransferData;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.IFolder;
import org.wcs.smart.hibernate.SaveObjectsJob;
import org.wcs.smart.internal.Messages;

/**
 * Drop listener for re-ordering folder items.
 *
 * @author elitvin
 * @since 6.0.0
 */
public abstract class FolderTreeDropListener extends ViewerDropAdapter {

	private FolderTreeContentProvider folderTreeContentProvider;
	
	/**
	 * @param viewer
	 * @param contentProvider 
	 */
	protected FolderTreeDropListener(TreeViewer viewer, FolderTreeContentProvider contentProvider) {
		super(viewer);
		this.folderTreeContentProvider = contentProvider;
	}

	protected abstract void moveObjectToFolder(Object obj, IFolder targetFolder);

	private void moveToFolder(Object obj, IFolder targetFolder) {
		moveObjectToFolder(obj, targetFolder == NoneFolder.INSTANCE ? null : targetFolder);
		folderTreeContentProvider.applyCurrentGrouping();
	}

	private void moveFolderToFolder(IFolder source, IFolder target) {
		if (source == target || source.equals(target)) {
			return;
		}
		if (target == NoneFolder.INSTANCE) {
			target = null;
		}
		if (source.getParentFolder() == target) {
			return;
		}
		
		//NOTE: <IFolder>.getChildFolders() need to be copied to a separate list to avoid issue caused by "orphanRemoval=true"
		List<? extends IFolder> sourceList = source.getParentFolder() != null ? new ArrayList<>(source.getParentFolder().getChildFolders()) :
			    FolderTreeUtils.getRootFoldersFromImput(folderTreeContentProvider.getElements(null));

		List<? extends IFolder> targetList = target != null ? new ArrayList<>(target.getChildFolders()) : 
			FolderTreeUtils.getRootFoldersFromImput(folderTreeContentProvider.getElements(null));
		
		sourceList.remove(source);

	    source.setParentFolder(target);
	    source.setFolderOrder(targetList.size());

		for (int i = 0; i < sourceList.size(); i ++){
			sourceList.get(i).setFolderOrder(i);
		}
	    
		Set<IFolder> changedFolders = new HashSet<>();
		changedFolders.addAll(sourceList);
		changedFolders.add(source);

		saveChangedFolders(changedFolders);
		
		folderTreeContentProvider.applyCurrentGrouping();
	}

	private void moveFolderPosition(IFolder source, IFolder target, boolean moveBefore) {
		if (source == target || source.equals(target)) {
			return;
		}
		
		//NOTE: <IFolder>.getChildFolders() need to be copied to a separate list to avoid issue caused by "orphanRemoval=true"
		List<? extends IFolder> sourceList = source.getParentFolder() != null ? new ArrayList<>(source.getParentFolder().getChildFolders()) :
			    FolderTreeUtils.getRootFoldersFromImput(folderTreeContentProvider.getElements(null));

		List<? extends IFolder> targetList = source.getParentFolder() == target.getParentFolder() ? sourceList : 
			    target.getParentFolder() != null ?  new ArrayList<>(target.getParentFolder().getChildFolders()) :
				FolderTreeUtils.getRootFoldersFromImput(folderTreeContentProvider.getElements(null));

		sourceList.remove(source);
	    source.setParentFolder(target.getParentFolder() == NoneFolder.INSTANCE ? null : target.getParentFolder());
		for (int i = 0; i < sourceList.size(); i ++) {
			sourceList.get(i).setFolderOrder(i);
		}

	    int index = targetList.indexOf(target);
	    if (index >= 0) {
	    	if (!moveBefore) {
	    		index++;
	    	}
	    	source.setFolderOrder(index);
	    } else {
	    	source.setFolderOrder(targetList.size());
	    }
		int i = 0;
	    for (IFolder f : targetList) {
			if (i == index) {
				i++;
			}
			f.setFolderOrder(i);
			i++;
		}

		Set<IFolder> changedFolders = new HashSet<>();
		changedFolders.add(source);
		changedFolders.addAll(sourceList);
		changedFolders.addAll(targetList);

		saveChangedFolders(changedFolders);

		folderTreeContentProvider.applyCurrentGrouping();
	}

	private void saveChangedFolders(Collection<IFolder> changedFolders) {
		changedFolders.remove(NoneFolder.INSTANCE);
		Job j = new SaveObjectsJob(Messages.FolderTreeDropListener_SaveJob_Title, changedFolders.toArray());
		j.schedule();
		try {
			j.join();
		} catch (InterruptedException e) {
			SmartPlugIn.displayError(Messages.FolderTreeDropListener_SaveJob_Error, e);
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#performDrop(java.lang.Object)
	 */
	@Override
	public boolean performDrop(Object data) {
		StructuredSelection selection = (StructuredSelection)LocalSelectionTransfer.getTransfer().getSelection();
		if (selection == null){
			return false;
		}

		Object obj = selection.getFirstElement();
		if (obj instanceof IFolder) {
			Object target = getCurrentTarget();
			int location = getCurrentLocation();

			if (!(target instanceof IFolder)) {
				target = folderTreeContentProvider.getParent(target);
				location = LOCATION_ON;
			}

			switch (location) {
			case LOCATION_BEFORE:
			case LOCATION_AFTER: {
				moveFolderPosition((IFolder)obj, (IFolder)target, location == LOCATION_BEFORE);
				break;
			}
			default:
				moveFolderToFolder((IFolder)obj, (IFolder)target);
				break;
			}
		} else {
			//item need to be moved to another folder
			Object target = getCurrentTarget();
			if (target instanceof IFolder) {
				moveToFolder(obj, (IFolder)target);
				return true;
			} else {
				target = folderTreeContentProvider.getParent(target);
				if (target instanceof IFolder) {
					moveToFolder(obj, (IFolder)target);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object, int, org.eclipse.swt.dnd.TransferData)
	 */
	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		StructuredSelection selection = (StructuredSelection)LocalSelectionTransfer.getTransfer().getSelection();
		if (selection == null){
			return false;
		}
		Object obj = selection.getFirstElement();
		if (obj == NoneFolder.INSTANCE) {
			return false;
		}
		Object objFolder = folderTreeContentProvider.getParent(obj);
		
		if (obj == target) {
			return false;
		}

		if (obj instanceof IFolder) {
			if (!(target instanceof IFolder) && folderTreeContentProvider.getParent(target) == objFolder) {
				//special case when element(target) and folder share the same parent folder
				return false;
			}
			if (target == NoneFolder.INSTANCE || folderTreeContentProvider.getParent(target) == NoneFolder.INSTANCE) {
				//this is drop to "None" folder or its child => we allow drop for any non-root folder
				return folderTreeContentProvider.getParent(obj) != null;
			}
			return !isInChildren(obj, target);
		} else {
			//item need to be moved to another folder
			if (target instanceof IFolder) {
				return target != objFolder;
			} else {
				return folderTreeContentProvider.getParent(target) != objFolder;
			}
			
		}
	}

	private boolean isInChildren(Object root, Object element) {
		if (root.equals(element)) {
			return true;
		}
		for (Object e : folderTreeContentProvider.getChildren(root)) {
			if (isInChildren(e, element)) {
				return true;
			}
		}
		return false;
	}

}
