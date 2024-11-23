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
package org.wcs.smart.patrol.internal.ui.views;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.dnd.TransferData;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.IFolder;
import org.wcs.smart.common.folder.FolderTreeDropListener;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.views.PatrolTreeContentProvider.GroupByType;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolFolder;
import org.wcs.smart.patrol.ui.PatrolEditorInput;

/**
 * Drop listener for re-ordering folder items.
 *
 * @author elitvin
 * @since 6.0.0
 */
public class PatrolFolderTreeDropListener extends FolderTreeDropListener {

	private PatrolTreeContentProvider contentProvider;
	
	/**
	 * @param viewer
	 * @param contentProvider 
	 */
	protected PatrolFolderTreeDropListener(TreeViewer viewer, PatrolTreeContentProvider contentProvider) {
		super(viewer, contentProvider);
		this.contentProvider = contentProvider;
	}
	
	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		if (contentProvider.getGroupBy() != GroupByType.FOLDER) {
			return false;
		}
		return super.validateDrop(target, operation, transferType);
	}

	@Override
	protected void moveObjectToFolder(Object obj, IFolder targetFolder) {
		if (!(obj instanceof PatrolEditorInput)) {
			SmartPlugIn.displayError(Messages.PatrolFolderTreeDropListener_UnexpectedSource2, null);
			return;
		}
		if (targetFolder != null && !(targetFolder instanceof PatrolFolder)) {
			SmartPlugIn.displayError(Messages.PatrolFolderTreeDropListener_UnexpectedTarget2, null);
			return;
		}
		moveObjectToFolder((PatrolEditorInput)obj, (PatrolFolder)targetFolder);
	}

	private void moveObjectToFolder(PatrolEditorInput obj, PatrolFolder targetFolder) {
		Job j = new Job(Messages.PatrolFolderTreeDropListener_MovePatrolJob_Title2) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try(Session s = HibernateManager.openSession()) {
					Patrol p = (Patrol) s.get(Patrol.class, obj.getUuid());
					p.setParentFolder(targetFolder);
					s.beginTransaction();
					s.persist(p);
					s.getTransaction().commit();
				}
				return Status.OK_STATUS;
			}
		};
		j.schedule();
		try {
			j.join();
		} catch (InterruptedException e) {
			SmartPlugIn.displayError(Messages.PatrolFolderTreeDropListener_MovePatrolJob_Error2, e);
		}
	}
	
}
