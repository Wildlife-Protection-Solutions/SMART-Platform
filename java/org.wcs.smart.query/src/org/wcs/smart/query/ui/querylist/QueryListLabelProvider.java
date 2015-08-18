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
package org.wcs.smart.query.ui.querylist;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.editor.QueryEditorInput;

/**
 * Label provider for the query list view.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryListLabelProvider extends LabelProvider {

	/**
	 * Creates a new label provider
	 */
	public QueryListLabelProvider(){
	}
	
	/**
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	@Override 
	public Image getImage(Object element) {
		if (element instanceof QueryFolder){
			return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.FOLDER_ICON);
		}else if (element instanceof QueryEditorInput){
			return ((QueryEditorInput)element).getType().getImage();
		}else if (element instanceof Query){
			return QueryTypeManager.INSTANCE.findQueryType(((Query)element).getTypeKey()).getImage();
		}
		return null;
	}
	
	
	/**
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		if (element instanceof QueryFolder){
			return ((QueryFolder) element).getName() ;
		}else if (element instanceof QueryEditorInput){
			return ((QueryEditorInput) element).getName() + " [" + ((QueryEditorInput)element).getId() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
		}else if (element instanceof Query){
			return ((Query)element).getName() + " [" + ((Query)element).getId() + "]";  //$NON-NLS-1$//$NON-NLS-2$
		}
		return super.getText(element);
	}

}
