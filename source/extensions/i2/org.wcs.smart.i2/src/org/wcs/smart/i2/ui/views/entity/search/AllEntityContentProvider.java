package org.wcs.smart.i2.ui.views.entity.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
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
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttribute.AttributeType;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.util.UuidUtils;

public class AllEntityContentProvider implements ILazyContentProvider {

	//TODO: dispose of table
	private static final String DB_NAME_NAME = "smart.i_entity_view";
	
	public static final String COL_ENTITY_TYPE_NAME = "i_entity_type_name";
	
	private TableViewer viewer;
	private Set<Integer> loaded = Collections.synchronizedSet(new HashSet<>());
	private EntityTableData data;
	
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
				
				StringBuilder sb = new StringBuilder();
				sb.append("CREATE TABLE ");
				sb.append(DB_NAME_NAME);
				sb.append(" ( entity_uuid char(16) for bit data, entity_type_uuid char(16) for bit data, i_primary_id varchar(1024), ");
				sb.append(COL_ENTITY_TYPE_NAME);
				sb.append(" varchar(1024), ");
				
				List<IntelEntityTypeAttribute> etattributes = session.createQuery("FROM IntelEntityTypeAttribute WHERE id.attribute.conservationArea = :ca")
				.setParameter("ca", ca)
				.list();
				
				List<IntelAttribute> eattributes = new ArrayList<>();
				etattributes.forEach( a-> {
					if (!eattributes.contains(a.getAttribute())) eattributes.add(a.getAttribute());
				});
				

				for (int i = 0; i < eattributes.size(); i ++) {
					IntelAttribute attribute = eattributes.get(i);
					sb.append(attribute.getKeyId());
					switch(attribute.getType()) {
					case BOOLEAN:
					case NUMERIC:
						sb.append(" double");
						break;
					case DATE:
					case EMPLOYEE:
					case LIST:
						sb.append("_uuid char(16) for bit data, ");
						sb.append(attribute.getKeyId() + " varchar(1024)");
						
						break;
					case POSITION:
					case TEXT:
						sb.append(" varchar(1024)");
						break;
					}
					sb.append(", ");
				}
				sb.deleteCharAt(sb.length() - 1);
				sb.deleteCharAt(sb.length() - 1);
				
				sb.append(")");
			
				session.createNativeQuery(sb.toString()).executeUpdate();
				
				//now we need to populate this table
				
				//entity uuid for this ca
				sb = new StringBuilder();
				sb.append(" INSERT INTO ");
				sb.append( DB_NAME_NAME );
				sb.append(" (entity_uuid, entity_type_uuid, i_primary_id)");
				sb.append(" SELECT e.uuid, t.uuid, a.keyid FROM smart.i_entity e join smart.i_entity_type t on e.entity_type_uuid = t.uuid join smart.i_attribute a on t.id_attribute_uuid = a.uuid " );
				sb.append(" WHERE e.ca_uuid = :ca");
				session.createNativeQuery(sb.toString())
					.setParameter("ca", ca.getUuid())
					.executeUpdate();
				
