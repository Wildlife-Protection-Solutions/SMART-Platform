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
package org.wcs.smart.report.ui;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.jobs.IJobChangeListener;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.progress.DeferredTreeContentManager;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.RootReportFolder;
import org.wcs.smart.util.SmartUtils;

/**
 * Content provider for report tree that makes use of
 * deferred loading.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ReportContentProvider extends BaseWorkbenchContentProvider{
	
	private DeferredTreeContentManager manager;
	private RootType type;
	
	private ConservationArea ca;
	
	public enum RootType{SHARED_ONLY, USER_ONLY, ALL};
	
	/**
	 * Creates a new content provide that displays
	 * all folders
	 */
	public ReportContentProvider(){
		this(RootType.ALL);
	}
	
	/**
	 * Creates a new content provider.
	 * @param ownerOnly display only owner owned folders
	 */
	public ReportContentProvider(RootType type){
		this(type, SmartDB.getCurrentConservationArea());
	}
	
	/**
	 * Creates a new content provider.
	 * @param ownerOnly display only owner owned folders
	 */
	public ReportContentProvider(RootType type, ConservationArea ca){
		super();
		this.type = type;
		this.ca = ca;
	}
	
	/**
	 * @see DeferredTreeContentManager#addUpdateCompleteListener(IJobChangeListener)
	 * @param listsner
	 */
	public void addUpdateCompleteListener(IJobChangeListener listsner){
			manager.addUpdateCompleteListener(listsner);
	}
	/**
	 * @see DeferredTreeContentManager#removeUpdateCompleteListener(IJobChangeListener)
	 * @param listsner
	 */
	public void removeUpdateCompleteListener(IJobChangeListener listsner){
		manager.removeUpdateCompleteListener(listsner);
	}
	
	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (ca == SmartDB.getCurrentConservationArea()){
			if (type == RootType.USER_ONLY ){
				return new Object[]{RootReportFolder.USER_ROOT_FOLDER};
			}else if (type == RootType.SHARED_ONLY){
				return new Object[]{RootReportFolder.CA_ROOT_FOLDER};
			}else{
				return new Object[]{
						RootReportFolder.CA_ROOT_FOLDER,
						RootReportFolder.USER_ROOT_FOLDER
				};
			}
		}else{
			if (type == RootType.USER_ONLY ){
				return new Object[]{
						RootReportFolder.createUserRootFolder(ca)
					};
			}else if (type == RootType.SHARED_ONLY){
				return new Object[]{RootReportFolder.createCaRootFolder(ca)};
			}else{
				return new Object[]{
						RootReportFolder.createCaRootFolder(ca),
						RootReportFolder.createUserRootFolder(ca)
				};
			}
		}
	}
	
	/**
	 * @see org.eclipse.ui.model.BaseWorkbenchContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput){
		if(viewer instanceof AbstractTreeViewer){
			manager = new DeferredTreeContentManager((AbstractTreeViewer) viewer);
		}
		super.inputChanged(viewer, oldInput, newInput);
	}
	
	
	
	/**
	 * @see org.eclipse.ui.model.BaseWorkbenchContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element){
		if (element instanceof Report){
			return false;
		}
		if (manager != null){
			if (manager.isDeferredAdapter(element)){
				return manager.mayHaveChildren(element);
			}
		}
		return super.hasChildren(element);
	}
	
	/**
	 * @see org.eclipse.ui.model.BaseWorkbenchContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parent){
		if (manager != null){
			Object[] kids = manager.getChildren(parent);
			if (kids != null){
				return kids;
			}
		}
		Object[] kids = super.getChildren(parent);
		return kids;
	}
	
	public static void sortItems(List<Object> items){
		Collections.sort(items, new Comparator<Object>(){

			@Override
			public int compare(Object a, Object b) {
				if (a instanceof ReportFolder && !(b instanceof ReportFolder)){
					return -1;
				}else if (a instanceof ReportFolder && b instanceof ReportFolder){
					return Collator.getInstance().compare(((ReportFolder)a).getName(), ((ReportFolder)b).getName());
				}else if (b instanceof ReportFolder && !(a instanceof ReportFolder)){
					return 1;
				}else if (a instanceof Report && b instanceof Report){
					return Collator.getInstance().compare(((Report)a).getName(), ((Report)b).getName());
				}
				return Collator.getInstance().compare(a.toString(),b.toString());
		}});
	}
	
	
	public static void assignNames(List<?> items, Session s){
		Language match = null;
		
		for (Object x : items){
			ConservationArea ca = null;
			if (x instanceof ReportFolder){
				ReportFolder f = (ReportFolder)x;
				if (f.getConservationArea().equals(SmartDB.getCurrentConservationArea())){
					continue;
				}
				ca = f.getConservationArea();
			}else if (x instanceof Report){
				Report r = (Report)x;
				r.getConservationArea().getName();
				if (r.getConservationArea().equals(SmartDB.getCurrentConservationArea())){
					continue;
				}
				ca = r.getConservationArea();
			}
			
			NamedItem it = (NamedItem)x;
			
			if (match == null){
				match = SmartUtils.findLanguageMatch(ca.getLanguages());
				if (match == null){
					match = ca.getDefaultLanguage();
				}
			}
			it.setName(ReportContentProvider.findLabel(match, it.getUuid(), ca, s));	
		}
	}
	private static String findLabel(Language match, byte[] item, ConservationArea currentCa, Session session){
		org.wcs.smart.ca.Label.LabelItemPK lid = new org.wcs.smart.ca.Label.LabelItemPK();
		lid.setElement(new UuidItem(item));
		lid.setLanguage(match);
		
		org.wcs.smart.ca.Label ll = (org.wcs.smart.ca.Label) session.load(org.wcs.smart.ca.Label.class, lid);
		if (ll == null){
			lid.setLanguage(currentCa.getDefaultLanguage());
			ll = (org.wcs.smart.ca.Label) session.load(org.wcs.smart.ca.Label.class, lid);
			if (ll != null){
				return ll.getValue();
			}
		}else{
			return ll.getValue();
		}
		return ""; //$NON-NLS-1$
	}
}