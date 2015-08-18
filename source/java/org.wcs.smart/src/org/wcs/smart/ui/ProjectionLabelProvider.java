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
package org.wcs.smart.ui;

import org.eclipse.jface.viewers.LabelProvider;
import org.opengis.referencing.FactoryException;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.util.ReprojectUtils;

/**
 * Label provider for projection objects
 * @author egouge
 *
 */
public class ProjectionLabelProvider extends LabelProvider{

	private static ProjectionLabelProvider instance;
	
	protected ProjectionLabelProvider(){
		
	}
	/**
	 * 
	 * @return project label provder
	 */
	public static ProjectionLabelProvider getInstance(){
		if (instance == null){
			instance = new ProjectionLabelProvider();
		}
		return instance;
		
	}
	
	@Override
	public String getText(Object element){
		if (element instanceof Projection){
			String name =  ((Projection) element).getName();
			if (name != null){
				return name;
			}else{
				try {
					return ReprojectUtils.stringToCrs(((Projection)element).getDefinition()).getName().getCode();
				} catch (FactoryException e) {
					return ""; //$NON-NLS-1$
				}
			}
		}
		return super.getText(element);
	}
}
