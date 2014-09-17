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
package org.wcs.smart.er.ui.surveydesign.editor;

import java.util.Arrays;
import java.util.Date;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.er.EcologicalRecordsPlugIn;

/**
 * Editor input for surveys
 * @author Emily
 *
 */
public class SurveyEditorInput implements IEditorInput {

	private byte[] uuid;
	private String id;
	private Date startDate;
	private String designName;
	/**
	 * Constructor
	 */
	public SurveyEditorInput(String id, byte[] uuid, Date startDate, String designName) {
		this.uuid = uuid;
		this.id = id;
		this.startDate = startDate;
		this.designName = designName;
	}
	
	public String getSurveyDesignName(){
		return this.designName;
	}
	
	public byte[] getUuid() {
		return uuid;
	}
	
	public Date getStartDate(){
		return this.startDate;
	}
	
	public String getSurveyId(){
		return this.id;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return EcologicalRecordsPlugIn.getDefault().getImageRegistry().getDescriptor(EcologicalRecordsPlugIn.SURVEY_ICON);
	}

	@Override
	public String getName() {
		return this.id + " [" + designName + "]"; 
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public boolean equals(Object other){
		if (other instanceof SurveyDesignEditorInput){
			return Arrays.equals(uuid, ((SurveyDesignEditorInput) other).getUuid());
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return Arrays.hashCode(uuid);
	}
}
