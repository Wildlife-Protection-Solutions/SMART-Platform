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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.intelligence.query.IntelligenceQueryPlugIn;
import org.wcs.smart.intelligence.query.internal.Messages;
import org.wcs.smart.intelligence.query.parser.Parser;
import org.wcs.smart.intelligence.query.ui.DefinitionPanel;
import org.wcs.smart.intelligence.query.ui.IntelligenceRecordEditor;
import org.wcs.smart.intelligence.query.ui.dropitem.IntelligenceDropItemFactory;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.ui.definition.ConservationAreaFilterPanel;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.query.ui.model.IDropItemFactory;

/**
 * Intelligence record query type class.
 * 
 * @author Emily
 *
 */
public class IntelligenceRecordQueryType implements IQueryType {

	public static final String KEY = "intelligencerecord"; //$NON-NLS-1$

	@Override
	public Class<? extends Query> getHibernateClass() {
		return IntelligenceRecordQuery.class;
	}

	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getGuiName() {
		return Messages.IntelligenceRecordQueryType_QueryTypeName;
	}

	@Override
	public String getEditorId() {
		return IntelligenceRecordEditor.ID;
	}

	@Override
	public Image getImage() {
		return IntelligenceQueryPlugIn.getDefault().getImageRegistry().get(IntelligenceQueryPlugIn.INTEL_RECORD_ICON);
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
		for (IDefinitionPanel def : components){
			if (def.getId().equals(DefinitionPanel.ID)){
				((IntelligenceRecordQuery)query).setQueryFilter(def.getQueryPart());
			}else if (def.getId().equals(ConservationAreaFilterPanel.ID)){
				((IntelligenceRecordQuery)query).setConservationAreaFilter(def.getQueryPart());
			}
		}
	}

	@Override
	public String validateQuery(List<IDefinitionPanel> components) {
		String filters= ""; //$NON-NLS-1$
		
		// validate each panel
		for (IDefinitionPanel p : components){
			String panelError = p.validate();
			if (panelError != null){
				return panelError;
			}
			
			if (p.getId().equals(DefinitionPanel.ID)){
				filters = p.getQueryPart();
			}
			
		}
		
		//validate query
		String queryString = filters;
		if (queryString.isEmpty()) return null;
		
		try(InputStream is = new ByteArrayInputStream(queryString.getBytes())){
			Parser parser = new Parser(is);
			parser.IntelligenceFilter();
		}catch (Exception ex){
			ex.printStackTrace();
			return ex.getMessage();
		}
		return null;
	}
	

	@Override
	public URL getDescription() {
		IPath path = new Path("src/org/wcs/smart/intelligence/query/model/record.html"); //$NON-NLS-1$
		return QueryPlugIn.findHelpURL(path, IntelligenceQueryPlugIn.getDefault().getBundle());
	}

}
