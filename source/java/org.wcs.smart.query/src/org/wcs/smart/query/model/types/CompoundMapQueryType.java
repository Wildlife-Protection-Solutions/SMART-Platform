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
package org.wcs.smart.query.model.types;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.CompoundMapQuery;
import org.wcs.smart.query.common.model.CompoundMapQueryLayer;
import org.wcs.smart.query.compound.ui.CompoundDefinitionPanel;
import org.wcs.smart.query.compound.ui.CompoundQueryDropFactory;
import org.wcs.smart.query.compound.ui.CompoundQueryEditor;
import org.wcs.smart.query.compound.ui.QueryDropItem;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryResultInfoProvider;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.query.ui.model.IDropItemFactory;

/**
 * Query type repesentation for Compound Map Queries
 * @author Emily
 *
 */
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
		return Messages.CompoundMapQueryType_MapQueryName;
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
				List<CompoundMapQueryLayer> newLayers = new ArrayList<CompoundMapQueryLayer>();
				
				for (DropItem it : ((CompoundDefinitionPanel)p).getItems()){
					if (it instanceof QueryDropItem){
						QueryDropItem qitem = (QueryDropItem)it;
						if (qitem.getLayer() != null){
							//update date filter
							qitem.getLayer().setDateFilter(qitem.getDateFilter().asString());
							newLayers.add(qitem.getLayer());
						}else{
							CompoundMapQueryLayer newlayer = new CompoundMapQueryLayer();
							newlayer.setQueryType(qitem.getQueryType().getKey());
							newlayer.setQueryUuid(qitem.getQueryUuid());
							newlayer.setDateFilter(qitem.getDateFilter().asString());
							newlayer.setMapQuery(cq);
							qitem.initializeData(newlayer);
							newLayers.add(newlayer);
						}
					}
				}
				for (int i = 0; i < newLayers.size(); i ++){
					newLayers.get(i).setOrder(i+1);
				}
				cq.getLayers().clear();
				cq.getLayers().addAll(newLayers);
			}
		}
	}

	@Override
	public String validateQuery(List<IDefinitionPanel> components) {
		String error = null;
		for (IDefinitionPanel p : components){
			error = p.validate();
		}
		if (error != null){
			return error;
		}
		//TODO: could potentially validate each individual query
		return null;
	}

	@Override
	public URL getDescription() {
		IPath path = new Path("src/org/wcs/smart/query/model/types/compound.html");  //$NON-NLS-1$
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
	
	/**
	 * 
	 * @return true if query type is supported in reports;
	 * false otherwise
	 */
	public boolean supportsReports(){
		return false;
	}

}
