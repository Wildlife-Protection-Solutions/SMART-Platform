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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
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

	/**
	 * temporary table for storing all entities for querying and sorting
	 */
	public static final String DB_NAME_NAME = "smart.i_entity_view"; //$NON-NLS-1$
	
	
	public static final String COL_ENTITY_TYPE_NAME = "i_entity_type_name"; //$NON-NLS-1$
	
	private TableViewer viewer;
	//set of indexes that have been loaded into the table viewer
	private Set<Integer> loaded = Collections.synchronizedSet(new HashSet<>());
	
	private EntityTableData data;
	
	//sort direction
	private String sortColumn = null;
	private int sortDirection = SWT.UP;
	
	private List<Listener> dataModified = new ArrayList<>();
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
					session.createNativeQuery("DROP TABLE " + DB_NAME_NAME).executeUpdate(); //$NON-NLS-1$
				}catch (Exception ex) {
					//ignore; table likely doesn't exist
					ex.printStackTrace();
				}
				
				//find all attributes
				List<IntelEntityTypeAttribute> etattributes = session
						.createQuery("FROM IntelEntityTypeAttribute WHERE id.attribute.conservationArea = :ca") //$NON-NLS-1$
						.setParameter("ca", ca) //$NON-NLS-1$
						.list();
						
				List<IntelAttribute> eattributes = new ArrayList<>();
					etattributes.forEach( a-> {
						if (!eattributes.contains(a.getAttribute())) eattributes.add(a.getAttribute());
				});
						
				//create a temporary entity table for working with
				StringBuilder sb = new StringBuilder();
				sb.append("CREATE TABLE "); //$NON-NLS-1$
				sb.append(DB_NAME_NAME);
				sb.append(" ( entity_uuid char(16) for bit data, "); //$NON-NLS-1$
				sb.append(" entity_type_uuid char(16) for bit data, "); //$NON-NLS-1$
				sb.append(" i_primary_id varchar(1024), "); //$NON-NLS-1$
				sb.append(" filter boolean, "); //$NON-NLS-1$
				sb.append(COL_ENTITY_TYPE_NAME);
				sb.append(" varchar(1024) "); //$NON-NLS-1$
				sb.append(")"); //$NON-NLS-1$
				session.createNativeQuery(sb.toString()).executeUpdate();
				
								
				//now we need to populate this table
				//entity uuid for this ca
				sb = new StringBuilder();
				sb.append(" INSERT INTO "); //$NON-NLS-1$
				sb.append( DB_NAME_NAME );
				sb.append(" (entity_uuid, entity_type_uuid, i_primary_id, filter)"); //$NON-NLS-1$
				sb.append(" SELECT e.uuid, t.uuid, a.keyid, true FROM smart.i_entity e join smart.i_entity_type t on e.entity_type_uuid = t.uuid join smart.i_attribute a on t.id_attribute_uuid = a.uuid " ); //$NON-NLS-1$
				sb.append(" WHERE e.ca_uuid = :ca"); //$NON-NLS-1$
				session.createNativeQuery(sb.toString())
					.setParameter("ca", ca.getUuid()) //$NON-NLS-1$
					.executeUpdate();
				sb = new StringBuilder();
				sb.append(" CREATE INDEX i_temp_entity_uuid_idx on " + DB_NAME_NAME + "(entity_uuid)"); //$NON-NLS-1$ //$NON-NLS-2$
				session.createNativeQuery(sb.toString()).executeUpdate();
				
				//entity type names
				sb = new StringBuilder();
				sb.append("SELECT distinct entity_type_uuid FROM " + DB_NAME_NAME); //$NON-NLS-1$
				List<?> entityTypes = session.createNativeQuery(sb.toString()).list();
				for (Object x : entityTypes) {
					UUID entityTypeUuid = UuidUtils.byteToUUID( (byte[]) x );
					IntelEntityType type = session.get(IntelEntityType.class, entityTypeUuid);
				
					sb = new StringBuilder();
					sb.append(" UPDATE "); //$NON-NLS-1$
					sb.append(DB_NAME_NAME);
					sb.append(" SET "); //$NON-NLS-1$
					sb.append(COL_ENTITY_TYPE_NAME);
					sb.append(" = :name WHERE entity_type_uuid = :uuid"); //$NON-NLS-1$
					
					session.createNativeQuery(sb.toString())
						.setParameter("name", type.getName()) //$NON-NLS-1$
						.setParameter("uuid", entityTypeUuid) //$NON-NLS-1$
						.executeUpdate();
					
				}
				
				Integer count = (Integer) session.createNativeQuery("SELECT count(*) FROM " + DB_NAME_NAME).uniqueResult(); //$NON-NLS-1$
				
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
	
	public void addListener(Listener dataModified) {
		this.dataModified.add(dataModified);
	}
	public void removeListener(Listener dataModified) {
		this.dataModified.remove(dataModified);
	}

	private void fireEvents() {
		Event evt = new Event();
		evt.data = data;
		
		for (Listener l : dataModified)l.handleEvent(evt);
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
			fireEvents();
			return;
		}
		loaded.clear();
		data = (EntityTableData) newInput;
		this.viewer.setItemCount(data.currentCount);
		fireEvents();
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
		
		
		Job j = new Job("update sort column") { //$NON-NLS-1$

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
		sb.append("ALTER TABLE " + DB_NAME_NAME); //$NON-NLS-1$
		sb.append(" ADD COLUMN "); //$NON-NLS-1$
		sb.append(sortAttribute.getKeyId());
		switch(sortAttribute.getType()) {
		case BOOLEAN:
		case NUMERIC:
		case POSITION:
			sb.append(" double "); //$NON-NLS-1$
			break;
		case DATE:
			sb.append(" date "); //$NON-NLS-1$
			break;
		
		case TEXT:
			sb.append(" varchar(1024)"); //$NON-NLS-1$
			break;
		case LIST:
		case EMPLOYEE:
			sb.append("_uuid char(16) for bit data"); //$NON-NLS-1$
		}
		session.createNativeQuery(sb.toString())
			.executeUpdate();
		
		if (sortAttribute.getType() == AttributeType.LIST || sortAttribute.getType() == AttributeType.EMPLOYEE) {
			sb = new StringBuilder();
			sb.append("ALTER TABLE " + DB_NAME_NAME); //$NON-NLS-1$
			sb.append(" ADD COLUMN "); //$NON-NLS-1$
			sb.append(sortAttribute.getKeyId());
			sb.append(" varchar(1024)"); //$NON-NLS-1$
			
			session.createNativeQuery(sb.toString())
				.executeUpdate();
		}
		
		
		if (sortAttribute.getType() == AttributeType.BOOLEAN ||
				sortAttribute.getType() == AttributeType.NUMERIC ||
				sortAttribute.getType() == AttributeType.POSITION) {	//position sorts on x
			sb = new StringBuilder();
			sb.append("UPDATE " + DB_NAME_NAME); //$NON-NLS-1$
			sb.append(" SET "); //$NON-NLS-1$
			sb.append(sortAttribute.getKeyId() + " = (SELECT "); //$NON-NLS-1$
			sb.append(" v.double_value "); //$NON-NLS-1$
			sb.append(" FROM smart.i_entity_attribute_value v join smart.i_attribute a on v.attribute_uuid = a.uuid "); //$NON-NLS-1$
			sb.append(" WHERE a.keyid = :attribute and v.entity_uuid = "); //$NON-NLS-1$
			sb.append( DB_NAME_NAME );
			sb.append(".entity_uuid)"); //$NON-NLS-1$
			
			session.createNativeQuery(sb.toString())
			.setParameter("attribute", sortAttribute.getKeyId()) //$NON-NLS-1$
			.executeUpdate();
		}else if (sortAttribute.getType() == AttributeType.TEXT) {
			sb = new StringBuilder();
			sb.append("UPDATE " + DB_NAME_NAME); //$NON-NLS-1$
			sb.append(" SET "); //$NON-NLS-1$
			sb.append(sortAttribute.getKeyId() + " = (SELECT "); //$NON-NLS-1$
			sb.append(" lower(v.string_value) "); //$NON-NLS-1$
			sb.append(" FROM smart.i_entity_attribute_value v join smart.i_attribute a on v.attribute_uuid = a.uuid "); //$NON-NLS-1$
			sb.append(" WHERE a.keyid = :attribute and v.entity_uuid = "); //$NON-NLS-1$
			sb.append( DB_NAME_NAME );
			sb.append(".entity_uuid)"); //$NON-NLS-1$
			
			session.createNativeQuery(sb.toString())
				.setParameter("attribute", sortAttribute.getKeyId()) //$NON-NLS-1$
				.executeUpdate();
			
		}else if (sortAttribute.getType() == AttributeType.DATE) {
			sb = new StringBuilder();
			sb.append("UPDATE " + DB_NAME_NAME); //$NON-NLS-1$
			sb.append(" SET "); //$NON-NLS-1$
			sb.append(sortAttribute.getKeyId() + " = (SELECT "); //$NON-NLS-1$
			sb.append("cast(v.string_value as date)"); //$NON-NLS-1$
			sb.append(" FROM smart.i_entity_attribute_value v join smart.i_attribute a on v.attribute_uuid = a.uuid "); //$NON-NLS-1$
			sb.append(" WHERE a.keyid = :attribute and v.entity_uuid = "); //$NON-NLS-1$
			sb.append( DB_NAME_NAME );
			sb.append(".entity_uuid)"); //$NON-NLS-1$
			
			session.createNativeQuery(sb.toString())
				.setParameter("attribute", sortAttribute.getKeyId()) //$NON-NLS-1$
				.executeUpdate();
			
		}else if (sortAttribute.getType() == AttributeType.LIST) {
			sb = new StringBuilder();
			sb.append("UPDATE " + DB_NAME_NAME); //$NON-NLS-1$
			sb.append(" SET "); //$NON-NLS-1$
			sb.append(sortAttribute.getKeyId() + "_uuid = (SELECT "); //$NON-NLS-1$
			sb.append(" v.list_item_uuid "); //$NON-NLS-1$
			sb.append(" FROM smart.i_entity_attribute_value v join smart.i_attribute a on v.attribute_uuid = a.uuid "); //$NON-NLS-1$
			sb.append(" WHERE a.keyid = :attribute and v.entity_uuid = "); //$NON-NLS-1$
			sb.append( DB_NAME_NAME );
			sb.append(".entity_uuid)"); //$NON-NLS-1$
			
			session.createNativeQuery(sb.toString())
				.setParameter("attribute", sortAttribute.getKeyId()) //$NON-NLS-1$
				.executeUpdate();
			
			IntelAttribute temp = session.get(IntelAttribute.class, sortAttribute.getUuid());
			for (IntelAttributeListItem item : temp.getAttributeList()) {
				sb = new StringBuilder();
				sb.append(" UPDATE "); //$NON-NLS-1$
				sb.append(DB_NAME_NAME);
				sb.append(" SET " + sortAttribute.getKeyId() + " = :name WHERE " + sortAttribute.getKeyId() + "_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				session.createNativeQuery(sb.toString())
					.setParameter("name", item.getName().toLowerCase()) //$NON-NLS-1$
					.setParameter("uuid", item.getUuid()) //$NON-NLS-1$
					.executeUpdate();
			}
			
		}else if (sortAttribute.getType() == AttributeType.EMPLOYEE) {
			sb = new StringBuilder();
			sb.append("UPDATE " + DB_NAME_NAME); //$NON-NLS-1$
			sb.append(" SET "); //$NON-NLS-1$
			sb.append(sortAttribute.getKeyId() + "_uuid = (SELECT "); //$NON-NLS-1$
			sb.append(" v.employee_uuid "); //$NON-NLS-1$
			sb.append(" FROM smart.i_entity_attribute_value v join smart.i_attribute a on v.attribute_uuid = a.uuid "); //$NON-NLS-1$
			sb.append(" WHERE a.keyid = :attribute and v.entity_uuid = "); //$NON-NLS-1$
			sb.append( DB_NAME_NAME );
			sb.append(".entity_uuid)"); //$NON-NLS-1$
			
			session.createNativeQuery(sb.toString())
				.setParameter("attribute", sortAttribute.getKeyId()) //$NON-NLS-1$
				.executeUpdate();
		
			sb = new StringBuilder();
			sb.append("SELECT distinct " + sortAttribute.getKeyId() + "_uuid FROM "); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(DB_NAME_NAME);
			sb.append(" WHERE " + sortAttribute.getKeyId() + "_uuid is not null"); //$NON-NLS-1$ //$NON-NLS-2$
			
			List<?> employees = session.createNativeQuery(sb.toString()).list();
			
			for (Object x : employees) {
				UUID uuid = UuidUtils.byteToUUID((byte[])x);
				Employee e = session.get(Employee.class, uuid);
				
				sb = new StringBuilder();
				sb.append(" UPDATE "); //$NON-NLS-1$
				sb.append(DB_NAME_NAME);
				sb.append(" SET " + sortAttribute.getKeyId() + " = :name WHERE " + sortAttribute.getKeyId() + "_uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				session.createNativeQuery(sb.toString())
					.setParameter("name", SmartLabelProvider.getShortLabel(e).toLowerCase()) //$NON-NLS-1$
					.setParameter("uuid", uuid) //$NON-NLS-1$
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
			Job j = new Job(Messages.AllEntityContentProvider_UpdateFilterJobName) {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					try(Session session = HibernateManager.openSession()){
						session.beginTransaction();
						try {
							StringBuilder sb = new StringBuilder();
							sb.append("UPDATE " + DB_NAME_NAME + " SET filter = true"); //$NON-NLS-1$ //$NON-NLS-2$
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
						fireEvents();
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
		
		Job j = new Job(Messages.AllEntityContentProvider_UpdateFilterJobName) {

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
						sb.append("UPDATE " + DB_NAME_NAME + " SET filter = false"); //$NON-NLS-1$ //$NON-NLS-2$
						session.createNativeQuery(sb.toString())
						.executeUpdate();
						
						if (!items.isEmpty()) {
							sb = new StringBuilder();
							sb.append("UPDATE " + DB_NAME_NAME + " SET filter = true WHERE entity_uuid in (:uuids)"); //$NON-NLS-1$ //$NON-NLS-2$
							
							session.createNativeQuery(sb.toString())
							.setParameterList("uuids", items) //$NON-NLS-1$
							.executeUpdate();
						}
						
						sb = new StringBuilder();
						sb.append(" SELECT count(*) FROM "+ DB_NAME_NAME + " WHERE filter = true"); //$NON-NLS-1$ //$NON-NLS-2$
						newCount = (Integer)session.createNativeQuery(sb.toString()).uniqueResult();
						
						session.getTransaction().commit();	
					} catch (Exception e) {
						Intelligence2PlugIn.displayLog(e.getMessage(), e);
						session.getTransaction().rollback();
					}
					
					
				}
				final int icount = newCount;
				Display.getDefault().syncExec(()->{
					//clear viewer and refresh
					loaded.clear();
					for (int i = 0; i < data.currentCount; i ++) viewer.clear(i);
					data.currentCount = icount;
					viewer.setItemCount(icount);
					fireEvents();
					viewer.refresh();
				});
				
				return Status.OK_STATUS;
			}
		};
		j.schedule();
	}
	
	/**
	 * 
	 * @return uuids of all entities that match the current filter
	 */
	public List<UUID> getAllDataItems(){
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT entity_uuid FROM "); //$NON-NLS-1$
		sb.append(DB_NAME_NAME);
		sb.append(" WHERE filter = true "); //$NON-NLS-1$
		
		List<UUID> allUuids = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			List<?> items = session.createNativeQuery(sb.toString()).list();
			for (Object x : items) {
				allUuids.add(  UuidUtils.byteToUUID((byte[])x));
			}
		}
		return allUuids;
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
		public int getTotalCount() { return this.totalCount; }
		public int getDisplayCount() { return this.currentCount; }
	}

	class LoadDataJob extends Job{

		private int startIndex = 0;
		private int pageSize = 100;
		private TableViewer viewer;
		private EntityTableData data;
		
		public LoadDataJob( int startIndex , TableViewer viewer, EntityTableData data) {
			super(Messages.AllEntityContentProvider_LoadingDataJobName);
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
				sb.append("SELECT entity_uuid, i_primary_id, "); //$NON-NLS-1$
				sb.append(COL_ENTITY_TYPE_NAME);
				sb.append(" FROM "); //$NON-NLS-1$
				sb.append( data.tableName );
				sb.append(" WHERE filter = true "); //$NON-NLS-1$
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
					sb.append(" ORDER BY "); //$NON-NLS-1$
					if (lower) {
						sb.append(" LOWER("); //$NON-NLS-1$
						sb.append(sortColumn);
						sb.append(")"); //$NON-NLS-1$
					}else if (date) {
						sb.append(" cast("); //$NON-NLS-1$
						sb.append(sortColumn);
						sb.append(" as date)"); //$NON-NLS-1$
					}else {
						sb.append(sortColumn);
					}
					sb.append(" "); //$NON-NLS-1$
					sb.append(getSortDirection() == SWT.UP ? " ASC " : " DESC "); //$NON-NLS-1$ //$NON-NLS-2$
				}
				
				NativeQuery<?> query = session.createNativeQuery(sb.toString());
				ScrollableResults results = query.scroll();
				if (!results.first()) return Status.OK_STATUS;
				results.scroll(startIndex);

				int cnt = 0;
				while(cnt < pageSize) {
					Object[] data = results.get();
					if (data == null) break;
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
						if (attributeValue.getEmployee() != null) {
							item.setAttribute(attributeValue.getAttribute().getKeyId(),SmartLabelProvider.getShortLabel(attributeValue.getEmployee()));
						}
					}else {
						item.setAttribute(attributeValue.getAttribute().getKeyId(), attributeValue.getAttributeValue());
					}
					
				}
			}
			return item;
			
		}
		
	}
}
