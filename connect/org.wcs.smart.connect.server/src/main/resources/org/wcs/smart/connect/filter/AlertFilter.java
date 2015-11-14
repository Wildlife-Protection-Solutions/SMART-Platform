/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.filter;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.model.Alert;
import org.wcs.smart.connect.model.Alert.AlertStatusEnum;

public class AlertFilter {
	private int levelFilter;
	private UUID typeUuidFilter;
	private AlertStatusEnum statusFilter;
	private UUID caUuidFilter;
	private Date startDateFilter;
	private Date endDateFilter;
	
	private String textSearchFilter;
	
	
	public AlertFilter(int levelFilter, UUID typeUuidFilter, AlertStatusEnum statusFilter, UUID caUuidFilter, Date startDateFilter, Date endDateFilter, String textSearchFilter){
		this.levelFilter = levelFilter;
		this.typeUuidFilter = typeUuidFilter;
		this.statusFilter = statusFilter;
		this.caUuidFilter = caUuidFilter;
		this.startDateFilter = startDateFilter;
		this.endDateFilter = endDateFilter;
		
		this.textSearchFilter = textSearchFilter;
	}
	
	public List<Alert> getAlerts(Session session){
		Criteria c = session.createCriteria(Alert.class);
		if(levelFilter != 0){
			c.add(Restrictions.eq("level", levelFilter));
		}
		if(typeUuidFilter != null){
			c.add(Restrictions.eq("typeUuid", typeUuidFilter));
		}
		if(statusFilter != null){
			c.add(Restrictions.eq("status", statusFilter));
		}
		if(caUuidFilter != null){
			c.add(Restrictions.eq("caUuid", caUuidFilter));
		}
		if(startDateFilter != null){
			c.add(Restrictions.gt("date", startDateFilter));
		}
		if(endDateFilter != null){
			c.add(Restrictions.lt("date", endDateFilter));
		}
		if(textSearchFilter != null){
			c.add(Restrictions.or(Restrictions.ilike("userGeneratedId", textSearchFilter, MatchMode.ANYWHERE), Restrictions.like("description", textSearchFilter, MatchMode.ANYWHERE)) );
		}
		
		return (List<Alert>)c.list();
	}
	
}
