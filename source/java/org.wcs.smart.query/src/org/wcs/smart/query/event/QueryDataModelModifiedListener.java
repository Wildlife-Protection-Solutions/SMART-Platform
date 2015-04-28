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

import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.MApplicationElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.wcs.smart.ca.datamodel.DataModelManager;
import org.wcs.smart.ca.datamodel.IDataModelListener;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.ui.editor.IQueryEditor;
import org.wcs.smart.query.ui.itempanel.QueryItemView;
import org.wcs.smart.util.E3Utils;

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

	@Inject private EPartService pService;
	@Inject private EModelService mService;
	@Inject private IEclipseContext currentContext;
	
	
	public QueryDataModelModifiedListener() {
		DataModelManager.getInstance().addChangeListener(this);
	}
	
	public void dispose(){
		DataModelManager.getInstance().removeChangeListener(this);
	}
	

	@Override
	public void modified() {
		// clear the current data model
		QueryDataModelManager.getInstance().clearDataModel();
		
		for (MPart p : pService.getParts()){
			Object t = E3Utils.getSourceObject(p);
			if (t instanceof IQueryEditor){
				((IQueryEditor) t).reparseQuery();
			}
		}
		
		//part service does not include parts that are not in the current perspective so we are
		//going to search the model for our query item view.
		List<MPart> allParts = mService.findElements(((MApplication)currentContext.get(MApplication.class)),
				MPart.class,EModelService.IN_ANY_PERSPECTIVE,new org.eclipse.e4.ui.workbench.Selector(){
					@Override
					public boolean select(MApplicationElement element) {
						if (element.getElementId().equals(QueryItemView.ID)){
							return true;
						}
						return false;
					}});  
		if (allParts.size() > 0){
			((QueryItemView)E3Utils.getSourceObject(allParts.get(0))).refresh();	
		}
	}

}
