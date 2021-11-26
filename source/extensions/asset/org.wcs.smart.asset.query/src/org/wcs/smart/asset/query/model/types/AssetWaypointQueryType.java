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
package org.wcs.smart.asset.query.model.types;

import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.asset.query.AssetQueryPlugIn;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.map.udig.QueryService;
import org.wcs.smart.asset.query.model.AssetDropItemFactory;
import org.wcs.smart.asset.query.model.AssetWaypointQuery;
import org.wcs.smart.asset.query.parser.internal.parser.Parser;
import org.wcs.smart.asset.query.ui.editor.AssetSimpleQueryResultEditor;
import org.wcs.smart.asset.query.ui.editor.DeleteObservationResultInfoProvider;
import org.wcs.smart.ca.Area;
import org.wcs.smart.observation.query.model.types.ZoomToInfoProvider;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.model.CustomArea;
import org.wcs.smart.query.model.IMappableQueryType;
import org.wcs.smart.query.model.IQueryResultInfoProvider;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.AreaFilter;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.query.model.filter.date.WaypointLastModifiedDateField;
import org.wcs.smart.query.ui.definition.BasicFilterDefintionPanel;
import org.wcs.smart.query.ui.definition.ConservationAreaFilterPanel;
import org.wcs.smart.query.ui.model.IQueryDefinitionPanel;
import org.wcs.smart.query.ui.model.IQueryDropItemFactory;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
/**
 * Waypoint query type
 * @author Emily
 *
 */
public class AssetWaypointQueryType implements IMappableQueryType {
	
	private static IQueryDropItemFactory dropItemFactory = null;
	
	/**
	 * @see org.wcs.smart.query.model.IQueryType#getHibernateClass()
	 */
	@Override
	public Class<? extends Query> getHibernateClass() {
		return AssetWaypointQuery.class;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getKey()
	 */
	@Override
	public String getKey() {
		return AssetWaypointQuery.KEY;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getGuiName()
	 */
	@Override
	public String getGuiName() {
		return Messages.AssetWaypointQueryType_WaypointQueryTypeName;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getEditorId()
	 */
	@Override
	public String getEditorId() {
		return AssetSimpleQueryResultEditor.ID;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getImage()
	 */
	@Override
	public Image getImage() {
		return AssetQueryPlugIn.getDefault().getImageRegistry().get(AssetQueryPlugIn.WAYPOINT_QUERY_ICON);
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#supportsCrossCaQueries()
	 */
	@Override
	public boolean supportsCrossCaQueries() {
		return true;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#supportsSingleCaQueries()
	 */
	@Override
	public boolean supportsSingleCaQueries() {
		return true;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getDropItemFactory()
	 */
	@Override
	public IQueryDropItemFactory getDropItemFactory() {
		if (dropItemFactory == null){
			dropItemFactory = new AssetDropItemFactory(){
				@Override
				public DropItem[] generateDropItem(Object source, String queryItemPanelId) {
					DropItem[] items = super.generateDropItem(source, queryItemPanelId);
					if (items != null){
						return items;
					}
					if (source instanceof Area){
						items = new DropItem[]{ createAreaDropItem((Area)source, AreaFilter.AreaFilterGeometryType.WAYPOINT) };
					}else if (source instanceof CustomArea) {
						items = new DropItem[] { createCustomAreaDropItem( null, AreaFilter.AreaFilterGeometryType.WAYPOINT ) };
					}
					return items;
					
				}
				
				
			};
		}
		return dropItemFactory;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#updateQueryDefinition(org.wcs.smart.query.model.Query, java.util.List)
	 */
	@Override
	public void updateQueryDefinition(Query query, List<IQueryDefinitionPanel> components) {
		for (IQueryDefinitionPanel p : components){
			if (p.getId().equals(BasicFilterDefintionPanel.ID)){
				((AssetWaypointQuery)query).setQueryFilter(p.getQueryPart());
			}else if (p.getId().equals(ConservationAreaFilterPanel.ID)){
				query.setConservationAreaFilter(p.getQueryPart());
			}
		}
	}
	
	
	/**
	 * @see org.wcs.smart.query.model.IQueryType#validateQuery(java.util.List)
	 */
	public String validateQuery(List<IQueryDefinitionPanel> components) {
		String filters= ""; //$NON-NLS-1$
		
		// validate each panel
		for (IQueryDefinitionPanel p : components){
			String panelError = p.validate();
			if (panelError != null){
				return panelError;
			}
			
			if (p.getId().equals(BasicFilterDefintionPanel.ID)){
				filters = p.getQueryPart();
			}
			
		}
		
		//validate query
		String queryString = filters;
		if (queryString.isEmpty()) return null;
		try(Reader is = new StringReader(queryString)){
			Parser parser = new Parser(is);
			parser.QueryFilter();
		}catch (Throwable ex){
			return ex.getMessage();
		}
		return null;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getDateFilterOptions()
	 */
	@Override
	public IDateFieldFilter[] getDateFilterOptions() {
		return new IDateFieldFilter[]{WaypointDateField.INSTANCE, WaypointLastModifiedDateField.INSTANCE};
	}

	@Override
	public URL getDescription() {
		IPath path = new Path("src/org/wcs/smart/asset/query/model/types/assetincident.html"); //$NON-NLS-1$
		return QueryPlugIn.findHelpURL(path, AssetQueryPlugIn.getDefault().getBundle());
	}
	
	@Override
	public IQueryResultInfoProvider[] getResultProviders(){
		return new IQueryResultInfoProvider[]{
				new AssetResultInfoProvider(),
				new ZoomToInfoProvider(),
				new AssetQueryShowInTableResultProvider(),
				new DeleteObservationResultInfoProvider()
		};
	}
	
	/**
	 * @see org.wcs.smart.query.model.IMappableQueryType#createQueryService(org.wcs.smart.query.model.Query)
	 */
	@Override
	public IQueryService createQueryService(Query query, IProjectionProvider prjProvider){
		return new QueryService((SimpleQuery) query, prjProvider);
	}
	
	/**
	 * 
	 * @return true if query type is supported in reports;
	 * false otherwise
	 */
	public boolean supportsReports(){
		return true;
	}
	
	@Override
	public boolean supportsImageResult() {
		return true;
	}
}
