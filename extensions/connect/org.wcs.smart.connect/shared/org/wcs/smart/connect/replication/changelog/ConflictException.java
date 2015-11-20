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
package org.wcs.smart.connect.replication.changelog;

import org.wcs.smart.connect.model.ChangeLogItem;

/**
 * SMART conflict exception thrown when trying to apply a change
 * log record to an row already modified.
 * 
 * @author Emily
 *
 */
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

