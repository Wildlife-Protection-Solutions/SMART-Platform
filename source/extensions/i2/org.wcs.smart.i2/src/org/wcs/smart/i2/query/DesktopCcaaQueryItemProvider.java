/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.query;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.DataModelMerger;
import org.wcs.smart.ca.datamodel.SimpleDataModel;

public class DesktopCcaaQueryItemProvider extends CcaaQueryItemProvider {

	public DesktopCcaaQueryItemProvider(Collection<ConservationArea> conservationAreas, ConservationArea queryCa) {
		super(conservationAreas, queryCa);
	}

	@Override
	public void reset() {
		mergedDataModel = null;
	}
	
	@Override
	protected SimpleDataModel getDataModel(Session session) {
		if (mergedDataModel == null) {
			synchronized (this) {
				if (mergedDataModel != null) return mergedDataModel;				
				Display.getDefault().syncExec(()->{
					DataModelMerger merger = new DataModelMerger();
					final ConservationArea[] cas =getConservationAreas().toArray(new ConservationArea[getConservationAreas().size()]);
					final ConservationArea defaultCa = getMainConservationArea();
					ProgressMonitorDialog pmd = new ProgressMonitorDialog(Display.getDefault().getActiveShell());
					try {
						pmd.run(true, false, new IRunnableWithProgress() {
							@Override
							public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
								mergedDataModel = merger.mergeDataModels(cas, defaultCa, session, null, new NullProgressMonitor());
								
							}
						});
					}catch (Exception ex) {
						throw new IllegalStateException("Could not merge datamodels: " + ex.getMessage(), ex); //$NON-NLS-1$
					}
				});
			}
		}
		return mergedDataModel;
	}

}
