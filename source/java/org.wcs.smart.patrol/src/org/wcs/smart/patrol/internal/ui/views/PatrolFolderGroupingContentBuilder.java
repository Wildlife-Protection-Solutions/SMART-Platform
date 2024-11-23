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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.folder.IGroupContentBuilder;
import org.wcs.smart.common.folder.ITreeElement;
import org.wcs.smart.common.folder.NoneFolder;
import org.wcs.smart.common.folder.TreeElement;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolFolder;
import org.wcs.smart.patrol.ui.PatrolEditorInput;

/**
 * Performs grouping of patrols based on folders that patrols are associated with.
 *
 * @author elitvin
 * @since 6.0.0
 */
public class PatrolFolderGroupingContentBuilder implements IGroupContentBuilder {

	@Override
	public List<ITreeElement> applyGrouping(Object input) {
		if (input instanceof PatrolEditorInput[]) {
			PatrolEditorInput[] patrols = (PatrolEditorInput[]) input;
			List<ITreeElement> result = new ArrayList<>();
			
			Job j = new Job(Messages.PatrolTreeContentProvider_JobName2) {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try(Session s = HibernateManager.openSession()) {
						List<PatrolFolder> rootFolders = PatrolHibernateManager.getRootPatrolFolders(s);
						List<ITreeElement> roots = new ArrayList<>(rootFolders.size()+1);
						
						Map<UUID, ITreeElement> foldersMap = new HashMap<>();
						ITreeElement noneTe = new TreeElement(NoneFolder.INSTANCE, null);
						foldersMap.put(null, noneTe);
						roots.add(noneTe);
						for (PatrolFolder f : rootFolders) {
							ITreeElement te = new TreeElement(f, null);
							foldersMap.put(f.getUuid(), te);
							roots.add(te);
							List<ITreeElement> children = addChildrenToMap(foldersMap, f);
							te.getChildren().addAll(children);
						}
						
						for (PatrolEditorInput p : patrols) {
							Patrol pp = (Patrol) s.get(Patrol.class, p.getUuid());
							ITreeElement el = foldersMap.get(pp.getParentFolder() != null ? pp.getParentFolder().getUuid() : null);
							if(el == null) {
								el = foldersMap.get(null); //this should never happen
							}
							el.getChildren().add(new TreeElement(p, el.getObject()));
						}
						result.addAll(roots);
					}
					return Status.OK_STATUS;
				}

				private List<ITreeElement> addChildrenToMap(Map<UUID, ITreeElement> foldersMap, PatrolFolder parentFolder) {
					List<ITreeElement> teList = new ArrayList<>(parentFolder.getChildFolders().size());
					for (PatrolFolder f : parentFolder.getChildFolders()) {
						ITreeElement te = new TreeElement(f, parentFolder);
						foldersMap.put(f.getUuid(), te);
						teList.add(te);
						List<ITreeElement> children = addChildrenToMap(foldersMap, f);
						te.getChildren().addAll(children);
					}
					return teList;
				}
			};
			
			j.schedule();
			try {
				j.join();
			} catch (InterruptedException e) {
				SmartPlugIn.displayError(Messages.PatrolFolderGroupingContentBuilder_SortFoldersJob_Error2, e);
			}
			
			return result;
		}
		return null;
	}

}
