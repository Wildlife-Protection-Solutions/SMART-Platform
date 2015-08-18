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
package org.wcs.smart.er.model;

import java.io.File;
import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.wcs.smart.SmartContext;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.util.UuidUtils;

/**
 * Survey waypoint source.
 * 
 * @author Emily
 *
 */
public class SurveyWaypointSource implements IWaypointSource{

	public static final String KEY = "SURVEY"; //$NON-NLS-1$
	
	@Override
	public String getKey() {
		return KEY;
	}

	@Override
	public String getName(Locale l) {
		return SmartContext.INSTANCE.getClass(IErLabelProvider.class).getLabel(this, l);
	}

	@Override
	public String getDatastoreFileLocation(Object object, Session session) throws Exception {
		if (object instanceof Mission){
			return getDatastoreFileLocation((Mission)object);
		}
		if (!(object instanceof Waypoint)){
			throw new Exception(MessageFormat.format("Object type {0} not supported for survey waypoint source attachments", object.getClass().getName())); //$NON-NLS-1$
		}
		final Waypoint wp = (Waypoint)object;
		if (wp.getUuid() == null){
			return null;
		}
		//need to determine the patrol this waypoint is associated
		//with; do in a different thread so we can have our own database
		//connection
		String missionDir = null;
		StatelessSession temp = session.getSessionFactory().openStatelessSession();
		try{
			Query q = temp.createQuery("SELECT m.uuid from Mission m join m.missionDays md join md.waypoints wp where wp.id.waypoint = :wp "); //$NON-NLS-1$
			q.setParameter("wp", wp); //$NON-NLS-1$
	
			List<?> pws = q.list();
		
			if (pws.size() > 0){
				UUID uuid = (UUID) pws.get(0);
				missionDir = UuidUtils.getDirectoryPath(uuid);
			}else{
				throw new Exception("Could not determine attached location for survey waypoint attachment. " + wp.getUuid().toString()); //$NON-NLS-1$
			}
		}finally{
			temp.close();
		}

		StringBuilder sb = new StringBuilder();
		sb.append(SurveyDesign.SURVEY_FILESTORE_LOC);
		sb.append(File.separator);
		if (missionDir != null){
			sb.append(missionDir);
			sb.append(File.separator);
		}
		
		return sb.toString();
	}
	
	public String getDatastoreFileLocation(Mission m){
		StringBuilder sb = new StringBuilder();
		sb.append(SurveyDesign.SURVEY_FILESTORE_LOC);
		sb.append(File.separator);
		if (m != null){
			sb.append(UuidUtils.uuidToString(m.getUuid()));
			sb.append(File.separator);
		}
		
		return sb.toString();
	}

}
