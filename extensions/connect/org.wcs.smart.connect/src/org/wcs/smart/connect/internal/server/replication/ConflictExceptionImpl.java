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
package org.wcs.smart.connect.internal.server.replication;

import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ChangeLogItem;
import org.wcs.smart.connect.replication.changelog.ConflictException;

/**
 * Implementation of ConflictException that
 * localizes message.
 * 
 * @author Emily
 *
 */
public class ConflictExceptionImpl extends ConflictException{

	private static final long serialVersionUID = 1L;

	public ConflictExceptionImpl(ChangeLogItem item) {
		super(item);
	}

	@Override
	protected String createMessage(ChangeLogItem item) {
		StringBuilder sb= new StringBuilder();
		sb.append(Messages.ConflictExceptionImpl_ConflictMessage);
		sb.append("\n"); //$NON-NLS-1$
		if (item.getFileName() != null){
			sb.append(Messages.ConflictExceptionImpl_FileLabel + item.getFileName());
		}else{
			sb.append(Messages.ConflictExceptionImpl_TableLabel);
			sb.append(item.getTableName());
			sb.append("\n"); //$NON-NLS-1$
			sb.append(item.getFieldName1() + ": " + item.getKey1().toString()); //$NON-NLS-1$
			if (item.getFieldName2() != null){
				sb.append("\n"); //$NON-NLS-1$
				sb.append(item.getFieldName2() + ": " ); //$NON-NLS-1$
				if (item.getKey2() != null){
					sb.append(item.getKey2().toString());	
				}else{
					sb.append(item.getKey2String());
				}
			}
		}
		sb.append("\n\n"); //$NON-NLS-1$
		sb.append(Messages.ConflictExceptionImpl_ResolutionMessage);
			
		return sb.toString();
	}

}
