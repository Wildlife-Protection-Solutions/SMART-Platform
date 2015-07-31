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
package org.wcs.smart.entity.query;

import java.net.URL;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.query.ui.model.IDropItemFactory;

/**
 * A Entity Sighting Query Type.  This is not
 * defined as a query type extension as we don't want
 * users creating new ones.  This is done simply so we are
 * reuse some of the query interfaces for the
 * EntitySightingQuery.
 * 
 * @author Emily
 *
 */
public class EntitySightingQueryType implements IQueryType {

	public static final EntitySightingQueryType INSTANCE = new EntitySightingQueryType();
	public static final String KEY = "ENTITY_SIGHTING"; //$NON-NLS-1$
	/**
	 * @return null this query type is not persisted to the database
	 */
	@Override
	public Class<? extends Query> getHibernateClass() {
		return null;
	}

	@Override
	public String getKey() {
		return KEY; 
	}

	@Override
	public String getGuiName() {
		return Messages.EntitySightingQueryType_QueryTypeName;
	}

	/**
	 * Not associated with query.
	 * 
	 * @return null  
	 */
	@Override
	public String getEditorId() {
		return null;
	}

	@Override
	public Image getImage() {
		return EntityPlugIn.getDefault().getImageRegistry().get(EntityPlugIn.SIGHTINGS_ICON);
	}

	@Override
	public boolean supportsCrossCaQueries() {
		return false;
	}

	@Override
	public boolean supportsSingleCaQueries() {
		return true;
	}

	/**
	 * Not supported.
	 * @return null
	 */
	@Override
	public IDropItemFactory getDropItemFactory() {
		return null;
	}


	/**
	 * Not supported.
	 * @return null
	 */
	@Override
	public void updateQueryDefinition(Query query,
			List<IDefinitionPanel> components) {
	}


	/**
	 * Not supported.
	 * @return null
	 */
	@Override
	public String validateQuery(List<IDefinitionPanel> components) {
		return null;
	}

	public URL getDescription() {
		return null;
	}
}
