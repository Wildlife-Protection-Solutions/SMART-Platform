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
package org.wcs.smart.plan.ui.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.model.Plan;

/**
 * Plan EditorInput
 * 
 * @author elitvin
 * @author jeffloun
 * @since 1.0.0
 */
public class PlanEditorInput implements IEditorInput {

	private UUID uuid;
	private String label;
	
	private List<PlanEditorInput> kids;
	private PlanEditorInput parent;
	private Plan.PlanType planType;
	
	/**
	 * Constructor
	 */
	public PlanEditorInput(UUID uuid, String label, Plan.PlanType type) {
		this.uuid = uuid;
		this.label = label;
		this.planType = type;
		
		this.kids = new ArrayList<PlanEditorInput>();
	}

	/**
	 * @return uuid
	 */
	public UUID getUuid(){
		return this.uuid;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#exists()
	 */
	@Override
	public boolean exists() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
	 */
	@Override
	public ImageDescriptor getImageDescriptor() {
		if (planType != null){
			return SmartPlanPlugIn.getDefault().getImageRegistry().getDescriptor(planType.getIconKey());
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	@Override
	public String getName() {
		if (label == null) return ""; //$NON-NLS-1$
		return label;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getPersistable()
	 */
	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getToolTipText()
	 */
	@Override
	public String getToolTipText() {
		return label;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + uuid.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PlanEditorInput other = (PlanEditorInput) obj;
		if (!uuid.equals(other.uuid))
			return false;
		return true;
	}
	
	/**
	 * 
	 * @return the parent plan
	 */
	public PlanEditorInput getParent(){
		return this.parent;
	}
	/**
	 * 
	 * @return the children plans
	 */
	public List<PlanEditorInput> getChildren(){
		return this.kids;
	}
	/**
	 * Adds a kid plan
	 * @param kid
	 */
	public void addKid(PlanEditorInput kid){
		this.kids.add(kid);
	}
	/**
	 * 
	 * @param parent the parent plan
	 */
	public void setParent(PlanEditorInput parent){
		this.parent = parent;
	}
	
}
