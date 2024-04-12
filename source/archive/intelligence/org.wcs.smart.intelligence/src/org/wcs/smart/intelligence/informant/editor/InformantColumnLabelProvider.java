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
package org.wcs.smart.intelligence.informant.editor;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.wcs.smart.intelligence.model.Informant;
import org.wcs.smart.intelligence.model.InformantDataKey;

/**
 * Label provider for additional(secure) informant columns 
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class InformantColumnLabelProvider extends ColumnLabelProvider {
	
	private InformantDataKey key;
	
	public InformantColumnLabelProvider(InformantDataKey key) {
		super();
		this.key = key;
	}

	@Override
	public String getText(Object element) {
		if (element instanceof Informant) {
			Informant i = (Informant) element;
			Object value = InformantEditor.getInformant(i,key);
			return value != null ? value.toString() : ""; //$NON-NLS-1$
		}
		return super.getText(element);
	}

}
