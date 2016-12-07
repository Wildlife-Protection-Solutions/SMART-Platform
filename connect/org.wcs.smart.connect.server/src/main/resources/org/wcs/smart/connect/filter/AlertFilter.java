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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.Alert;
import org.wcs.smart.connect.model.Alert.AlertStatusEnum;
import org.wcs.smart.connect.model.AlertType;
import org.wcs.smart.connect.security.AlertAction;
import org.wcs.smart.connect.security.SecurityManager;


/*
 * AlertFilter is an object that hold all the filter information for specifying exactly what alerts the user is looking for.
 * 
 * The constructor takes string filters from the HTML form that typically provides them, then converts to proper types for each filter.
 */

public class AlertFilter {
	private ArrayList<Integer> levelFilter;
	private List<UUID> typeUuidFilter;
	private List<AlertStatusEnum> statusFilter;
	private List<UUID> caUuidFilter;
	private Date startDateFilter;
	private Date endDateFilter;
	
	private String textSearchFilter;
	private String sortBy;
	
	private boolean sortAscending; 
	
	public AlertFilter(String levelFilter, String typeUuidFilter, 
			String statusFilter, String caUuidFilter, String startDateFilter, 
			String endDateFilter, String textSearchFilter, String sortBy, 
			Boolean sortAscending, Locale l){
		//level
		if(levelFilter != null){
			this.levelFilter = new ArrayList<Integer>();
			if(!levelFilter.equals("")){ //$NON-NLS-1$
				List<String> items = Arrays.asList(levelFilter.split("\\s*,\\s*")); //$NON-NLS-1$
				for(int x=0; x<items.size(); x++){
					try{
						this.levelFilter.add(Integer.parseInt(items.get(x)) );
					}catch(Exception e){
						throw new SmartConnectException(Response.Status.BAD_REQUEST + Messages.getString("AlertFilter.InvalidLevel",l)); //$NON-NLS-1$
					}
				}
			}
		}
		//type
		if(typeUuidFilter != null){
			this.typeUuidFilter = new ArrayList<UUID>();
			if(!typeUuidFilter.equals("")){ //$NON-NLS-1$
				List<String> items = Arrays.asList(typeUuidFilter.split("\\s*,\\s*")); //$NON-NLS-1$
				for(int x=0; x<items.size(); x++){
					try{
						this.typeUuidFilter.add(UUID.fromString(items.get(x)) );
					}catch(Exception e){
						throw new SmartConnectException(Response.Status.BAD_REQUEST + Messages.getString("AlertFilter.InvalidUuid",l)); //$NON-NLS-1$
					}
				}
			}
		}
		
		//status
		if(statusFilter != null){
			this.statusFilter = new ArrayList<AlertStatusEnum>();
			if(!statusFilter.equals("")){ //$NON-NLS-1$
				List<String> items = Arrays.asList(statusFilter.split("\\s*,\\s*")); //$NON-NLS-1$
				for(int x=0; x<items.size(); x++){
					try{
						this.statusFilter.add(AlertStatusEnum.valueOf(items.get(x)));
					}catch(Exception e){
						throw new SmartConnectException(Response.Status.BAD_REQUEST + Messages.getString("AlertFilter.InvalidStatus",l)); //$NON-NLS-1$
					}
				}
			}
		}
		
		//CA filter
		if(caUuidFilter != null){
			this.caUuidFilter = new ArrayList<UUID>();
			if(!caUuidFilter.equals("")){ //$NON-NLS-1$
				List<String> items = Arrays.asList(caUuidFilter.split("\\s*,\\s*")); //$NON-NLS-1$
				for(int x=0; x<items.size(); x++){
					try{
						this.caUuidFilter.add(UUID.fromString(items.get(x)) );
					}catch(Exception e){
						throw new SmartConnectException(Response.Status.BAD_REQUEST + Messages.getString("AlertFilter.InvalidUuid",l)); //$NON-NLS-1$
					}
				}
			}
		}
		
		
		//date filters
		if(startDateFilter != null && !startDateFilter.isEmpty()){ 
			try {		
				if(endDateFilter == null || endDateFilter.isEmpty()){
					this.endDateFilter = new Date((System.currentTimeMillis() + (1000 * 60 * 60 * 24 * 365))); //Add lots of time to now, in case clocks are off etc, really annoying to not see 'future' alerts when you select "all alerts".
				}else{
					this.endDateFilter = new Date(Long.parseLong(endDateFilter));
				}
				this.startDateFilter = new Date(Long.parseLong(startDateFilter));
			
			} catch (Exception e) {
				throw new SmartConnectException(Response.Status.BAD_REQUEST + Messages.getString("AlertFilter.InvalidDate",l)); //$NON-NLS-1$
			}
		}
		
		
		//Text filter
		this.textSearchFilter = textSearchFilter;
		
		this.sortBy = sortBy;
		this.sortAscending = sortAscending;
	}
	
