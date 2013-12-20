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
package org.wcs.smart.query.event;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.IDataModelListener;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.ui.editor.IQueryEditor;
import org.wcs.smart.query.ui.itempanel.QueryItemView;

/**
 * A listener that deals with update the
 * query perspective when the data model is
 * modified.  The QueryItemView is refreshed and all
 * query editor queries are reparsed.
 * 
 * @author Emily
 * 
 */
public class QueryDataModelModifiedListener implements IDataModelListener {

	private List<IQueryEditor> editors = new ArrayList<IQueryEditor>();
	private QueryItemView itemView = null;
	
	IPartListener2 partListener = new IPartListener2() {
		
		@Override
		public void partVisible(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partOpened(IWorkbenchPartReference partRef) {
			IWorkbenchPart part = partRef.getPart(false);
			if (part instanceof IQueryEditor){
				editors.add((IQueryEditor)part);
			}else if (part instanceof QueryItemView){
				itemView = (QueryItemView) part;
			}
		}
		
		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partHidden(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partClosed(IWorkbenchPartReference partRef) {
			IWorkbenchPart part = partRef.getPart(false);
			if (part instanceof IQueryEditor){
				editors.remove((IQueryEditor)part);
			}else if (part instanceof QueryItemView){
				itemView = null;
			}
			
		}
		
		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {
		}
		
		@Override
		public void partActivated(IWorkbenchPartReference partRef) {
		}
	};
	
	
	public QueryDataModelModifiedListener() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(partListener);
		DataModelManager.getInstance().addChangeListener(this);
	}
	
	public void dispose(){
		DataModelManager.getInstance().removeChangeListener(this);
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().removePartListener(partListener);
	}
	

	@Override
	public void modified() {
		// clear the current data model
		QueryDataModelManager.getInstance().clearDataModel();
		
		for (IQueryEditor e : editors){
			e.reparseQuery();
		}
		if (itemView != null){
			itemView.refresh();
		}

	}

}
