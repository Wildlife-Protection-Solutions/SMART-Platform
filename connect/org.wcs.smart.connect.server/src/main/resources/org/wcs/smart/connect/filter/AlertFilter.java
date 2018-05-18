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

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.core.Response;

import org.hibernate.Session;
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
	
	public List<Alert> getAlerts(Session s, String username){
		
		CriteriaBuilder cb = s.getCriteriaBuilder();
		CriteriaQuery<Alert> c = cb.createQuery(Alert.class);
		Root<Alert> from = c.from(Alert.class);

		List<Alert> emptyList = new ArrayList<Alert>();
		
		List<Predicate> andFilters = new ArrayList<Predicate>();
		//level
		if(levelFilter != null){
			if(levelFilter.size() == 0){
				return emptyList; //if you have no options selected, your result will always be no alerts.
			}
			List<Predicate> orFilters = new ArrayList<Predicate>();
			for(int x=0; x < levelFilter.size();x++){
				orFilters.add(cb.equal(from.get("level"), levelFilter.get(x))); //$NON-NLS-1$
			}
			andFilters.add(cb.or(orFilters.toArray(new Predicate[orFilters.size()])));
		}
		
		//type
		if(typeUuidFilter != null){
			List<Predicate> orFilters = new ArrayList<Predicate>();
			for(int x=0; x < typeUuidFilter.size();x++){
				orFilters.add(cb.equal(from.get("typeUuid"), typeUuidFilter.get(x) )); //$NON-NLS-1$
			}
			orFilters.add(cb.equal(from.get("typeUuid"), AlertType.NULL_TYPE)); //$NON-NLS-1$
			andFilters.add(cb.or(orFilters.toArray(new Predicate[orFilters.size()])));
		}
		
		//status
		if(statusFilter != null){
			if(statusFilter.size() == 0){
				return emptyList; //if you have no options selected, your result will always be no alerts.
			}
			List<Predicate> orFilters = new ArrayList<Predicate>();
			for(int x=0; x < statusFilter.size();x++){
				orFilters.add(cb.equal(from.get("status"), statusFilter.get(x) )); //$NON-NLS-1$
			}
			andFilters.add(cb.or(orFilters.toArray(new Predicate[orFilters.size()])));
		}
	
		
		if(caUuidFilter != null){
			List<Predicate> orFilters = new ArrayList<Predicate>();
			for(int x=0; x < caUuidFilter.size();x++){
				if(SecurityManager.INSTANCE.canAccess(s, username , AlertAction.VIEW_ALERTS_KEY, caUuidFilter.get(x)) ){;
					orFilters.add(cb.equal(from.get("ca").get("uuid"), caUuidFilter.get(x) )); //$NON-NLS-1$ //$NON-NLS-2$

				}
			}
			//add null ca alerts
			if(SecurityManager.INSTANCE.canAccess(s, username , AlertAction.VIEW_ALERTS_KEY, null) ){
				orFilters.add(from.get("ca").get("uuid") .isNull()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			andFilters.add(cb.or(orFilters.toArray(new Predicate[orFilters.size()])));
		}

		if(startDateFilter != null){
			andFilters.add(cb.greaterThan(from.get("date"), startDateFilter)); //$NON-NLS-1$
		}
		if(endDateFilter != null){
			andFilters.add(cb.lessThan(from.get("date"), endDateFilter)); //$NON-NLS-1$
		}
		if(textSearchFilter != null){
			andFilters.add( cb.or(
					cb.like(cb.upper(from.get("userGeneratedId")), "%" + textSearchFilter.toUpperCase() + "%"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					cb.like(cb.upper(from.get("description")), "%" + textSearchFilter.toUpperCase() + "%"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		c.where(cb.and(andFilters.toArray(new Predicate[andFilters.size()])));
		if(sortAscending){
			c.orderBy(cb.asc(from.get(sortBy))); 
//			c.addOrder(Order.asc(sortBy));
		}else{
			c.orderBy(cb.desc(from.get(sortBy))); 
//			c.addOrder(Order.desc(sortBy));
		}
		return s.createQuery(c).list();
	}
	
}