				//attributes
				for (int i = 0; i < eattributes.size(); i ++) {
					
					IntelAttribute attribute = eattributes.get(i);
					System.out.println("processing attribute: " + attribute.getName() + " " + i + "/" + eattributes.size());
					
					sb = new StringBuilder();
					sb.append("UPDATE " + DB_NAME_NAME);
					sb.append(" SET ");
					
					switch(attribute.getType()) {
						case BOOLEAN:
						case NUMERIC:
							sb.append(attribute.getKeyId() + " = (SELECT ");
							sb.append(" v.double_value ");
							break;
						case DATE:
						case EMPLOYEE:
						case TEXT:
							sb.append(attribute.getKeyId() + " = (SELECT ");
							sb.append("v.string_value");
							break;
						case POSITION:
							sb.append(attribute.getKeyId() + " = (SELECT ");
							sb.append(" smart.toPoint(v.double_value, v.double_value2) ");
							break;
						case LIST:
							sb.append(attribute.getKeyId() + "_uuid = (SELECT ");
							sb.append(" v.list_item_uuid");				
					}
					sb.append(" FROM smart.i_entity_attribute_value v join smart.i_attribute a on v.attribute_uuid = a.uuid ");
					sb.append(" WHERE a.keyid = :attribute and v.entity_uuid = ");
					sb.append( DB_NAME_NAME );
					sb.append(".entity_uuid)");
					
					session.createNativeQuery(sb.toString())
						.setParameter("attribute", attribute.getKeyId())
						.executeUpdate();
				}
				
				
				//list entity attributes
				for (IntelAttribute attribute : eattributes) {
					if (attribute.getType() != AttributeType.LIST) continue;
					
					sb = new StringBuilder();
					sb.append("SELECT distinct " + attribute.getKeyId() + "_uuid FROM " + DB_NAME_NAME + "");
					List<?> listItems = session.createNativeQuery(sb.toString()).list();
					for (Object x : listItems) {
						if (x == null) continue;
						UUID listItemUuid = UuidUtils.byteToUUID( (byte[]) x );
						IntelAttributeListItem item = session.get(IntelAttributeListItem.class, listItemUuid);
					
						sb = new StringBuilder();
						sb.append(" UPDATE ");
						sb.append(DB_NAME_NAME);
						sb.append(" SET " + attribute.getKeyId() + " = :name WHERE " + attribute.getKeyId() + "_uuid = :uuid");
						
						session.createNativeQuery(sb.toString())
							.setParameter("name", item.getName())
							.setParameter("uuid", listItemUuid)
							.executeUpdate();
					}
				}
				
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
				EntityTableData data = new EntityTableData();
				data.attributes = eattributes;
				data.tableName = DB_NAME_NAME;
				data.totalCount = count;
				
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
		loaded.clear();
		this.viewer = (TableViewer) viewer;
		if (newInput == null || !(newInput instanceof EntityTableData)) {
			data = null;
			return;
		}
		data= (EntityTableData) newInput;
	}
	
	private String sortColumn = null;
	private int sortDirection = SWT.UP;
	
	public void setSortColumn(String sortColumn) {
		if (this.sortColumn == sortColumn) {
			if (sortDirection == SWT.UP) {
				sortDirection = SWT.DOWN;
			}else if (sortDirection == SWT.DOWN) {
				sortDirection = SWT.UP;
			}
		}
		this.sortColumn = sortColumn;
		loaded.clear();
		for (int i = 0; i < data.getTotalCount(); i ++) viewer.clear(i);
		viewer.refresh();
	}

	public int getSortDirection() {
		return this.sortDirection;
	}
	
	class EntityTableData {
		
		private String tableName;
		private List<IntelAttribute> attributes;	
		private int totalCount = 0;
		
		public List<IntelAttribute> getAttributes(){ return attributes; }
		
		public int getTotalCount() { return totalCount; }
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
				sb.append(",");
				for (int i = 0; i < data.getAttributes().size(); i ++) {
					sb.append(data.getAttributes().get(i).getKeyId() );
					sb.append(",");
				}
				sb.deleteCharAt(sb.length() - 1);
				sb.append(" FROM ");
				sb.append( data.tableName );
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
				results.first();
				results.scroll(startIndex);

				int cnt = 0;
				while(cnt < pageSize) {
					Object[] data = results.get();
					EntityTableRowItem item = asRowItem(data);
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
		
		private EntityTableRowItem asRowItem(Object[] rowdata) {
			//entity_uuid , i_primary_id , i_entity_type_name
			
			UUID entityUuid = UuidUtils.byteToUUID((byte[])rowdata[0]);
			String id = (String)rowdata[1];
			String type = (String)rowdata[2];
			
			EntityTableRowItem item = new EntityTableRowItem(entityUuid, id);
			item.setType(type);
			
			for (int i = 0; i < data.attributes.size(); i ++) {
				String key = data.attributes.get(i).getKeyId();
				Object value = rowdata[i + 3];
				item.setAttribute(key, value);
			}
			return item;
			
		}
		
	}
}
