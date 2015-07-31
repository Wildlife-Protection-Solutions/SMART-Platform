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
package org.wcs.smart.patrol.query.model.types;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.ca.Area;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.query.PatrolQueryPlugIn;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.model.PatrolDropItemFactory;
import org.wcs.smart.patrol.query.model.PatrolEndDateField;
import org.wcs.smart.patrol.query.model.PatrolQueryOption;
import org.wcs.smart.patrol.query.model.PatrolQueryOptions;
import org.wcs.smart.patrol.query.model.PatrolStartDateField;
import org.wcs.smart.patrol.query.model.PatrolSummaryQuery;
import org.wcs.smart.patrol.query.model.PatrolValueOption;
import org.wcs.smart.patrol.query.parser.internal.parser.Parser;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolGroupBy;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolValueItem;
import org.wcs.smart.patrol.query.ui.definition.PatrolSummaryGroupByValuePanel;
import org.wcs.smart.patrol.query.ui.definition.SimpleValueRateFilterPanel;
import org.wcs.smart.patrol.query.ui.definition.dropItems.PatrolDropItems;
import org.wcs.smart.patrol.query.ui.editor.PatrolSummaryEditor;
import org.wcs.smart.patrol.query.ui.itempanel.SummaryFilterPanel;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.AreaFilter;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.query.model.summary.AttributeGroupBy;
import org.wcs.smart.query.model.summary.CategoryGroupBy;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.SumQueryDefinition;
import org.wcs.smart.query.ui.definition.ConservationAreaFilterPanel;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.query.ui.model.IDropItemFactory;
import org.wcs.smart.query.ui.model.impl.AbstractValueDropItem;
/**
 * Summary query type
 * @author Emily
 *
 */
public class PatrolSummaryQueryType implements IQueryType {
		
	private static IDropItemFactory dropItemFactory = null;
	
	/**
	 * @see org.wcs.smart.query.model.IQueryType#getHibernateClass()
	 */
	@Override
	public Class<? extends Query> getHibernateClass() {
		return PatrolSummaryQuery.class;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getKey()
	 */
	@Override
	public String getKey() {
		return PatrolSummaryQuery.KEY;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getGuiName()
	 */
	@Override
	public String getGuiName() {
		return Messages.PatrolSummaryQueryType_SummaryQueryTypeName;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getEditorId()
	 */
	@Override
	public String getEditorId() {
		return PatrolSummaryEditor.ID;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getImage()
	 */
	@Override
	public Image getImage() {
		return PatrolQueryPlugIn.getDefault().getImageRegistry().get(PatrolQueryPlugIn.SUMMARY_QUERY_ICON);
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
			dropItemFactory = new PatrolDropItemFactory(){
				@Override
				public DropItem[] generateDropItem(Object source, String queryItemPanelId) {
					DropItem[] items = super.generateDropItem(source, queryItemPanelId);
					
					if (source instanceof Area){
						if (queryItemPanelId.equals(SummaryFilterPanel.ID)){
							items = new DropItem[]{ createAreaGroupByDropItem((Area)source) };
						}else {
							items = new DropItem[]{ createAreaDropItem((Area)source, AreaFilter.AreaFilterGeometryType.TRACK) };
						}
					}

					if (items != null){
						for (int i = 0; i < items.length; i ++){
							if (items[i] instanceof AbstractValueDropItem){
								((AbstractValueDropItem)items[i]).setEncounterRateOptions(PatrolDropItems.SUMMARY_ENCOUNTER_RATE_DROP_OPTIONS);
							}
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
	
		PatrolSummaryQuery summary = (PatrolSummaryQuery)query;
		
		String filters= ""; //$NON-NLS-1$
		String definition = ""; //$NON-NLS-1$
		
		for (IDefinitionPanel p : components){
			if (p.getId().equals(SimpleValueRateFilterPanel.ID)){
				filters = p.getQueryPart();
			}else if (p.getId().equals(PatrolSummaryGroupByValuePanel.ID)){
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
			
			if (p.getId().equals(SimpleValueRateFilterPanel.ID)){
				filters = p.getQueryPart();
			}else if (p.getId().equals(PatrolSummaryGroupByValuePanel.ID)){
				definition = p.getQueryPart();
			}
			
		}
		
		//validate query
		String queryString = definition + "|" + filters; //$NON-NLS-1$
		try(InputStream is = new ByteArrayInputStream(queryString.getBytes())){
			Parser parser = new Parser(is);
			SumQueryDefinition def = parser.SumQuery();
			
			validateQueryParts(def);
		}catch (Throwable ex){
			return ex.getMessage();
		}
		return null;
	}

	public static IDateFieldFilter[] validDateFields(){
		return new IDateFieldFilter[]{WaypointDateField.INSTANCE,
				PatrolStartDateField.INSTANCE,
				PatrolEndDateField.INSTANCE};
	}
	
	
	/**
	 * Validates the query parts.  Assumes the query
	 * definition is formed from a valid string.
	 * <p>
	 * This validates the items in the query.
	 * </p>
	 * 
	 * @param def the summary query definition
	 * @return error string or null if query validates okay
	 */
	private void validateQueryParts(SumQueryDefinition def) throws Exception{
		List<IGroupBy> groupBys = new ArrayList<IGroupBy>();
		if (def.getRowGroupByPart() != null){
			groupBys.addAll(def.getRowGroupByPart().getGroupBys());
		}
		if (def.getColumnGroupByPart().getGroupBys() != null){
			groupBys.addAll(def.getColumnGroupByPart().getGroupBys());
		}
		
		for (IValueItem valueIt : def.getValuePart().getValueItems()){
			if (valueIt instanceof PatrolValueItem){
				PatrolValueItem pIt = (PatrolValueItem) valueIt;
				PatrolValueOption option = PatrolQueryOptions.findPatrolValueItem(pIt.getPatrolValueOptionKey());
				if (option == PatrolValueOption.NUM_NIGHTS){
					//cannot group by patrol leader, patrol memeber, time period, or transport
					for (IGroupBy groupBy : groupBys){
						if (groupBy instanceof CategoryGroupBy ){
							throw new Exception(
									MessageFormat.format(
									Messages.SummaryQuery_CannotGroupByCategory, new Object[]{option.getGuiName(Locale.getDefault())}));
									
						}else if (groupBy instanceof AttributeGroupBy){
							throw new Exception(MessageFormat.format(
									Messages.SummaryQuery_CannotGroupByAttribute, new Object[]{option.getGuiName(Locale.getDefault())}));
						}
					}
				}else if (option == PatrolValueOption.MAN_DAYS ||
						option == PatrolValueOption.MAN_HOURS || 
								option == PatrolValueOption.NUM_MEMBERS ){
					
					for (IGroupBy groupBy : groupBys){
						if (groupBy instanceof PatrolGroupBy){
							if (((PatrolGroupBy)groupBy).getOption().equals(PatrolQueryOption.EMPLOYEE)){
								throw new Exception( MessageFormat.format(
										Messages.SummaryQuery_GroupByError3 , new Object[]{option.getGuiName(Locale.getDefault()), PatrolQueryOption.EMPLOYEE.getGuiName(Locale.getDefault())}));
							}
						}
					}
				}
			}
		}
	}
	
	@Override
	public URL getDescription() {
		IPath path = new Path("src/org/wcs/smart/patrol/query/model/types/patrolsummary.html"); //$NON-NLS-1$
		return QueryPlugIn.findHelpURL(path, PatrolQueryPlugIn.getDefault().getBundle());
	}
}