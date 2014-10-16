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
package org.wcs.smart.er.query.importexport;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.er.model.Mission;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.Survey;
import org.wcs.smart.er.query.filter.MissionFilter;
import org.wcs.smart.er.query.filter.MissionMemberFilter;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.filter.SurveyFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.xml.model.QueryType;
import org.wcs.smart.query.xml.model.UuidItemType;
import org.wcs.smart.util.SmartUtils;

/**
 * Filter visitor for processing patrol filters.
 * 
 * @author Emily
 *
 */
public class SurveyFilterProcessorVisitor implements IFilterVisitor {
    
	private Session session;
	private Exception ex;
	private QueryType qt;
	
	public SurveyFilterProcessorVisitor(Session session, QueryType qt){
		this.session = session;
		this.qt = qt;
	}
	
	@Override
	public void visit(IFilter filter) {
		try{
			if (filter instanceof MissionMemberFilter){
				MissionMemberFilter pf = (MissionMemberFilter)filter;
		        qt.getUuiditem().add(employeeToUuidItem(pf.getUuid()));
			}else if (filter instanceof SamplingUnitFilter){
				SamplingUnitFilter sf = (SamplingUnitFilter)filter;
				qt.getUuiditem().add(samplingUnitToUuidItem(SmartUtils.decodeHex(sf.getUuid())));
			}
		}catch (Exception ex){
			this.ex = ex;
		}
	}
	
	public Exception getException(){
		return this.ex;
	}
	
	public UuidItemType employeeToUuidItem(byte[] uuid){
		Employee e = (Employee) session.load(Employee.class, uuid);
		
		UuidItemType item = new UuidItemType();
        item.setUuid(SmartUtils.encodeHex(uuid));
        item.setId(e.getId());
        item.getValue().add(e.getGivenName());
        item.getValue().add(e.getFamilyName());
        return item;
	}
	
	public UuidItemType samplingUnitToUuidItem(byte[] uuid){
		SamplingUnit su = (SamplingUnit)session.load(SamplingUnit.class, uuid);
		
		UuidItemType item = new UuidItemType();
		item.setUuid(SmartUtils.encodeHex(uuid));
		item.setId(su.getId());
        return item;
	}
	
}
