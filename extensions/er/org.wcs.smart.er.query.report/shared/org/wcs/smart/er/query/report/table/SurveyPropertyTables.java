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
package org.wcs.smart.er.query.report.table;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.data.oda.smart.impl.table.IDynamicSmartTables;
import org.wcs.smart.data.oda.smart.impl.table.SmartBirtTable;
import org.wcs.smart.er.hibernate.CaSurveyHibernateManager;
import org.wcs.smart.er.hibernate.CcaaSurveyHibernateManager;
import org.wcs.smart.er.hibernate.ISurveyHibernateManager;
import org.wcs.smart.er.hibernate.SurveyDesignProxy;
import org.wcs.smart.er.model.SurveyDesign;

/**
 * SMART Report Tables Extension that provides access to
 * survey design properties. 
 * 
 * @author Emily
 *
 */
public class SurveyPropertyTables implements IDynamicSmartTables {

	public SurveyPropertyTables() {
	}

	@Override
	public List<SmartBirtTable> getTables(SmartConnection connection) {
		//find all entities
		List<SmartBirtTable> tables = new ArrayList<SmartBirtTable>();
		//do not close session; this will be done by the wizard 
		//TODO: once ccaa implemented for surveys this will need to be updated
		
		ISurveyHibernateManager mgr = null;
		if (connection.getConservationAreas().size() == 1){
			mgr = new CaSurveyHibernateManager(connection.getConservationAreas().iterator().next());
		}else{
			mgr = new CcaaSurveyHibernateManager();
		}
		Session s = connection.getSession();
		for (SurveyDesignProxy sdi : mgr.getSurveyDesignEditorInputs(s, null)){
			SurveyDesign sd = (SurveyDesign)s.load(SurveyDesign.class, sdi.getUuid());
			SurveyDesignPropertyTable table = new SurveyDesignPropertyTable(sd);
			tables.add(table);
		}
		
		return tables;
	}

}
