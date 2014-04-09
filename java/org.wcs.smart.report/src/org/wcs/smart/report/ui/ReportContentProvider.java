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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.RootReportFolder;
/**
 * A report content provide that expects everything to be loaded
 * into memory.
 * 
 * <p>
 *  Input for this content provided is expected to an array that
 *  contains two lists of objects.  The objects can be Report or ReportFolder.
 *  The first array is the shared reports, the second the user reports.  Either
 *  can be null which means that node of the tree won't be displayed.
 * 
 * @author Emily
 *
 */
public class ReportContentProvider implements ITreeContentProvider{

	private RootReportFolder userFolder;
	private RootReportFolder caFolder;
	
	private List<Object> userItems;
	private List<Object> caItems;

	private String loadingLabel = null;
	
	/**
	 * Creates a new content provider.
	 * 
	 */
	public ReportContentProvider(){

	}
	
	@Override
	public void dispose() {
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null){
			return;
		}
		if (newInput instanceof String){
			loadingLabel = (String) newInput;
			return;
		}
		loadingLabel = null;
		Object[] items = (Object[])newInput;
		if (items[0] == null){
			caFolder = null;
			caItems = null;
		}else{
			caFolder = RootReportFolder.CA_ROOT_FOLDER;
			caItems = (List<Object>) items[0];
		}
		if (items[1] == null){
			userFolder = null;
			userItems = null;
		}else{
			userFolder = RootReportFolder.USER_ROOT_FOLDER;
			userItems = (List<Object>) items[1];
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (loadingLabel != null){
			return new Object[]{loadingLabel};
		}
		if (caFolder != null && userFolder != null){
			return new Object[]{caFolder, userFolder};
		}else if (caFolder == null && userFolder != null){
			return new Object[]{userFolder};
		}else if (caFolder != null && userFolder == null){
			return new Object[]{caFolder};
		}
		return null;
	}
	
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof Report){
			return null;
		}else if (parentElement == caFolder || parentElement == userFolder){
			List<Object> roots = null;
			if (parentElement == caFolder){
				roots = caItems;
			}else{
				roots = userItems;
			}
			List<Object> items = new ArrayList<Object>();
			for (Object x : roots){
				if (x instanceof Report){
					if (((Report) x).getFolder() == null){
						items.add(x);
					}
				}else if (x instanceof ReportFolder){
					if (((ReportFolder)x).getParentFolder() == null){
						items.add(x);
					}
				}
			}
			return items.toArray();
		}else if (parentElement instanceof ReportFolder){
			List<Object> kids = new ArrayList<Object>();
			kids.addAll(((ReportFolder)parentElement).getChildren());
			if (caItems != null){
				for (Object x : caItems){
					if (x instanceof Report && ((Report)x).getFolder() == parentElement){
						kids.add(x);
					}
				}
			}
			if (userItems != null){
				for (Object x : userItems){
					if (x instanceof Report && ((Report)x).getFolder() == parentElement){
						kids.add(x);
					}
				}
			}
			return kids.toArray();
		}
		return null;
	}

	 /* (non-Javadoc)
     * Method declared on ITreeContentProvider.
     */
	@Override
    public Object getParent(Object element) {
		if (element instanceof Report){
			Object parent = ((Report) element).getFolder();
			if (parent == null){
				if (((Report) element).getShared()){
					return caFolder;
				}else{
					return userFolder;
				}
			}
			return parent;
		}else if (element instanceof ReportFolder){
			if (((ReportFolder) element).getParentFolder() == null){
				if (((ReportFolder)element).getEmployee() == null){
					return caFolder;
				}else{
					return userFolder;
				}
			}else{
				return ((ReportFolder) element).getParentFolder();
			}
		}
		return null;
    }

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof Report ){
			return false;
		}
		return true;
	}
	
	
	
	
}
