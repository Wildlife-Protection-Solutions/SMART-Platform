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
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.query.AssetQueryPlugIn;
import org.wcs.smart.asset.query.internal.Messages;
import org.wcs.smart.asset.query.map.udig.QueryService;
import org.wcs.smart.asset.query.model.AssetDropItemFactory;
import org.wcs.smart.asset.query.model.AssetSummaryQuery;
import org.wcs.smart.asset.query.parser.internal.filter.AssetDeploymentDateField;
import org.wcs.smart.asset.query.parser.internal.parser.Parser;
import org.wcs.smart.asset.query.parser.internal.summary.AssetGroupBy;
import org.wcs.smart.asset.query.parser.internal.summary.AssetValueItem;
import org.wcs.smart.asset.query.ui.definition.AssetFilterPanel;
import org.wcs.smart.asset.query.ui.definition.AssetSummaryGroupByValuePanel;
import org.wcs.smart.asset.query.ui.editor.AssetSummaryEditor;
import org.wcs.smart.asset.query.ui.itempanel.SummaryDeploymentItemPanel;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.model.IMappableQueryType;
import org.wcs.smart.query.model.IQueryResultInfoProvider;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.summary.AreaGroupBy;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.SumQueryDefinition;
import org.wcs.smart.query.ui.definition.ConservationAreaFilterPanel;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.query.ui.model.IDropItemFactory;

/**
 * Summary query type
 * @author Emily
 *
 */
public class AssetDeploymentSummaryQueryType implements IMappableQueryType {
		
	private static IDropItemFactory dropItemFactory = null;
	
	/**
	 * @see org.wcs.smart.query.model.IQueryType#getHibernateClass()
	 */
	@Override
	public Class<? extends Query> getHibernateClass() {
		return AssetSummaryQuery.class;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getKey()
	 */
	@Override
	public String getKey() {
		return AssetSummaryQuery.DEPLOYMENT_SUMMARY_KEY;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getGuiName()
	 */
	@Override
	public String getGuiName() {
		return Messages.AssetDeploymentSummaryQueryType_QueryTypeName;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getEditorId()
	 */
	@Override
	public String getEditorId() {
		return AssetSummaryEditor.ID;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getImage()
	 */
	@Override
	public Image getImage() {
		return AssetQueryPlugIn.getDefault().getImageRegistry().get(AssetQueryPlugIn.SUMMARY_QUERY_ICON);
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
	public IDropItemFactory getDropItemFactory() {
		if (dropItemFactory == null){
			dropItemFactory = new AssetDropItemFactory(){
				@Override
				public DropItem[] generateDropItem(Object source, String queryItemPanelId) {
					DropItem[] items = super.generateDropItem(source, queryItemPanelId);
					
					if (source instanceof Area){
						if (queryItemPanelId.equals(SummaryDeploymentItemPanel.ID)){
							items = new DropItem[]{ createAreaGroupByDropItem((Area)source) };
						}
					}else if (source instanceof AreaType) {
						if (queryItemPanelId.equals(SummaryDeploymentItemPanel.ID)){
							items = new DropItem[]{ createAreaGroupByDropItem((AreaType)source) };
						}
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
	public void updateQueryDefinition(Query query, List<IDefinitionPanel> components) {
	
		AssetSummaryQuery summary = (AssetSummaryQuery)query;
		
		String filters= ""; //$NON-NLS-1$
		String definition = ""; //$NON-NLS-1$
		
		for (IDefinitionPanel p : components){
			if (p.getId().equals(AssetFilterPanel.ID)){
				filters = p.getQueryPart();
			}else if (p.getId().equals(AssetSummaryGroupByValuePanel.ID)){
				definition = p.getQueryPart();
			}else if (p.getId().equals(ConservationAreaFilterPanel.ID)){
				query.setConservationAreaFilter(p.getQueryPart());
			}
		}
		summary.setQuery(definition + "|" + filters); //$NON-NLS-1$
	}
	
	/**
	 * @see org.wcs.smart.query.model.IQueryType#validateQuery(java.util.List)
	 */
	@Override
	public String validateQuery(List<IDefinitionPanel> components) {
		String filters= ""; //$NON-NLS-1$
		String definition = ""; //$NON-NLS-1$
		
		// validate each panel
		for (IDefinitionPanel p : components){
			String panelError = p.validate();
			if (panelError != null){
				return panelError;
			}
			if (p.getId().equals(AssetFilterPanel.ID)){
				filters = p.getQueryPart();
			}else  if (p.getId().equals(AssetSummaryGroupByValuePanel.ID)){
				definition = p.getQueryPart();
			}
		}
		
		//validate query
		String queryString = definition + "|" + filters ; //$NON-NLS-1$
		SumQueryDefinition def = null;
		try(Reader is = new StringReader(queryString)){
			Parser parser = new Parser(is);
			def = parser.SumQuery();			
		}catch (Throwable ex){
			return ex.getMessage();
		}
		
		//if value item include asset time then can group by
		//can only be station, location, asset or assettype
		boolean hasTimeValue = false;
		for (IValueItem vi : def.getValuePart().getValueItems()) {
			if (vi.getClass().equals(AssetValueItem.class)) {
				hasTimeValue = true;
				break;
			}
		}
		if (!hasTimeValue) return null;
		
		List<IGroupBy> all = new ArrayList<>(def.getColumnGroupByPart().getGroupBys());
		all.addAll(def.getRowGroupByPart().getGroupBys());
		for (IGroupBy gb : all) {
			if (!(gb instanceof AssetGroupBy || gb instanceof AreaGroupBy)) {
				return Messages.AssetDeploymentSummaryQueryType_groupbyValidation;
			}
		}
		
		return null;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getDateFilterOptions()
	 */
	@Override
	public IDateFieldFilter[] getDateFilterOptions() {
		return new IDateFieldFilter[]{AssetDeploymentDateField.INSTANCE};
	}

	
	@Override
	public URL getDescription() {
		IPath path = new Path("src/org/wcs/smart/asset/query/model/types/assetsummary.html"); //$NON-NLS-1$
		return QueryPlugIn.findHelpURL(path, AssetQueryPlugIn.getDefault().getBundle());
	}
	
	@Override
	public IQueryResultInfoProvider[] getResultProviders(){
		return new IQueryResultInfoProvider[]{};
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
	public IQueryService createQueryService(Query query, IProjectionProvider prjProvider) {
		try {
			if (AssetSummaryQuery.canAddGeometry( ((AssetSummaryQuery)query).getQueryDefinition() )){
				return new QueryService((AssetSummaryQuery) query);
			}
		}catch (Exception ex) {
			AssetPlugIn.log(ex.getMessage(), ex);
		}
		return null;
	}
}