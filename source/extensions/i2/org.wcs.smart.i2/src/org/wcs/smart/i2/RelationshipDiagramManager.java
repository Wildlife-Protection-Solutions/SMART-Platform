/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.RelationshipDiagramStyle;

/**
 * Functions for managing relationship diagram related items.
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
public enum RelationshipDiagramManager {

	INSTANCE;
	
	private RelationshipDiagramManager() {}

	/**
	 * Fetches a list of {@link RelationshipDiagramStyle} for current conservation area
	 * 
	 * @param session session
	 * @return List of {@link RelationshipDiagramStyle}
	 */
	public List<RelationshipDiagramStyle> getStyles(Session session) {
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		List<RelationshipDiagramStyle>  styles =
				QueryFactory.buildQuery(session, RelationshipDiagramStyle.class,"conservationArea", ca).getResultList(); //$NON-NLS-1$
		//TODO: ZZZZZZZZZZZ need default style
//		if (profiles.isEmpty()) {
//			RelationshipDiagramStyle defaultProfile = createDefaultStyle(session);
//			return Arrays.asList(defaultProfile);
//		}
		return styles;
	}

	/**
	 * Delete a {@link RelationshipDiagramStyle}
	 */
	public void deleteStyle(Session session, RelationshipDiagramStyle style) {
		session.delete(style);
	}
	
}
