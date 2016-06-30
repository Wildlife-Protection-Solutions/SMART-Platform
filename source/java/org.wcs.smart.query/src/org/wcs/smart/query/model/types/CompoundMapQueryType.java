package org.wcs.smart.query.model.types;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.CompoundMapQuery;
import org.wcs.smart.query.common.model.CompoundMapQueryLayer;
import org.wcs.smart.query.compound.engine.CompoundQueryDropFactory;
import org.wcs.smart.query.compound.ui.CompoundDefinitionPanel;
import org.wcs.smart.query.model.IQueryResultInfoProvider;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.ui.editor.CompoundQueryEditor;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.query.ui.model.IDropItemFactory;
import org.wcs.smart.util.UuidUtils;

public class CompoundMapQueryType implements IQueryType {

	@Override
	public Class<? extends Query> getHibernateClass() {
		return CompoundMapQuery.class;
	}

	@Override
	public String getKey() {
		return CompoundMapQuery.TYPE_KEY;
	}

	@Override
	public String getGuiName() {
		return "Compound Map Query";
	}

	@Override
	public String getEditorId() {
		return CompoundQueryEditor.ID;
	}

	@Override
	public Image getImage() {
		return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.COMPOUND_ICON);
	}

	@Override
	public boolean supportsCrossCaQueries() {
		return true;
	}

	@Override
	public boolean supportsSingleCaQueries() {
		return true;
	}

	@Override
	public IDropItemFactory getDropItemFactory() {
		return CompoundQueryDropFactory.INSTANCE;
	}

	@Override
	public void updateQueryDefinition(Query query,
			List<IDefinitionPanel> components) {
		CompoundMapQuery cq = (CompoundMapQuery)query;
		if (cq.getLayers() == null){
			cq.setLayers(new ArrayList<CompoundMapQueryLayer>());
		}
		for (IDefinitionPanel p : components){
			if (p.getId().equals(CompoundDefinitionPanel.ID)){
				HashMap<UUID,CompoundMapQueryLayer> newLayers = new HashMap<UUID, CompoundMapQueryLayer>();
				
				if (!p.getQueryPart().trim().isEmpty()){
					String parts[] = p.getQueryPart().split(",");
					for (String layer : parts){
						String[] bits = layer.split(":");
						CompoundMapQueryLayer newlayer = new CompoundMapQueryLayer();
						newlayer.setQueryType(bits[0]);
						newlayer.setQueryUuid(UuidUtils.stringToUuid(bits[1]));
						newLayers.put(newlayer.getQueryUuid(), newlayer);
						
					}
					List<CompoundMapQueryLayer> toDelete = new ArrayList<CompoundMapQueryLayer>();
					for (CompoundMapQueryLayer existing : cq.getLayers()){
						if (!newLayers.containsKey(existing.getQueryUuid())){
							toDelete.add(existing);
						}
					}
					for (CompoundMapQueryLayer delete : toDelete){
						delete.getMapQuery().getLayers().remove(delete);
						delete.setMapQuery(null);
					}
					
					//update or add all new layers
					for (CompoundMapQueryLayer newLayer : newLayers.values()){
						CompoundMapQueryLayer toUpdate = null;
						for (CompoundMapQueryLayer oldLayer : cq.getLayers()){
							if (oldLayer.getQueryUuid().equals(newLayer.getQueryUuid())){
								toUpdate = oldLayer;
								break;
							}
						}
						if (toUpdate == null){
							cq.getLayers().add(newLayer);
							newLayer.setMapQuery(cq);
						}
					}
				}
			}
		}
	}

	@Override
	public String validateQuery(List<IDefinitionPanel> components) {
		//could potentially validate each individual query
		return null;
	}

	@Override
	public URL getDescription() {
		IPath path = new Path("src/org/wcs/smart/query/model/types/compound.html"); //$NON-NLS-1$
		return QueryPlugIn.findHelpURL(path, QueryPlugIn.getDefault().getBundle());
	}

	@Override
	public IQueryResultInfoProvider[] getResultProviders() {
		return new IQueryResultInfoProvider[]{};
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getDateFilterOptions()
	 */
	@Override
	public IDateFieldFilter[] getDateFilterOptions() {
		return new IDateFieldFilter[]{};
	}

}
