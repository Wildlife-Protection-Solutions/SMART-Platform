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

import java.util.UUID;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.model.SurveyDesign.State;

/**
 * The Survey Design Editor
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class SurveyDesignEditorInput implements IEditorInput {

	private UUID uuid;
	private String name;
	private SurveyDesign.State state;
	private String key;
	
	/**
	 * Constructor
	 */
	public SurveyDesignEditorInput(String name, UUID uuid, String key, SurveyDesign.State state) {
		this.uuid = uuid;
		this.name = name;
		this.state = state;
		this.key = key;
	}
	
	/**
	 * Survey design state
	 * 
	 * @return
	 */
	public SurveyDesign.State getState(){
		return this.state;
	}
	
	public UUID getUuid() {
		return uuid;
	}
	
	public String getSurveyDesignKey(){
		return this.key;
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
		if (state == State.INACTIVE){
			return EcologicalRecordsPlugIn.getDefault().getImageRegistry().getDescriptor(EcologicalRecordsPlugIn.SURVEY_DESIGN_INACTIVE_ICON);
		}else{
			return EcologicalRecordsPlugIn.getDefault().getImageRegistry().getDescriptor(EcologicalRecordsPlugIn.SURVEY_DESIGN_ICON);
		}
	}

	@Override
	public String getName() {
		if (this.name == null){
			return ""; //$NON-NLS-1$
		}
		return this.name; 
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
			return uuid.equals(((SurveyDesignEditorInput) other).getUuid());
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return uuid.hashCode();
	}
}
