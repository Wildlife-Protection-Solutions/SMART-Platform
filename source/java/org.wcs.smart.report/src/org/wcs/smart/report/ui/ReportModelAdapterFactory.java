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

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.RootReportFolder;

/**
 * Adapter factory to adapt report model elements.
 * @author egouge
 * @since 1.0.0
 */
public class ReportModelAdapterFactory implements IAdapterFactory {

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adapterType.isAssignableFrom(IDeferredWorkbenchAdapter.class)){
			if (adaptableObject.getClass() == ReportFolder.class){
				return (T)ReportFolderModelAdapter.adapter;
			}else if (adaptableObject.getClass() == RootReportFolder.class){
				return (T)RootReportFolderModelAdapter.adapter;
			}
		}
		
		return null;
	}

	
	@Override
	public Class<?>[] getAdapterList() {
		return new Class[]{IDeferredWorkbenchAdapter.class};
	}

}
