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

package org.wcs.smart.plan.model;


import java.text.MessageFormat;
import java.util.Locale;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.wcs.smart.SmartContext;
import org.wcs.smart.plan.IPlanLabelProvider;


/**
 * Represents a Administrative plan target
 * 
 * @author Jeff
 * @since 1.0.0
 */
@Entity
@DiscriminatorValue("ADMINISTRATIVE")
public class AdministrativePlanTarget extends PlanTarget{
	public static final String SUMMARY_KEY = "adminsummary"; //$NON-NLS-1$
	public static final int MAX_DESC_LENGTH = 256;
	
	private String targetDesc;
	private boolean status;
	
	public AdministrativePlanTarget(){
		status = false;//always default to false 
	}

	@Column(name = "description")
	public String getTargetDesc() {
		return targetDesc;
	}
	public void setTargetDesc(String targetDesc) {
		this.targetDesc = targetDesc;
	}
	
	@Override
	public AdministrativePlanTarget clone() {
		AdministrativePlanTarget n = new AdministrativePlanTarget();
		super.clone(n);//method in Superclass, adds the shared values to n (name, category, plan)
		n.targetDesc = this.targetDesc;
		n.status = this.status;
		return n;
	}
	
	@Column(name = "completed")
	public boolean getStatus(){
		return status;
	}
	
	public void setStatus(boolean s) {
		status = s;
	}

	@Override
	public String getSummary(Locale l) {
		return MessageFormat.format(
				SmartContext.INSTANCE.getClass(IPlanLabelProvider.class).getLabel(SUMMARY_KEY, l),
				getName());
	}
}