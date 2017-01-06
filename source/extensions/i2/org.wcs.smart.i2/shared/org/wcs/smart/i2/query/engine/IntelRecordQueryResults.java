package org.wcs.smart.i2.query.engine;

import java.sql.Date;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryColumn;
import org.wcs.smart.i2.query.IResultItem;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Envelope;

public class IntelRecordQueryResults implements IPagedQueryResultSet {

	
	private String resultsTable = null;
	private int totalItems;
	private int categoryCnt;
	
	public IntelRecordQueryResults(String resultsTable, int totalItems, int categoryCnt){
		this.resultsTable = resultsTable;
		this.totalItems = totalItems;
		this.categoryCnt = categoryCnt;
	}
	
	private UUID asUuid(ScrollableResults sc, int index){
		Object x = sc.get(index);
		if (x == null) return null;
		if (x instanceof UUID) return (UUID) x;
		if (x instanceof byte[]) return UuidUtils.byteToUUID((byte[])x);
		return null;
	}
	private IntelRecordResultItem asResultItem(ScrollableResults sc, Session session){
		
		IntelRecordResultItem item = new IntelRecordResultItem();
		
		item.setObservationUuid(asUuid(sc,0));
		item.setLocationUuid(asUuid(sc,1));
		item.setRecordUuid(asUuid(sc,2));
		item.setRecordStatus((String)sc.get(3));
		item.setRecordTitle((String)sc.get(4));
		
		item.setLocationId((String)sc.get(5));
		item.setLocationDate((Timestamp)sc.get(6));
		item.setLocationComment((String)sc.get(7));
		//item.setGeometry((byte[])sc.get(8));TODO:
		item.setCategoryUuid(asUuid(sc,9));
		
		List<String> categories = new ArrayList<String>();
		for (int i = 0; i < categoryCnt; i ++){
			Object x = sc.get(i + 10);
			if (x != null) categories.add((String)x);
		}
		item.setCategoryLabels(categories.toArray(new String[categories.size()]));
		
		return item;
	}
	@Override
	public List<? extends IResultItem> getData(int offset, int pageSize) {
		final List<IntelRecordResultItem> items = new ArrayList<>();

		Session session = HibernateManager.openSession();
		try{
			ScrollableResults sc = session.createSQLQuery("SELECT * FROM " + resultsTable).scroll();
			try{
				if (!sc.setRowNumber(offset)) return items;
				for (int i = 0; i <= pageSize; i ++){
					items.add(asResultItem(sc, session));
					if (!sc.next()) break; //nothing else to get
				}
			}finally{
				sc.close();
			}	
		}finally{
			session.close();
		}
		return items;
	}

	@Override
	public Envelope getEnvelope() {
		return null;
	}

//	@Override
//	public IQueryResultSetIterator<? extends IResultItem> iterator(int pageSize) {
//		return null;
//	}
//
//	@Override
//	public IQueryResultSetIterator<? extends IResultItem> iterator(int pageSize, Session session) {
//		return null;
//	}

	@Override
	public void setSorting(IQueryColumn sortColumn, int direction) {
	}

	@Override
	public int getItemCount() {
		return totalItems;
	}

	@Override
	public void dispose(Session session) throws SQLException {
		String sql = "DROP TABLE " + resultsTable;
		resultsTable = null;
		session.createSQLQuery(sql).executeUpdate();
		
	}

	@Override
	public boolean isDisposed() {
		return resultsTable == null;
	}

}
