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

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TextCellEditor;
import org.wcs.smart.intelligence.informant.SaveInformantJob;
import org.wcs.smart.intelligence.model.Informant;
import org.wcs.smart.intelligence.model.InformantDataKey;

/**
 * EditingSupport for additional(secure) informant columns
 * 
 * @author elitvin
 * @since 3.2.0
 */
public class InformantColumnEditingSupport extends EditingSupport {

	private InformantDataKey key;
	private TextCellEditor textEditor;
	
	public InformantColumnEditingSupport(TableViewer viewer, InformantDataKey key) {
		super(viewer);
		this.key = key;
		textEditor = new TextCellEditor(viewer.getTable());
	}	

	@Override
	protected CellEditor getCellEditor(Object element) {
		return textEditor;
	}

	@Override
	protected boolean canEdit(Object element) {
		return true;
	}

	@Override
	protected Object getValue(Object element) {
		if (element instanceof Informant) {
			Informant i = (Informant) element;
			Object value = i.get(key);
			return value != null ? value.toString() : ""; //$NON-NLS-1$
		}
		return null;
	}

	@Override
	protected void setValue(Object element, Object value) {
		if (element instanceof Informant) {
			Informant i = (Informant) element;
			i.set(key, value);
			SaveInformantJob job = new SaveInformantJob(i);
			job.schedule();
			try {
				job.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		getViewer().refresh();
	}

}
