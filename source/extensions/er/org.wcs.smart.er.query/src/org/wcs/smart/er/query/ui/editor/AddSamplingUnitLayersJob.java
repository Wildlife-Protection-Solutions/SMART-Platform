package org.wcs.smart.er.query.ui.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.commands.DeleteLayersCommand;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.hibernate.SurveyHibernateManager;
import org.wcs.smart.er.map.samplingunit.SamplingUnitGeoResource;
import org.wcs.smart.er.map.samplingunit.SamplingUnitService;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.model.ISurveyQuery;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.model.Query;

public abstract class AddSamplingUnitLayersJob extends Job {

	private SamplingUnitService service = null;

	public AddSamplingUnitLayersJob() {
		super(Messages.SurveySimpleQueryResultEditor_LoadSuJobName);
	}

	public void dispose() {
		if (service != null) {
			CatalogPlugin.getDefault().getLocalCatalog().remove(service);
			service.dispose(null);
			service = null;
		}
	}

	public abstract Query getQuery();

	public abstract Map getMap();

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if (getQuery() instanceof ISurveyQuery) {
			ISurveyQuery qq = (ISurveyQuery) getQuery();

			String sdkey = qq.getSurveyDesign();
			if (sdkey != null) {
				if (service != null && service.getSurveyDesign().getKeyId().equals(sdkey)) {
					// we don't have to do anything
					return Status.OK_STATUS;
				}
				disposeService(monitor);
				Session s = HibernateManager.openSession();
				try {
					SurveyDesign sd = (SurveyDesign) s.load(SurveyDesign.class, sdkey);
					service = new SamplingUnitService(sd);
				
					@SuppressWarnings("unchecked")
					List<IGeoResource> layers = (List<IGeoResource>) service
							.resources(null);
					List<IGeoResource> layersToAdd = new ArrayList<IGeoResource>();
					Set<SamplingUnit.GeometryType> types = SurveyHibernateManager
							.getInstance().getSamplingUnitTypes(sd, s);
					for (IGeoResource layer : layers) {
						if (types.contains(SamplingUnit.GeometryType
								.valueOf(((SamplingUnitGeoResource) layer)
										.getDataType()))) {
							layersToAdd.add(layer);
						}
					}
					AddLayersCommand command = new AddLayersCommand(layersToAdd);
					if (getMap() == null)
						return Status.CANCEL_STATUS;
					getMap().sendCommandASync(command);
				} catch (Exception ex) {
					EcologicalRecordsPlugIn
							.log("Error adding survey design sampling unit layers.", ex); //$NON-NLS-1$
				} finally {
					s.close();
				}
				return Status.OK_STATUS;
			}
		}
		// dispose service
		disposeService(monitor);
		return Status.OK_STATUS;
	}

	private void disposeService(IProgressMonitor monitor) {
		if (service == null)
			return;
		try {
			List<? extends IGeoResource> resources = service.resources(monitor);
			List<ILayer> toDelete = new ArrayList<ILayer>();
			for (IGeoResource r : resources) {
				for (ILayer layer : getMap().getLayersInternal()) {
					if (layer.getID().equals(r.getIdentifier())) {
						toDelete.add(layer);
						break;
					}
				}
			}
			if (toDelete.size() > 0) {
				DeleteLayersCommand cmd = new DeleteLayersCommand(
						toDelete.toArray(new ILayer[toDelete.size()]));
				getMap().sendCommandASync(cmd);
			}
		} catch (IOException e) {
			EcologicalRecordsPlugIn.log(
					"Error disposing survey design sampling unit layers.", e); //$NON-NLS-1$
		}
		CatalogPlugin.getDefault().getLocalCatalog().remove(service);
		service.dispose(monitor);
		service = null;
	}

}
