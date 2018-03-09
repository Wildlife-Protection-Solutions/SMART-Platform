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
