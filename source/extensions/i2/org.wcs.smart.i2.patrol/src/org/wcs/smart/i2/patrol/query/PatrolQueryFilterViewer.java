package org.wcs.smart.i2.patrol.query;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.filter.IFilter;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.patrol.PatrolProfilePlugIn;
import org.wcs.smart.i2.patrol.internal.Messages;
import org.wcs.smart.patrol.query.ext.IExtensionFilter;
import org.wcs.smart.patrol.query.ext.IExtensionFilterViewer;
import org.wcs.smart.patrol.query.model.PatrolDropItemFactory;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ErrorDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

public class PatrolQueryFilterViewer implements IExtensionFilterViewer {


	private IntelRecordPatrolQueryOption option = new IntelRecordPatrolQueryOption();

	
	@Override
	public String asSql(IQueryEngine engine, Session session, IFilter filter){
		if (!(filter instanceof IntelRecordPatrolQueryFilter)){
			return null;
		}
		IntelRecordPatrolQueryFilter qfilter = (IntelRecordPatrolQueryFilter)filter;
		
		String prefix = engine.tablePrefix(qfilter.getPatrolQueryOption().getPatrolAttributeClass());
		String v = SharedUtils.stripQuotes((String)qfilter.getValue());
		//if v is empty this means that this is "Any Plan" case
		String intelPart = !qfilter.isAnyRecord() ? " AND p2i.i_record_uuid = x'" + v + "'" : "";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		String sql = "EXISTS (SELECT * FROM smart.i_patrol_record_motivation p2i WHERE p2i.patrol_uuid = " + prefix + ".uuid" + intelPart + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return sql;
	}

	@Override
	public String getName() {
		return Messages.PatrolQueryFilterViewer_MotivatedByName;
	}

	@Override
	public DropItem asDropItem() {
		DropItem it = PatrolDropItemFactory.INSTANCE.createPatrolFilterDropItem(option);
		it.initializeData(new Object[]{new PatrolProfileRecordPatrolData()});
		return it;
	}



	@Override
	public Image getImage() {
		return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RECORD);
	}



	@Override
	public Class<? extends IExtensionFilter> getFilterClass() {
		return IntelRecordPatrolQueryFilter.class;
	}



	@Override
	public DropItem[] getDropItems(IFilter filter, Session session) {
		if (!(filter instanceof IntelRecordPatrolQueryFilter)){
			return null;
		}
		IntelRecordPatrolQueryFilter f = (IntelRecordPatrolQueryFilter)filter;
		
		DropItem it = PatrolDropItemFactory.INSTANCE.createPatrolFilterDropItem(option);
		
		String id = SharedUtils.stripQuotes((String)f.getValue());
		ListItem listItem;
		try {
			listItem = f.isAnyRecord() ? PatrolProfileRecordPatrolData.ANY_ITEM
					: findRecord(session, id);

			it.initializeData(new Object[]{new PatrolProfileRecordPatrolData(), listItem});
		} catch (Exception e) {
			PatrolProfilePlugIn.log(e.getMessage(), e);
			return new DropItem[]{new ErrorDropItem(Messages.PatrolQueryFilterViewer_ParseError)};
		}
		return new DropItem[]{it};
	}
	
	private ListItem findRecord(Session session, String uuid) throws Exception{
		IntelRecord r = session.get(IntelRecord.class, UuidUtils.stringToUuid(uuid));
		if (r == null) throw new Exception(Messages.PatrolQueryFilterViewer_RecordNotFoundError);
		return new ListItem(r.getUuid(), r.getTitle());
	}
}