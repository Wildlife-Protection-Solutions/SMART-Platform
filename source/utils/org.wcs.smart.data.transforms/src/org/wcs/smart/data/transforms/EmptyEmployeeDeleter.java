package org.wcs.smart.data.transforms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.wcs.smart.patrol.xml.model.PatrolLegType;
import org.wcs.smart.patrol.xml.model.PatrolMemberType;
import org.wcs.smart.patrol.xml.model.PatrolType;

/**
 * This deleter removes any employees whose name and id is blank.
 * 
 * @author Emily
 *
 */
public class EmptyEmployeeDeleter implements IDataProcessor {
	
	private void deleteEmptyMembers(PatrolType patrol) throws Exception{

		for (PatrolLegType leg : patrol.getLegs()){
			List<PatrolMemberType> toDelete = new ArrayList<PatrolMemberType>();
			for (PatrolMemberType member: leg.getMembers()){
				if (member.getEmployeeId().isEmpty() && member.getFamilyName().isEmpty() && member.getGivenName().isEmpty()){
					toDelete.add(member);
				}
			}
			leg.getMembers().removeAll(toDelete);
		}

	}
	
	@Override
	public void processFile(File in, File out) throws Exception{
		PatrolType pt = DataUtils.readPatrol(in);
		deleteEmptyMembers(pt);
		DataUtils.writePatrol(out, pt);
	}
	
	
	public static void main(String args[]){
		EmptyEmployeeDeleter deleter =  new EmptyEmployeeDeleter();
		DataUtils.processConfiguration(args, deleter, false);
	}
	
}
