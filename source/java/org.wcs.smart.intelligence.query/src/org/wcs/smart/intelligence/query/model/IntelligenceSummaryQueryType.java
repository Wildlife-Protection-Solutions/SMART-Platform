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
import org.wcs.smart.query.model.IQueryResultInfoProvider;
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

	@Override
	public Class<? extends Query> getHibernateClass() {
		return IntelligenceSummaryQuery.class;
	}

	@Override
	public String getKey() {
		return IntelligenceSummaryQuery.KEY;
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

	@Override
	public IQueryResultInfoProvider[] getResultProviders(){
		return new IQueryResultInfoProvider[]{};
	}
}
