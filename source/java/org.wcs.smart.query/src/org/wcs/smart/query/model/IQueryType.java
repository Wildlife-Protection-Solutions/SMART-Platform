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
package org.wcs.smart.query.model;

import java.net.URL;
import java.util.List;

import javax.annotation.Nonnull;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.query.ui.model.IDropItemFactory;
/**
 * Query type extension point.
 * 
 * @author Emily
 *
 */
public interface IQueryType {

	/**
	 * Query type extension point id.
	 */
	public static final String EXTENSION_ID = "org.wcs.smart.query.queryType"; //$NON-NLS-1$
	
	/**
	 * 
	 * @return the hibernate class that represents the 
	 * query object.
	 * 
	 */
	public Class<? extends Query> getHibernateClass();
	
	/**
	 * The query key must only contain lowercase letters and digits.  No 
	 * special characters or spaces.
	 * 
	 * @return query type unique key
	 */
	public String getKey();
	
	/**
	 * @return the name displayed on the gui
	 */
	public String getGuiName();
	
	/**
	 * 
	 * @return the editor associated
	 * with the give query type/
	 */
	public String getEditorId();
	
	/**
	 * 
	 * @return the icon associated with the image type
	 */
	public Image getImage();
	
	/**
	 * 
	 * @return true if query type supports cross ca queries
	 */
	public boolean supportsCrossCaQueries();
	
	/**
	 * 
	 * @return true if query type supports single ca queries
	 */
	public boolean supportsSingleCaQueries();
	
	/**
	 * 
	 * @return drop item factory for query type
	 */
	public IDropItemFactory getDropItemFactory();
	
	/**
	 * Updates the query definition from the various drop panel components
	 * @param query the query object to update
	 * @param components the drop panel components that define the new query definition
	 */
	public void updateQueryDefinition(Query query, List<IDefinitionPanel> components);
	
	/**
	 * Determines if the provided components create a valid query
	 * definition.
	 * 
	 * @param components the various components that define the query definition
	 * @return null if query is valid otherwise error string
	 */
	public String validateQuery(List<IDefinitionPanel> components);
	
	/**
	 * This url is currently used in the new query wizard.  
	 * 
	 * @return a url link to a html page that contains
	 * a description of the query type; can return null
	 */
	public URL getDescription();
	
	/**
	 * Providers a list of info providers that can be execute
	 * against a query results record. Can return an 
	 * empty array but should never return null.
	 * 
	 * @return array of result providers.  
	 */
	@Nonnull
	public IQueryResultInfoProvider[] getResultProviders();
	
}
