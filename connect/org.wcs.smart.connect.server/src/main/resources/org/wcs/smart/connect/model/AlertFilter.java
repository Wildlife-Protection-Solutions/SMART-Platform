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
package org.wcs.smart.connect.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.model.Alert.AlertStatusEnum;


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
	
	
	public AlertFilter(String levelFilter, String typeUuidFilter, String statusFilter, String caUuidFilter, String startDateFilter, String endDateFilter, String textSearchFilter, String sortBy, Boolean sortAscending){
		//level
		if(levelFilter != null){
			this.levelFilter = new ArrayList<Integer>();
			if(!levelFilter.equals("")){
				List<String> items = Arrays.asList(levelFilter.split("\\s*,\\s*"));
				for(int x=0; x<items.size(); x++){
					try{
						this.levelFilter.add(Integer.parseInt(items.get(x)) );
					}catch(Exception e){
						throw new SmartConnectException(Response.Status.BAD_REQUEST + "; Invalid level/importance filter");
					}
				}
			}
		}
		//type
		if(typeUuidFilter != null){
			this.typeUuidFilter = new ArrayList<UUID>();
			if(!typeUuidFilter.equals("")){
				List<String> items = Arrays.asList(typeUuidFilter.split("\\s*,\\s*"));
				for(int x=0; x<items.size(); x++){
					try{
						this.typeUuidFilter.add(UUID.fromString(items.get(x)) );
					}catch(Exception e){
						throw new SmartConnectException(Response.Status.BAD_REQUEST + "; Invalid UUID format in filter");
					}
				}
			}
		}
		
		//status
		if(statusFilter != null){
			this.statusFilter = new ArrayList<AlertStatusEnum>();
			if(!statusFilter.equals("")){
				List<String> items = Arrays.asList(statusFilter.split("\\s*,\\s*"));
				for(int x=0; x<items.size(); x++){
					try{
						if(items.get(x).compareTo(AlertStatusEnum.ACTIVE.getValue()) == 0){
							this.statusFilter.add(AlertStatusEnum.ACTIVE);
						}else if(items.get(x).compareTo(AlertStatusEnum.DISABLED.getValue()) == 0){
							this.statusFilter.add(AlertStatusEnum.DISABLED);
						}
					}catch(Exception e){
						throw new SmartConnectException(Response.Status.BAD_REQUEST + "; Invalid status filter value");
					}
				}
			}
		}
		
		//CA filter
		if(caUuidFilter != null){
			this.caUuidFilter = new ArrayList<UUID>();
			if(!caUuidFilter.equals("")){
				List<String> items = Arrays.asList(caUuidFilter.split("\\s*,\\s*"));
				for(int x=0; x<items.size(); x++){
					try{
						this.caUuidFilter.add(UUID.fromString(items.get(x)) );
					}catch(Exception e){
						throw new SmartConnectException(Response.Status.BAD_REQUEST + "; Invalid UUID format in filter");
					}
				}
			}
		}
		
		//date filters
		if(startDateFilter != null && !startDateFilter.equals("")			
			&& endDateFilter != null && !endDateFilter.equals("") ){
			try {		
				this.startDateFilter = new Date(Long.parseLong(startDateFilter));
				this.endDateFilter = new Date(Long.parseLong(endDateFilter));
			} catch (Exception e) {
				throw new SmartConnectException(Response.Status.BAD_REQUEST + "; Invalid Date format in filters, should be a valid unix-timestamp");
			}
		}
		
		
		//Text filter
		this.textSearchFilter = textSearchFilter;
		
		this.sortBy = sortBy;
		this.sortAscending = sortAscending;
	}
	
	public List<Alert> getAlerts(Session session){
		Criteria c = session.createCriteria(Alert.class);
		List<Alert> emptyList = new ArrayList<Alert>();
		
		
		//level
		if(levelFilter != null){
			Disjunction or = Restrictions.disjunction();
			if(levelFilter.size() == 0){
				return emptyList; //if you have no options selected, your result will always be no alerts.
			}
			for(int x=0; x < levelFilter.size();x++){
				or.add(Restrictions.eq("level", levelFilter.get(x)));
			}
			c.add(or);
			
		}
		
		//type
		if(typeUuidFilter != null){
			Disjunction or = Restrictions.disjunction();
			if(typeUuidFilter.size() == 0){
				return emptyList; //if you have no options selected, your result will always be no alerts.				

			}
			for(int x=0; x < typeUuidFilter.size();x++){
				or.add(Restrictions.eq("typeUuid", typeUuidFilter.get(x) ));
			}
			c.add(or);
		}
		//status
		if(statusFilter != null){
			Disjunction or = Restrictions.disjunction();
			if(statusFilter.size() == 0){
				return emptyList; //if you have no options selected, your result will always be no alerts.
			}
			for(int x=0; x < statusFilter.size();x++){
				or.add(Restrictions.eq("status", statusFilter.get(x) ));
			}
			c.add(or);
		}
	
		
		if(caUuidFilter != null){
			Disjunction or = Restrictions.disjunction();
			if(caUuidFilter.size() == 0){
				return emptyList; //if you have no options selected, your result will always be no alerts.
			}
			for(int x=0; x < caUuidFilter.size();x++){
				or.add(Restrictions.eq("caUuid", caUuidFilter.get(x) ));
			}
			c.add(or);
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

		if(sortAscending){
			c.addOrder(Order.asc(sortBy));
		}else{
			c.addOrder(Order.desc(sortBy));
		}
		return (List<Alert>)c.list();
	}
	
}
