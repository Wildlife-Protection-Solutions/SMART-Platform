package org.wcs.smart.connect.model;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
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
