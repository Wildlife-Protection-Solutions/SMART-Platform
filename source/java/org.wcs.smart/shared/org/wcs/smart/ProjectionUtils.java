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
package org.wcs.smart;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;

/**
 * Utility functions associated with observations
 * @author Emily
 *
 */
public enum ProjectionUtils {
	
	INSTANCE;
	
	/**
	 * Creates a projection provider that returns the
	 * observation view projection if defined.  If not defined
	 * then projection provider returns the database default projection.
	 * 
	 * @param s
	 * @return
	 */
	public IProjectionProvider createProjectionProvider(Session session, ConservationArea ca){
		Projection prj = getCurrentViewProjection(session, ca);
		if (prj != null){
			try{
				prj.setParsedCoordinateReferenceSystem(ReprojectUtils.stringToCrs(prj.getDefinition()));
			}catch (Exception ex){
				Logger.getLogger(ProjectionUtils.class.getName()).log(Level.WARNING, ex.getMessage(), ex);
				prj = null;
			}
		}
		if (prj == null){
			prj = new Projection();
			prj.setParsedCoordinateReferenceSystem(GeometryUtils.SMART_CRS);
			prj.setName(GeometryUtils.SMART_CRS.getName().toString());
		}
		return new ProjectionProvider(prj);
	}
	
	/**
	 * Gets the current view projection for the provided Conservation Area
	 * @param s
	 * @param ca
	 * @return
	 */
	public Projection getCurrentViewProjection(Session s, ConservationArea ca) {
		return (Projection)s.createCriteria(Projection.class)
				.add(Restrictions.eq("conservationArea", ca)) //$NON-NLS-1$
			.add(Restrictions.eq("isDefault",true)) //$NON-NLS-1$
			.uniqueResult();
	}
}
