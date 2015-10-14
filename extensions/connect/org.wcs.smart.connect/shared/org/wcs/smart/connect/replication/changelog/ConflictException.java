package org.wcs.smart.connect.replication.changelog;

import org.wcs.smart.connect.model.ChangeLogItem;

public class ConflictException extends Exception{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ConflictException(ChangeLogItem item){
		super(createMessage(item));
	}
	
	private static String createMessage(ChangeLogItem item){
		StringBuilder sb= new StringBuilder();
		sb.append("Change log conflict. Both the server and local databases have modified the same item.");
		sb.append("\n");
		sb.append("Table: ");
		sb.append(item.getTableName());
		sb.append("\n");
		sb.append(item.getFieldName1() + ": " + item.getKey1().toString());
		if (item.getFieldName2() != null){
			sb.append("\n");
			sb.append(item.getFieldName2() + ": " );
			if (item.getKey2() != null){
				sb.append(item.getKey2().toString());	
			}else{
				sb.append(item.getKey2String());
			}
		}
		sb.append("\n\n");
		sb.append("The only way resolve a conflict is to delete your local copy, re-download from connect server, then reapply any local changes you have made.");
		return sb.toString();
		
	}
}

