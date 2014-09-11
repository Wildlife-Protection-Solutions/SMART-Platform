package org.wcs.smart.er.query.filter;

import java.text.MessageFormat;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.impl.ErrorDropItem;
import org.wcs.smart.util.SmartUtils;

public class MissionMemberFilter implements IFilter {

	public static final String MEMBER_QUERY_KEY = "s:missionmember:"; //$NON-NLS-1$
	public static final String LEADER_QUERY_KEY = "s:missionleader:"; //$NON-NLS-1$
	
	/**
	 * Creates a mission filter.
	 * 
	 * @return
	 */
	public static MissionMemberFilter createFilter(String key){
		String[] bits = key.split(":"); //$NON-NLS-1$
		byte[] uuid = null;
		try {
			uuid = SmartUtils.decodeHex(bits[2]);
		} catch (Exception e) {
			//TODO:
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		boolean isLeader = bits[1].equals("missionleader"); //$NON-NLS-1$
		
		return new MissionMemberFilter(uuid, isLeader);
	}

	private byte[] uuid;
	private boolean isLeader;
	
	public MissionMemberFilter(byte[] employeeUuid, boolean isLeader){
		this.uuid = employeeUuid;
		this.isLeader = isLeader;
	}
	
	/**
	 * Get the employee uuid
	 * @return
	 */
	public byte[] getUuid(){
		return this.uuid;
	}
	
	/**
	 * if leader of member filter
	 * @return
	 */
	public boolean isLeader(){
		return this.isLeader;
	}

	
	@Override
	public String asString() {
		if (isLeader()){
			return LEADER_QUERY_KEY + SmartUtils.encodeHex(uuid);
		}else{
			return MEMBER_QUERY_KEY + SmartUtils.encodeHex(uuid);
		}
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public DropItem[] getDropItems(Session session) throws Exception {
		Employee e = (Employee) session.load(Employee.class, uuid);
		if (e == null){
			return new DropItem[]{new ErrorDropItem(MessageFormat.format(Messages.MissionMemberFilter_EmployeeNotFound, new Object[]{SmartUtils.encodeHex(uuid)}))};
		}
		e.getFullLabel();

		DropItem di = null;
		if (isLeader() ){
			di = SurveyDropItemFactory.INSTANCE.createMissionLeaderDropItem();
		}else{
			di = SurveyDropItemFactory.INSTANCE.createMissionMemberDropItem();
		}
		di.initializeData(e);
		
		return new DropItem[]{di};

	}

}