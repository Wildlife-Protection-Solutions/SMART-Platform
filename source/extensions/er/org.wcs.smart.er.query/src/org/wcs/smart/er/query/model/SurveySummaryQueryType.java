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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.er.query.ERQueryPlugIn;
import org.wcs.smart.er.query.filter.MissionEndDateField;
import org.wcs.smart.er.query.filter.MissionStartDateField;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.internal.parser.Parser;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.er.query.ui.editor.SurveySummaryEditor;
import org.wcs.smart.er.query.ui.panels.ISurveyPanel;
import org.wcs.smart.er.query.ui.panels.definition.FilterDefintionPanel;
import org.wcs.smart.er.query.ui.panels.definition.SummaryDefinitionPanel;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.model.filter.date.WaypointDateField;
import org.wcs.smart.query.ui.definition.ConservationAreaFilterPanel;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.query.ui.model.IDropItemFactory;
/**
 * Summary query type
 * @author Emily
 *
 */
public class SurveySummaryQueryType implements IQueryType {
	
	public static final String KEY = "surveysummary"; //$NON-NLS-1$
	
	/**
	 * @see org.wcs.smart.query.model.IQueryType#getHibernateClass()
	 */
	@Override
	public Class<? extends Query> getHibernateClass() {
		return SurveySummaryQuery.class;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getKey()
	 */
	@Override
	public String getKey() {
		return KEY;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getGuiName()
	 */
	@Override
	public String getGuiName() {
		return Messages.SurveySummaryQueryType_QueryTypeName;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getEditorId()
	 */
	@Override
	public String getEditorId() {
		return SurveySummaryEditor.ID;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#getImage()
	 */
	@Override
	public Image getImage() {
		return ERQueryPlugIn.getDefault().getImageRegistry().get(ERQueryPlugIn.SUMMARY_ICON);
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
		return SurveyDropItemFactory.INSTANCE;
	}

	/**
	 * @see org.wcs.smart.query.model.IQueryType#updateQueryDefinition(org.wcs.smart.query.model.Query, java.util.List)
	 */
	@Override
	public void updateQueryDefinition(Query query, List<IDefinitionPanel> components) {
	
		SurveySummaryQuery summary = (SurveySummaryQuery)query;
		
		String filters= ""; //$NON-NLS-1$
		String definition = ""; //$NON-NLS-1$
		
		for (IDefinitionPanel p : components){
			if (p.getId().equals(FilterDefintionPanel.ID)){
				filters = p.getQueryPart();
			}else if (p.getId().equals(SummaryDefinitionPanel.ID)){
				definition = p.getQueryPart();
			}else if (p.getId().equals(ConservationAreaFilterPanel.ID)){
				query.setConservationAreaFilter(p.getQueryPart());
			}
			
			if (p instanceof ISurveyPanel){
				summary.setSurveyDesign(((ISurveyPanel)p).getSurveyDesign());
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
			
			if (p.getId().equals(FilterDefintionPanel.ID)){
				filters = p.getQueryPart();
			}else if (p.getId().equals(SummaryDefinitionPanel.ID)){
				definition = p.getQueryPart();
			}
			
		}
		
		//validate query
		String queryString = definition + "|" + filters; //$NON-NLS-1$
		System.out.println(queryString);
		InputStream is = new ByteArrayInputStream(queryString.getBytes());
		try{
			Parser parser = new Parser(is);
			parser.SumQuery();
		}catch (Exception ex){
			ex.printStackTrace();
			return ex.getMessage();
		}finally{
			try {
				is.close();
			} catch (IOException e) {
				//eatme
			}
		}
		return null;
	}

	public static IDateFieldFilter[] validDateFields(){
		return new IDateFieldFilter[]{WaypointDateField.INSTANCE, MissionStartDateField.INSTANCE, MissionEndDateField.INSTANCE};
	}
	
	
	@Override
	public URL getDescription() {
		IPath path = new Path("src/org/wcs/smart/er/query/model/types/surveysummary.html"); //$NON-NLS-1$
		return QueryPlugIn.findHelpURL(path, ERQueryPlugIn.getDefault().getBundle());
	}
}