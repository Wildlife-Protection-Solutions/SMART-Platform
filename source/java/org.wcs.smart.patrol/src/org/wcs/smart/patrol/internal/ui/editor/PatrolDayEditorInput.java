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
package org.wcs.smart.patrol.internal.ui.editor;

import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.Date;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.patrol.internal.Messages;

/**
 * Editor input for a patrol day.  Include the patrol
 * day date, and list of patrol leg day uuids.  
 * @author Emily
 * @since 1.0.0
 */
public class PatrolDayEditorInput  implements IEditorInput {

	private Date patrolDay;
	
	public PatrolDayEditorInput(Date patrolDay){
		this.patrolDay = patrolDay;
	}
	
	
	public Date getPatrolDay(){
		return this.patrolDay;
	}
	
	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#exists()
	 */
	@Override
	public boolean exists() {
		return false;
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
	 */
	@Override
	public ImageDescriptor getImageDescriptor() {		
		//return PatrolUtils.getImageDescriptor(this.type);
		return null;
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	@Override
	public String getName() {	
		return Messages.PatrolDayEditorInput_DayEditorName_Prefix + "_" + patrolDay.getTime(); //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getPersistable()
	 */
	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getToolTipText()
	 */
	@Override
	public String getToolTipText() {
		return MessageFormat.format(Messages.PatrolDayEditorInput_DayEditor_Tooltip, new Object[]{ DateFormat.getDateInstance(DateFormat.MEDIUM).format( patrolDay.getTime() )});
	}

	public int hashCode() {
		return patrolDay.hashCode();
	}

	public boolean equals(Object obj) {
		if (obj != null && obj instanceof PatrolDayEditorInput){
			return patrolDay.equals(  ((PatrolDayEditorInput)obj).getPatrolDay()  );
		}
		return false;
	}
}
