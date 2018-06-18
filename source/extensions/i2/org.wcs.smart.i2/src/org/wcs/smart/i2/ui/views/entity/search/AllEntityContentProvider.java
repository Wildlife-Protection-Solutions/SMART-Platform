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
package org.wcs.smart.i2.ui.views.entity.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityAttributeValue;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.search.AdvancedEntitySearch;
import org.wcs.smart.i2.search.IntelSearchResult;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.UuidUtils;

/**
 * Content provider for the all table entity part of the
 * entity list view.
 * 
 * @author Emily
 *
 */
public class AllEntityContentProvider implements ILazyContentProvider {

	//TODO: dispose of table
	//temporary table for storing all entities for querying and sorting
	private static final String DB_NAME_NAME = "smart.i_entity_view";
	
	
	public static final String COL_ENTITY_TYPE_NAME = "i_entity_type_name";
	
	private TableViewer viewer;
	//set of indexes that have been loaded into the table viewer
	private Set<Integer> loaded = Collections.synchronizedSet(new HashSet<>());
	
	private EntityTableData data;
	
	//sort direction
	private String sortColumn = null;
	private int sortDirection = SWT.UP;
	
	/**
	 * Generates the temporary entity table to support viewing and
	 * sorting of all entity table
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public synchronized EntityTableData generateData() {
	
		ConservationArea ca = SmartDB.getCurrentConservationArea();
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
			
				try {
					session.createNativeQuery("DROP TABLE " + DB_NAME_NAME).executeUpdate();
				}catch (Exception ex) {
					//ignore; table likely doesn't exist
					ex.printStackTrace();
				}
				
				//find all attributes
				List<IntelEntityTypeAttribute> etattributes = session
						.createQuery("FROM IntelEntityTypeAttribute WHERE id.attribute.conservationArea = :ca")
						.setParameter("ca", ca)
						.list();
						
				List<IntelAttribute> eattributes = new ArrayList<>();
					etattributes.forEach( a-> {
						if (!eattributes.contains(a.getAttribute())) eattributes.add(a.getAttribute());
				});
						
				//create a temporary entity table for working with
				StringBuilder sb = new StringBuilder();
				sb.append("CREATE TABLE ");
				sb.append(DB_NAME_NAME);
				sb.append(" ( entity_uuid char(16) for bit data, ");
				sb.append(" entity_type_uuid char(16) for bit data, ");
				sb.append(" i_primary_id varchar(1024), ");
				sb.append(" filter boolean, ");
				sb.append(COL_ENTITY_TYPE_NAME);
				sb.append(" varchar(1024) ");
				sb.append(")");
				session.createNativeQuery(sb.toString()).executeUpdate();
				
								
				//now we need to populate this table
				//entity uuid for this ca
				sb = new StringBuilder();
				sb.append(" INSERT INTO ");
				sb.append( DB_NAME_NAME );
				sb.append(" (entity_uuid, entity_type_uuid, i_primary_id, filter)");
				sb.append(" SELECT e.uuid, t.uuid, a.keyid, true FROM smart.i_entity e join smart.i_entity_type t on e.entity_type_uuid = t.uuid join smart.i_attribute a on t.id_attribute_uuid = a.uuid " );
				sb.append(" WHERE e.ca_uuid = :ca");
				session.createNativeQuery(sb.toString())
					.setParameter("ca", ca.getUuid())
					.executeUpdate();
				sb = new StringBuilder();
				sb.append(" CREATE INDEX i_temp_entity_uuid_idx on " + DB_NAME_NAME + "(entity_uuid)");
				session.createNativeQuery(sb.toString()).executeUpdate();
				
				//entity type names
				sb = new StringBuilder();
				sb.append("SELECT distinct entity_type_uuid FROM " + DB_NAME_NAME);
				List<?> entityTypes = session.createNativeQuery(sb.toString()).list();
				for (Object x : entityTypes) {
					UUID entityTypeUuid = UuidUtils.byteToUUID( (byte[]) x );
					IntelEntityType type = session.get(IntelEntityType.class, entityTypeUuid);
				
					sb = new StringBuilder();
					sb.append(" UPDATE ");
					sb.append(DB_NAME_NAME);
					sb.append(" SET ");
					sb.append(COL_ENTITY_TYPE_NAME);
					sb.append(" = :name WHERE entity_type_uuid = :uuid");
					
					session.createNativeQuery(sb.toString())
						.setParameter("name", type.getName())
						.setParameter("uuid", entityTypeUuid)
						.executeUpdate();
					
				}
				
				Integer count = (Integer) session.createNativeQuery("SELECT count(*) FROM " + DB_NAME_NAME).uniqueResult();
				
				session.getTransaction().commit();
				
				//create results
				EntityTableData data = new EntityTableData();
				data.attributes = eattributes;
				data.tableName = DB_NAME_NAME;
				data.totalCount = count;
				data.currentCount = count;
				
				return data;
			}catch(Exception ex){
				ex.printStackTrace();
				session.getTransaction().rollback();
			}
			return null;
		}
	}

	@Override
	public void updateElement(int index) {
		if (viewer.getElementAt(index) != null || loaded.contains(index)){
			//EG: this is the only way I can get the table to refresh properly
			//if refresh is called 
			viewer.replace(viewer.getElementAt(index) , index);
			
			return;
		}
		if (index < 0 || index >= viewer.getTable().getItemCount()) return;
		
		(new LoadDataJob(index, viewer, data)).schedule();
	}
	
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = (TableViewer) viewer;
		if (newInput == null || !(newInput instanceof EntityTableData)) {
			data = null;
			this.viewer.setItemCount(0);
			return;
		}
		data = (EntityTableData) newInput;
		this.viewer.setItemCount(data.currentCount);
	}
	
	/**
	 * Sets the current sort column
	 * 
	 * @param sortColumn
	 */
	public void setSortColumn(String sortColumn) {
		if (this.sortColumn == sortColumn) {
			if (sortDirection == SWT.UP) {
				sortDirection = SWT.DOWN;
			}else if (sortDirection == SWT.DOWN) {
				sortDirection = SWT.UP;
			}
		}
		this.sortColumn = sortColumn;
		
		
		Job j = new Job("update sort column") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try(Session session = HibernateManager.openSession()){
					session.beginTransaction();
					addDataColumn(sortColumn, session);
					session.getTransaction().commit();
				}
				refreshViewer();				
				return Status.OK_STATUS;
			}
		};
		j.schedule();
	}

	private void refreshViewer() {
		Display.getDefault().syncExec(()->{
			//clear viewer and refresh
			loaded.clear();
			for (int i = 0; i < data.currentCount; i ++) viewer.clear(i);
			viewer.refresh();
		});
	}
	
	/**
	 * Adds a data column to the entity table for sorting
	 * 
	 * @param key the attribute key to be sorted
	 * @param session
	 */
	private void addDataColumn(String key, Session session) {
		if (data == null) return;
		if (data.sortColumns.contains(key)) return;
		if (key.equals(COL_ENTITY_TYPE_NAME)) return;
		data.sortColumns.add(key);
		IntelAttribute sortAttribute  = null;
		for (IntelAttribute a : data.attributes) {
			if (a.getKeyId().equals(key)) {
				sortAttribute = a;
				break;
			}
		}
		if (sortAttribute == null) return;
		
		StringBuilder sb = new StringBuilder();
		sb.append("ALTER TABLE " + DB_NAME_NAME);
		sb.append(" ADD COLUMN ");
		sb.append(sortAttribute.getKeyId());
		switch(sortAttribute.getType()) {
		case BOOLEAN:
		case NUMERIC:
		case POSITION:
			sb.append(" double ");
			break;
		case DATE:
			sb.append(" date ");
			break;
		
		case TEXT:
			sb.append(" varchar(1024)");
			break;
		case LIST:
		case EMPLOYEE:
			sb.append("_uuid char(16) for bit data");
		}
		session.createNativeQuery(sb.toString())
			.executeUpdate();
		
		if (sortAttribute.getType() == AttributeType.LIST || sortAttribute.getType() == AttributeType.EMPLOYEE) {
			sb = new StringBuilder();
			sb.append("ALTER TABLE " + DB_NAME_NAME);
			sb.append(" ADD COLUMN ");
			sb.append(sortAttribute.getKeyId());
			sb.append(" varchar(1024)");
			
			session.createNativeQuery(sb.toString())
				.executeUpdate();
		}
		
		
		if (sortAttribute.getType() == AttributeType.BOOLEAN ||
				sortAttribute.getType() == AttributeType.NUMERIC ||
				sortAttribute.getType() == AttributeType.POSITION) {	//position sorts on x
			sb = new StringBuilder();
			sb.append("UPDATE " + DB_NAME_NAME);
			sb.append(" SET ");
			sb.append(sortAttribute.getKeyId() + " = (SELECT ");
			sb.append(" v.double_value ");
			sb.append(" FROM smart.i_entity_attribute_value v join smart.i_attribute a on v.attribute_uuid = a.uuid ");
			sb.append(" WHERE a.keyid = :attribute and v.entity_uuid = ");
			sb.append( DB_NAME_NAME );
			sb.append(".entity_uuid)");
			
			session.createNativeQuery(sb.toString())
			.setParameter("attribute", sortAttribute.getKeyId())
			.executeUpdate();
		}else if (sortAttribute.getType() == AttributeType.TEXT) {
			sb = new StringBuilder();
			sb.append("UPDATE " + DB_NAME_NAME);
			sb.append(" SET ");
			sb.append(sortAttribute.getKeyId() + " = (SELECT ");
			sb.append(" lower(v.string_value) ");
			sb.append(" FROM smart.i_entity_attribute_value v join smart.i_attribute a on v.attribute_uuid = a.uuid ");
			sb.append(" WHERE a.keyid = :attribute and v.entity_uuid = ");
			sb.append( DB_NAME_NAME );
			sb.append(".entity_uuid)");
			
			session.createNativeQuery(sb.toString())
				.setParameter("attribute", sortAttribute.getKeyId())
				.executeUpdate();
			
		}else if (sortAttribute.getType() == AttributeType.DATE) {
			sb = new StringBuilder();
			sb.append("UPDATE " + DB_NAME_NAME);
			sb.append(" SET ");
			sb.append(sortAttribute.getKeyId() + " = (SELECT ");
			sb.append("cast(v.string_value as date)");
			sb.append(" FROM smart.i_entity_attribute_value v join smart.i_attribute a on v.attribute_uuid = a.uuid ");
			sb.append(" WHERE a.keyid = :attribute and v.entity_uuid = ");
			sb.append( DB_NAME_NAME );
			sb.append(".entity_uuid)");
			
			session.createNativeQuery(sb.toString())
				.setParameter("attribute", sortAttribute.getKeyId())
				.executeUpdate();
			
		}else if (sortAttribute.getType() == AttributeType.LIST) {
			sb = new StringBuilder();
			sb.append("UPDATE " + DB_NAME_NAME);
			sb.append(" SET ");
			sb.append(sortAttribute.getKeyId() + "_uuid = (SELECT ");
			sb.append(" v.list_item_uuid ");
			sb.append(" FROM smart.i_entity_attribute_value v join smart.i_attribute a on v.attribute_uuid = a.uuid ");
			sb.append(" WHERE a.keyid = :attribute and v.entity_uuid = ");
			sb.append( DB_NAME_NAME );
			sb.append(".entity_uuid)");
			
			session.createNativeQuery(sb.toString())
				.setParameter("attribute", sortAttribute.getKeyId())
				.executeUpdate();
			
			IntelAttribute temp = session.get(IntelAttribute.class, sortAttribute.getUuid());
			for (IntelAttributeListItem item : temp.getAttributeList()) {
				sb = new StringBuilder();
				sb.append(" UPDATE ");
				sb.append(DB_NAME_NAME);
				sb.append(" SET " + sortAttribute.getKeyId() + " = :name WHERE " + sortAttribute.getKeyId() + "_uuid = :uuid");
				
				session.createNativeQuery(sb.toString())
					.setParameter("name", item.getName().toLowerCase())
					.setParameter("uuid", item.getUuid())
					.executeUpdate();
			}
			
		}else if (sortAttribute.getType() == AttributeType.EMPLOYEE) {
			sb = new StringBuilder();
			sb.append("UPDATE " + DB_NAME_NAME);
			sb.append(" SET ");
			sb.append(sortAttribute.getKeyId() + "_uuid = (SELECT ");
			sb.append(" v.employee_uuid ");
			sb.append(" FROM smart.i_entity_attribute_value v join smart.i_attribute a on v.attribute_uuid = a.uuid ");
			sb.append(" WHERE a.keyid = :attribute and v.entity_uuid = ");
			sb.append( DB_NAME_NAME );
			sb.append(".entity_uuid)");
			
			session.createNativeQuery(sb.toString())
				.setParameter("attribute", sortAttribute.getKeyId())
				.executeUpdate();
		
			sb = new StringBuilder();
			sb.append("SELECT distinct " + sortAttribute.getKeyId() + "_uuid FROM ");
			sb.append(DB_NAME_NAME);
			sb.append(" WHERE " + sortAttribute.getKeyId() + "_uuid is not null");
			
			List<?> employees = session.createNativeQuery(sb.toString()).list();
			
			for (Object x : employees) {
				UUID uuid = UuidUtils.byteToUUID((byte[])x);
				Employee e = session.get(Employee.class, uuid);
				
				sb = new StringBuilder();
				sb.append(" UPDATE ");
				sb.append(DB_NAME_NAME);
				sb.append(" SET " + sortAttribute.getKeyId() + " = :name WHERE " + sortAttribute.getKeyId() + "_uuid = :uuid");
				
				session.createNativeQuery(sb.toString())
					.setParameter("name", SmartLabelProvider.getShortLabel(e).toLowerCase())
					.setParameter("uuid", uuid)
					.executeUpdate();
			}	
		}
	}
	
	/**
	 * Sets the filter string
	 * @param filterString
	 */
	public void setFilter(String filterString) {
		if (filterString == null || filterString.isEmpty()) {
			Job j = new Job("update filter") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try(Session session = HibernateManager.openSession()){
						session.beginTransaction();
						try {
							StringBuilder sb = new StringBuilder();
							sb.append("UPDATE " + DB_NAME_NAME + " SET filter = true");
							session.createNativeQuery(sb.toString())
							.executeUpdate();
						} catch (Exception e) {
							e.printStackTrace();
						}
						
						session.getTransaction().commit();
					}
					Display.getDefault().syncExec(()->{
						//clear viewer and refresh
						loaded.clear();						
						for (int i = 0; i < data.currentCount; i ++) viewer.clear(i);						
						viewer.setItemCount(data.totalCount);
						data.currentCount = data.totalCount;
						viewer.refresh();
					});
					
					return Status.OK_STATUS;
				}
			};
			j.schedule();
			return;
		}
		
		AdvancedEntitySearch search = new AdvancedEntitySearch(SmartDB.getCurrentConservationArea());
		search.setSearchString(filterString);
		
		Job j = new Job("update filter") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				int newCount = 0;
				try(Session session = HibernateManager.openSession()){
					session.beginTransaction();
					try {
						IntelSearchResult e = search.doSearch(session, Locale.getDefault(), monitor);
						
						List<UUID> items = new ArrayList<>();
						e.getAllResults().forEach(ii->items.add(ii.getEntityUuid()));
						
						StringBuilder sb = new StringBuilder();
						sb.append("UPDATE " + DB_NAME_NAME + " SET filter = false");
						session.createNativeQuery(sb.toString())
						.executeUpdate();
						
						if (!items.isEmpty()) {
							sb = new StringBuilder();
							sb.append("UPDATE " + DB_NAME_NAME + " SET filter = true WHERE entity_uuid in (:uuids)");
							
							session.createNativeQuery(sb.toString())
							.setParameterList("uuids", items)
							.executeUpdate();
						}
						
						sb = new StringBuilder();
						sb.append(" SELECT count(*) FROM "+ DB_NAME_NAME + " WHERE filter = true");
						newCount = (Integer)session.createNativeQuery(sb.toString()).uniqueResult();
						
						
					} catch (Exception e) {
						Intelligence2PlugIn.displayLog(e.getMessage(), e);
					}
					
					session.getTransaction().commit();
				}
				final int icount = newCount;
				Display.getDefault().syncExec(()->{
					//clear viewer and refresh
					loaded.clear();
					for (int i = 0; i < data.currentCount; i ++) viewer.clear(i);
					data.currentCount = icount;
					viewer.setItemCount(icount);
					viewer.refresh();
				});
				
				return Status.OK_STATUS;
			}
		};
		j.schedule();
	}
	/**
	 * Gets the current sort direction
	 * @return
	 */
	public int getSortDirection() {
		return this.sortDirection;
	}
	
	/**
	 * Class to track entity results data
	 * @author Emily
	 *
	 */
	class EntityTableData {
		
		//temporary database table name
		private String tableName;
		//attributes in the table
		private List<IntelAttribute> attributes;
		//total entity count in the table
		private int totalCount = 0;
		//current count being displayed (used with filters)
		private int currentCount = 0;
		//set of attribute keys representing attribute data that has been
		//added to the table when sorting
		private HashSet<String> sortColumns = new HashSet<>();

		/**
		 * The attributes represented in the results data
		 * @return
		 */
		public List<IntelAttribute> getAttributes(){ return attributes; }
		
		
		
	}

	class LoadDataJob extends Job{

		private int startIndex = 0;
		private int pageSize = 100;
		private TableViewer viewer;
		private EntityTableData data;
		
		public LoadDataJob( int startIndex , TableViewer viewer, EntityTableData data) {
			super("loading entity data");
			this.startIndex = startIndex;
			this.viewer = viewer;
			this.data = data;
			synchronized (loaded) {
				for (int i = startIndex; i < startIndex + pageSize; i ++) {
					loaded.add(i);
				}	
			}
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {

			try(Session session = HibernateManager.openSession()){
				StringBuilder sb = new StringBuilder();
				sb.append("SELECT entity_uuid, i_primary_id, ");
				sb.append(COL_ENTITY_TYPE_NAME);
				sb.append(" FROM ");
				sb.append( data.tableName );
				sb.append(" WHERE filter = true ");
				if (sortColumn != null) {
					boolean lower = false;
					boolean date = false;
					if (sortColumn.equals(COL_ENTITY_TYPE_NAME)) {
						lower = true;
					}else {
						for (IntelAttribute a : data.getAttributes()) {
							if (a.getKeyId().equals(sortColumn)) {
								if (a.getType() == AttributeType.TEXT || 
										a.getType() == AttributeType.LIST) {
									lower = true;
									break;
								}else if (a.getType() == AttributeType.DATE) {
									date = true;
									break;
								}
							}
						}
					}
					sb.append(" ORDER BY ");
					if (lower) {
						sb.append(" LOWER(");
						sb.append(sortColumn);
						sb.append(")");
					}else if (date) {
						sb.append(" cast(");
						sb.append(sortColumn);
						sb.append(" as date)");
					}else {
						sb.append(sortColumn);
					}
					sb.append(" ");
					sb.append(getSortDirection() == SWT.UP ? " ASC " : " DESC ");
				}
				
				NativeQuery<?> query = session.createNativeQuery(sb.toString());
				ScrollableResults results = query.scroll();
				if (!results.first()) return Status.OK_STATUS;
				results.scroll(startIndex);

				int cnt = 0;
				while(cnt < pageSize) {
					Object[] data = results.get();
					EntityTableRowItem item = asRowItem(data, session);
					final int index = startIndex + cnt;
					Display.getDefault().syncExec(()->{
						if(viewer.getControl().isDisposed()) return;
						viewer.replace(item, index);
					});
					
					if (!results.next()) break;
					cnt ++;
				}
				
				
			}
			
			return Status.OK_STATUS;
		}
		
		private EntityTableRowItem asRowItem(Object[] rowdata, Session session) {
			//entity_uuid , i_primary_id , i_entity_type_name
			
			UUID entityUuid = UuidUtils.byteToUUID((byte[])rowdata[0]);
			String id = (String)rowdata[1];
			String type = (String)rowdata[2];
			
			EntityTableRowItem item = new EntityTableRowItem(entityUuid, id);
			item.setType(type);
			
			IntelEntity ie = session.get(IntelEntity.class, entityUuid);
			if (ie != null) {
				for (IntelEntityAttributeValue attributeValue : ie.getAttributes()) {
					if (attributeValue.getAttribute().getType() == AttributeType.LIST) {
						if (attributeValue.getAttributeListItem() != null) {
							item.setAttribute(attributeValue.getAttribute().getKeyId(), attributeValue.getAttributeListItem().getName());
						}
					}else if (attributeValue.getAttribute().getType() == AttributeType.EMPLOYEE) {
						item.setAttribute(attributeValue.getAttribute().getKeyId(),SmartLabelProvider.getShortLabel(attributeValue.getEmployee()));
					}else {
						item.setAttribute(attributeValue.getAttribute().getKeyId(), attributeValue.getAttributeValue());
					}
					
				}
			}
			return item;
			
		}
		
	}
}
