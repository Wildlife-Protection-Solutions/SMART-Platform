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
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.model.Alert.AlertStatusEnum;

public class AlertFilter {
	private ArrayList<Integer> levelFilter;
	private List<UUID> typeUuidFilter;
	private List<AlertStatusEnum> statusFilter;
	private List<UUID> caUuidFilter;
	private Date startDateFilter;
	private Date endDateFilter;
	
	private String textSearchFilter;
	
	
	public AlertFilter(String levelFilter, String typeUuidFilter, String statusFilter, String caUuidFilter, Date startDateFilter, Date endDateFilter, String textSearchFilter){
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

		this.startDateFilter = startDateFilter;
		this.endDateFilter = endDateFilter;
		
		this.textSearchFilter = textSearchFilter;
	}
	
	public List<Alert> getAlerts(Session session){
		Criteria c = session.createCriteria(Alert.class);
		
		//level
		if(levelFilter != null){
			Disjunction or = Restrictions.disjunction();
			if(levelFilter.size() == 0){
				or.add(Restrictions.eq("level", -99999));
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
				or.add(Restrictions.eq("typeUuid", UUID.fromString("99999999-9999-9999-9999-999999999999")) );
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
				or.add(Restrictions.eq("status", "showNoRows"));
			}
			for(int x=0; x < statusFilter.size();x++){
				or.add(Restrictions.eq("status", statusFilter.get(x) ));
			}
			c.add(or);
		}
	
		
		if(caUuidFilter != null){
			Disjunction or = Restrictions.disjunction();
			if(caUuidFilter.size() == 0){
				or.add(Restrictions.eq("caUuid", UUID.fromString("99999999-9999-9999-9999-999999999999")) );
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
		
		return (List<Alert>)c.list();
	}
	
}
