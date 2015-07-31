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
package org.wcs.smart.er.query.ui.dropitems;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IFilterDropItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Sampling unit drop item.  This works for both
 * fixed sampling unit items and mission tracks that
 * are sampling units.
 * 
 * @author Emily
 *
 */
public class SamplingUnitDropItem extends DropItem implements ISurveyDesignDropItem, IFilterDropItem{

	private Color redColor = null;
	private Color defaultColor = null;
	
	private SamplingUnitFilter.Source source = null;	
	private SamplingUnit su = null;
	private MissionTrack mt = null;
	private SurveyDesign sd = null;
	
	private Label label = null;
		
	public SamplingUnitDropItem(SamplingUnit su){
		this.su = su;
	}
	
	public SamplingUnitDropItem(SamplingUnit su, SamplingUnitFilter.Source source){
		this.su = su;
		this.source = source;
	}

	public SamplingUnitDropItem(MissionTrack mt){
		this.mt = mt;
	}
	
	
	public void setSource(SamplingUnitFilter.Source source){
		this.source = source;
	}

	public String getErrorMessage(){
		if (sd == null){
			return Messages.SamplingUnitDropItem_DesignRequired;
		}else if (su.getUuid() != null && !sd.equals(su.getSurveyDesign())){
			//null uuid is none option; which doesn't need sd
			return Messages.SamplingUnitDropItem_InvalidDesign;
		}
		return null;
	}
		
	@Override
	public String getText() {
		String error = getErrorMessage();
		if (error != null){
			return error;
		}
		return MessageFormat.format(Messages.SamplingUnitDropItem_Label, new Object[]{su == null? mt.getId() : su.getId(), source.guiName});
	}

	@Override
	public String asQueryPart() {
		String error = getErrorMessage();
		if (error != null){
			return null;
		}
		
		if (su != null){
			if (su == SamplingUnitFilter.NONE){
				return "s:samplingunit:" + source.queryKey + ":" + SamplingUnitFilter.NONE_KEY; //$NON-NLS-1$ //$NON-NLS-2$
			}else{
				return "s:samplingunit:" + source.queryKey + ":" + UuidUtils.uuidToString(su.getUuid()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
		}else if (mt != null){
			return "s:samplingunittrack:" + source.queryKey + ":" + UuidUtils.uuidToString(mt.getUuid()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return null;
	}

	@Override
	public void initializeData(Object data) {
	}

	@Override
	public void dispose(){
		super.dispose();
		if (redColor != null){
			redColor.dispose();
		}
	}
	@Override
	protected void createComposite(Composite parent) {
		redColor =  new Color(Display.getDefault(),new RGB(255, 210,210) );
		
		Composite c = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		c.setLayout(gl);
		label = new Label(c, SWT.NONE);
		label.setText(getText());
		initDrag(label);
		initDrag(c);
		defaultColor = label.getBackground();
	}

	private void updateLabel(){
		String msg = getErrorMessage();
		if (msg != null){
			label.setBackground(redColor);
			label.getParent().getParent().setBackground(redColor);
		}else{
			label.setBackground(defaultColor);
			label.getParent().getParent().setBackground(defaultColor);
		}
		label.setText(getText());
		getTargetPanel().redraw();
	}
	
	@Override
	public void setSurveyDesign(SurveyDesign design) {
		this.sd = design;
		updateLabel();
	}
}
