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
package org.wcs.smart.intelligence.query.model;

import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.intelligence.query.IntelligenceQueryPlugIn;
import org.wcs.smart.intelligence.query.internal.Messages;
import org.wcs.smart.intelligence.query.ui.IntelligenceSummaryEditor;
import org.wcs.smart.intelligence.query.ui.dropitem.IntelligenceDropItemFactory;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.model.SummaryHeader;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.query.ui.model.IDropItemFactory;

/**
 * Query Type class for intelligence summary queries.
 * 
 * @author Emily
 *
 */
public class IntelligenceSummaryQueryType implements IQueryType {

	public static final String KEY = "intelligencesummary"; //$NON-NLS-1$

	@Override
	public Class<? extends Query> getHibernateClass() {
		return IntelligenceSummaryQuery.class;
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getGuiName() {
		return Messages.IntelligenceSummaryQueryType_TypeName;
	}

	@Override
	public String getEditorId() {
		return IntelligenceSummaryEditor.ID;
	}

	@Override
	public Image getImage() {
		return IntelligenceQueryPlugIn.getDefault().getImageRegistry().get(IntelligenceQueryPlugIn.INTEL_SUMMARY_ICON);
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
		return IntelligenceDropItemFactory.INSTANCE;
	}

	@Override
	public void updateQueryDefinition(Query query,
			List<IDefinitionPanel> components) {
	}

	@Override
	public String validateQuery(List<IDefinitionPanel> components) {
		return null;
	}

	@Override
	public URL getDescription() {
		IPath path = new Path("src/org/wcs/smart/intelligence/query/model/summary.html"); //$NON-NLS-1$
		return QueryPlugIn.findHelpURL(path, IntelligenceQueryPlugIn.getDefault().getBundle());
	}
	
	
	public static final String FOLLOW_KEY = "follow"; //$NON-NLS-1$
	public static final String NOT_FOLLOW_KEY = "notfollow"; //$NON-NLS-1$
	public static final String NUMBER_KEY = "intellcnt"; //$NON-NLS-1$
	
	/**
	 * Creates the template for the results.  These queries
	 * have on value (Number of Intelligence) grouped into either
	 * Followed Up or Not Followed Up.
	 * 
	 * @return
	 */
	public static SummaryQueryResult createResultTemplate(){
		SummaryQueryResult results = new SummaryQueryResult();
		
		results.addValueHeader(
				new SummaryHeader(Messages.SummaryIntelligenceQueryEngine_NumberRecordShortName, Messages.SummaryIntelligenceQueryEngine_NumberRecordLongName, NUMBER_KEY, true));
		
		results.addRowHeader(
				new SummaryHeader[]{new SummaryHeader(Messages.SummaryIntelligenceQueryEngine_FollowedUpHeaderLongName, Messages.SummaryIntelligenceQueryEngine_FollowedUpHeaderShortName, FOLLOW_KEY, false), 
				new SummaryHeader(Messages.SummaryIntelligenceQueryEngine_NotFollowedUpHeaderLongName, Messages.SummaryIntelligenceQueryEngine_NoFollowedUpHeaderShortName, NOT_FOLLOW_KEY, false)}); 

		return results;
	}
	

}
