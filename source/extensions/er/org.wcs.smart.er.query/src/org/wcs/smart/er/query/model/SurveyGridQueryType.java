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
package org.wcs.smart.er.query.model;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.engine.visitors.HasTrackFilterVisitor;
import org.wcs.smart.er.query.engine.visitors.SurveyHasObservationFilterVisitor;
import org.wcs.smart.er.query.filter.MissionEndDateField;
import org.wcs.smart.er.query.filter.MissionStartDateField;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.internal.parser.Parser;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.er.query.ui.editor.SurveyGriddedEditor;
import org.wcs.smart.er.query.ui.panels.ISurveyPanel;
import org.wcs.smart.er.query.ui.panels.definition.GriddedDefinitionPanel;
import org.wcs.smart.er.query.ui.panels.definition.SimpleValueRateFilterPanel;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.visitors.HasObservationValueVisitor;
import org.wcs.smart.query.model.IQueryResultInfoProvider;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.query.model.summary.GridQueryDefinition;
import org.wcs.smart.query.ui.definition.ConservationAreaFilterPanel;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.query.ui.model.IDropItemFactory;
import org.wcs.smart.query.ui.model.impl.AbstractValueDropItem;

/**
 * Patrol Query Type
 * @author Emily
 *
 */
public class SurveyGridQueryType implements IQueryType {

	
	private static IDropItemFactory dropItemFactory;
	
	/**
	 * @see org.wcs.smart.query.model.IQueryType#getHibernateClass()
	 */
	@Override
	public Class<? extends Query> getHibernateClass() {
		return SurveyGriddedQuery.class;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getKey()
	 */
	@Override
	public String getKey() {
		return SurveyGriddedQuery.KEY;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getGuiName()
	 */
	@Override
	public String getGuiName() {
		return Messages.SurveyGridQueryType_QueryTypeName;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getEditorId()
	 */
	@Override
	public String getEditorId() {
		return SurveyGriddedEditor.ID;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getImage()
	 */
	@Override
	public Image getImage() {
		return ERQueryPlugIn.getDefault().getImageRegistry().get(ERQueryPlugIn.GRIDDED_ICON);
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#supportsCrossCaQueries()
	 */
	@Override
	public boolean supportsCrossCaQueries() {
		return false;
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
			dropItemFactory = new SurveyDropItemFactory(){
				@Override
				public DropItem[] generateDropItem(Object source, String queryItemPanelId) {
					DropItem[] items = super.generateDropItem(source, queryItemPanelId);
					if (items != null){
						for (int i = 0; i < items.length; i ++){
							if (items[i] instanceof AbstractValueDropItem){
								((AbstractValueDropItem)items[i]).setEncounterRateOptions(SurveyDropItemFactory.GRID_ENCOUNTER_RATE_ITEMS);
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
	
		SurveyGriddedQuery summary = (SurveyGriddedQuery)query;
		
		String filters= ""; //$NON-NLS-1$
		String definition = ""; //$NON-NLS-1$
		
		for (IDefinitionPanel p : components){
			if (p.getId().equals(SimpleValueRateFilterPanel.ID)){
				filters = p.getQueryPart();
			}else if (p.getId().equals(GriddedDefinitionPanel.ID)){
				definition = p.getQueryPart();
				if (((GriddedDefinitionPanel)p).getCrs() != null){
					summary.setCrsDefinition(((GriddedDefinitionPanel)p).getCrs().toWKT());
				}
			}else if (p.getId().equals(ConservationAreaFilterPanel.ID)){
				query.setConservationAreaFilter(p.getQueryPart());
			}
			if (p instanceof ISurveyPanel){
				summary.setSurveyDesign(((ISurveyPanel) p).getSurveyDesign());
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
			}else if (p.getId().equals(GriddedDefinitionPanel.ID)){
				definition = p.getQueryPart();
			}
		}
		
		//validate query
		String queryString = definition + "|" + filters; //$NON-NLS-1$
		
		GridQueryDefinition def = null;
		try(InputStream is = new ByteArrayInputStream(queryString.getBytes())){
			Parser parser = new Parser(is);
			def = parser.GridQuery();
		}catch (Throwable ex){
			return ex.getMessage();
		}
		
		boolean hasObservationValue = false;
		boolean hasObservationFilter = false;
		boolean hasTrackFilter = false;
		
		HasObservationValueVisitor v1 = new HasObservationValueVisitor();
		def.getValuePart().accept(v1);
		hasObservationValue = v1.hasAttribute() || v1.hasCategory();
		
		SurveyHasObservationFilterVisitor v2 = new SurveyHasObservationFilterVisitor();
		if (def.getValueFilter() != null && def.getValueFilter().getFilter() != null){
			def.getValueFilter().getFilter().accept(v2);
			hasObservationFilter = v2.hasObservationFilter();
		}
		
		if (!hasObservationFilter){
			if (def.getRateFilter() != null && def.getRateFilter().getFilter() != null){
				def.getRateFilter().getFilter().accept(v2);
				hasObservationFilter = v2.hasObservationFilter();
			}
		}

		HasTrackFilterVisitor v3 = new HasTrackFilterVisitor();
		if (def.getValueFilter() != null && def.getValueFilter().getFilter() != null){
			def.getValueFilter().getFilter().accept(v3);
			hasTrackFilter = v3.hasTrack();
		}
		if (!hasTrackFilter){
			if (def.getRateFilter() != null && def.getRateFilter().getFilter() != null){
				def.getRateFilter().getFilter().accept(v3);
				hasTrackFilter = v3.hasTrack();
			}
		}
		
		if (hasObservationValue && hasTrackFilter){
			return Messages.SurveyGridQueryType_SummaryQueryError1;
		}
		if (hasTrackFilter && hasObservationFilter){
			return Messages.SurveyGridQueryType_SummaryQueryError2;
		}
		return null;
	}


	/**
	 * Valid filter fields for query type
	 * @return
	 */
	public static IDateFieldFilter[] validDateFields(){
		return new IDateFieldFilter[]{WaypointDateField.INSTANCE, 
				MissionStartDateField.INSTANCE, MissionEndDateField.INSTANCE};
	}

	@Override
	public URL getDescription() {
		IPath path = new Path("src/org/wcs/smart/er/query/model/types/surveygrid.html"); //$NON-NLS-1$
		return QueryPlugIn.findHelpURL(path, ERQueryPlugIn.getDefault().getBundle());
	}

	@Override
	public IQueryResultInfoProvider[] getResultProviders(){
		return new IQueryResultInfoProvider[]{};
	}
}