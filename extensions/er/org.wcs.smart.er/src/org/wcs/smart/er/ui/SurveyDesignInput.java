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
package org.wcs.smart.er.ui;

import java.util.Arrays;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.model.SurveyDesign;

/**
 * Survey design editor input.
 * @author Emily
 *
 */
public class SurveyDesignInput implements IEditorInput {

	private byte[] uuid;
	
	private String name;
	
	private SurveyDesign.State state;
	
	/**
	 * Creates new input
	 * 
	 * @param name
	 * @param uuid
	 * @param state
	 */
	public SurveyDesignInput(String name, byte[] uuid, SurveyDesign.State state){
		this.name = name;
		this.uuid = uuid;
		this.state = state;
	}
	
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}
	
	public byte[] getUuid(){
		return this.uuid;
	}

	public SurveyDesign.State getState(){
		return this.state;
	}
	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return EcologicalRecordsPlugIn.getDefault().getImageRegistry().getDescriptor(EcologicalRecordsPlugIn.SURVEY_DESIGN_ICON);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return null;
	}

	public int hashCode() {
		return Arrays.hashCode(uuid);
	}

	public boolean equals(Object obj) {
		if (obj != null && obj instanceof SurveyDesignInput){
			return Arrays.equals(this.uuid, ((SurveyDesignInput)obj).uuid);
		}
		return false;
	}
}
