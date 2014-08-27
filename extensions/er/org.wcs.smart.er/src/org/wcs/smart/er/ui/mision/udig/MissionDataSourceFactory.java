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
package org.wcs.smart.er.ui.mision.udig;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.hibernate.Session;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.SmartUtils;

/**
 * Smart patrol data source factor.  This is a read only data source.
 * Sources waypoints and tracks from a patrol.
 * 
 * @author Emily
 * @author elitvin
 * @since 3.0.0
 */
public class MissionDataSourceFactory implements DataStoreFactorySpi{

	public static final Param MISSION_UUID = new Param("missionuuid", String.class, "Mission UUID", true);  //$NON-NLS-1$
	  
	/* (non-Javadoc)
	 * @see org.geotools.data.DataAccessFactory#canProcess(java.util.Map)
	 */
	@Override
	public boolean canProcess(Map<String, Serializable> params) {
		if (params.containsKey(MISSION_UUID.key)){
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.DataAccessFactory#getDescription()
	 */
	@Override
	public String getDescription() {
		return "Mission points location data source";
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.DataAccessFactory#getDisplayName()
	 */
	@Override
	public String getDisplayName() {
		return "Mission";
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.DataAccessFactory#getParametersInfo()
	 */
	@Override
	public Param[] getParametersInfo() {
		return new Param[]{MISSION_UUID };
	}

	/**
	 * @see org.geotools.data.DataAccessFactory#isAvailable()
	 */
	@Override
	public boolean isAvailable() {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.geotools.factory.Factory#getImplementationHints()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Map<Key, ?> getImplementationHints() {
		return Collections.EMPTY_MAP;
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.DataStoreFactorySpi#createDataStore(java.util.Map)
	 */
	@Override
	public DataStore createDataStore(Map<String, Serializable> params)
			throws IOException {
		
		Session session = HibernateManager.openSession();
		Mission mission = null;
		try{
			String uuid = (String)params.get(MISSION_UUID.key);
			if (uuid != null){
				mission = (Mission)session.load(Mission.class, SmartUtils.decodeHex(uuid));
				if (mission != null ){
					//load lazy items
					mission.getWaypoints().size();
				}
			}
		}catch (Exception ex){
			throw new IOException(ex);
		}finally{
			session.close(); 
		}
		return new MissionDataSource(mission);
	}

	/* (non-Javadoc)
	 * @see org.geotools.data.DataStoreFactorySpi#createNewDataStore(java.util.Map)
	 */
	@Override
	public DataStore createNewDataStore(Map<String, Serializable> arg0)
			throws IOException {
		throw new UnsupportedOperationException("This is a read-only data source");
	}

}
