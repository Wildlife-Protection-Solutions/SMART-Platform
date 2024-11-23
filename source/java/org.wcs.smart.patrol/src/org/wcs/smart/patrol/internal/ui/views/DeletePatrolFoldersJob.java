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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolFolder;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.criteria.Root;

/**
 * Job that performs delete operation for {@link PatrolFolder} objects.
 * Job also updates the order of remaining elements and removes association
 * from {@link Patrol} to deleted {@link PatrolFolder}.
 *
 * @author elitvin
 * @since 6.0.0
 */
public class DeletePatrolFoldersJob extends Job {

	private List<PatrolFolder> roots;
	private Collection<PatrolFolder> foldersToDel;

	public DeletePatrolFoldersJob(List<PatrolFolder> roots, Collection<PatrolFolder> foldersToDel) {
		super(Messages.DeletePatrolFoldersJob_Title2);
		this.roots = roots;
		this.foldersToDel = foldersToDel;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		monitor.beginTask(Messages.DeletePatrolFoldersJob_Title2, 1);
		try (Session s = HibernateManager.openSession()) {
			s.beginTransaction();

			Set<PatrolFolder> affectedParents = new HashSet<>();
			for (PatrolFolder f : foldersToDel) {
				PatrolFolder parent = f.getParentFolder();
				affectedParents.add(parent);
				List<PatrolFolder> list = parent != null ? parent.getChildFolders() : roots;
				list.remove(f);
				s.remove(f);
			}
			affectedParents.removeAll(foldersToDel);
			
			for (PatrolFolder parent : affectedParents) {
				List<PatrolFolder> list = parent != null ? parent.getChildFolders() : roots;
				for (int i = 0; i < list.size(); i++) {
					PatrolFolder f = list.get(i);
					f.setFolderOrder(i);
					s.merge(f);
				}
			}

			CriteriaBuilder cb = s.getCriteriaBuilder();
			CriteriaUpdate<Patrol> c = cb.createCriteriaUpdate(Patrol.class);
			Root<Patrol> r = c.from(Patrol.class);
			c.set("parentFolder", null).where(r.get("parentFolder").in(foldersToDel)); //$NON-NLS-1$ //$NON-NLS-2$
			s.createMutationQuery(c).executeUpdate();
			
			s.getTransaction().commit();
		}
		return Status.OK_STATUS;
	}

}
