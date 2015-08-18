package org.wcs.smart.er.query.filter;

import java.util.UUID;

import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;
import org.wcs.smart.util.UuidUtils;

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
		UUID uuid = null;
		try {
			uuid = UuidUtils.stringToUuid(bits[2]);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
		boolean isLeader = bits[1].equals("missionleader"); //$NON-NLS-1$
		
		return new MissionMemberFilter(uuid, isLeader);
	}

	private UUID uuid;
	private boolean isLeader;
	
	public MissionMemberFilter(UUID employeeUuid, boolean isLeader){
		this.uuid = employeeUuid;
		this.isLeader = isLeader;
	}
	
	/**
	 * Get the employee uuid
	 * @return
	 */
	public UUID getUuid(){
		return this.uuid;
	}
	
	/**
	 * Sets the employee uuid
	 * @param uuid
	 */
	public void setUuid(UUID uuid){
		this.uuid = uuid;
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
			return LEADER_QUERY_KEY + UuidUtils.uuidToString(uuid);
		}else{
			return MEMBER_QUERY_KEY + UuidUtils.uuidToString(uuid);
		}
	}

	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
	}

}