	@SuppressWarnings("unchecked")
	public List<Alert> getAlerts(Session s, String username){
		Criteria c = s.createCriteria(Alert.class,"alerts");
		c.createAlias("alerts.ca", "ca"); 
		c.setFetchMode("alerts.ca", FetchMode.JOIN);
		
		
		List<Alert> emptyList = new ArrayList<Alert>();
		
		//level
		if(levelFilter != null){
			Disjunction or = Restrictions.disjunction();
			if(levelFilter.size() == 0){
				return emptyList; //if you have no options selected, your result will always be no alerts.
			}
			for(int x=0; x < levelFilter.size();x++){
				or.add(Restrictions.eq("level", levelFilter.get(x))); //$NON-NLS-1$
			}
			c.add(or);
		}
		
		//type
		if(typeUuidFilter != null){
			Disjunction or = Restrictions.disjunction();
			for(int x=0; x < typeUuidFilter.size();x++){
				or.add(Restrictions.eq("typeUuid", typeUuidFilter.get(x) )); //$NON-NLS-1$
			}
			or.add(Restrictions.eq("typeUuid", AlertType.NULL_TYPE)); //$NON-NLS-1$
			c.add(or);
		}
		
		//status
		if(statusFilter != null){
			Disjunction or = Restrictions.disjunction();
			if(statusFilter.size() == 0){
				return emptyList; //if you have no options selected, your result will always be no alerts.
			}
			for(int x=0; x < statusFilter.size();x++){
				or.add(Restrictions.eq("status", statusFilter.get(x) )); //$NON-NLS-1$
			}
			c.add(or);
		}
	
		
		if(caUuidFilter != null){
			Disjunction or = Restrictions.disjunction();
			if(caUuidFilter.size() == 0){
				return emptyList; //if you have no options selected, your result will always be no alerts.
			}
			for(int x=0; x < caUuidFilter.size();x++){
				if(SecurityManager.INSTANCE.canAccess(s, username , AlertAction.VIEW_ALERTS_KEY, caUuidFilter.get(x)) ){;
					or.add(Restrictions.eq("ca.uuid", caUuidFilter.get(x) )); //$NON-NLS-1$
				}
			}
			c.add(or);
		}

		if(startDateFilter != null){
			c.add(Restrictions.gt("date", startDateFilter)); //$NON-NLS-1$
		}
		if(endDateFilter != null){
			c.add(Restrictions.lt("date", endDateFilter)); //$NON-NLS-1$
		}
		if(textSearchFilter != null){
			c.add(Restrictions.or(Restrictions.ilike("userGeneratedId", textSearchFilter, MatchMode.ANYWHERE), Restrictions.ilike("description", textSearchFilter, MatchMode.ANYWHERE)) ); //$NON-NLS-1$ //$NON-NLS-2$
		}

		if(sortAscending){
			c.addOrder(Order.asc(sortBy));
		}else{
			c.addOrder(Order.desc(sortBy));
		}
		return (List<Alert>)c.list();
	}
	
}
