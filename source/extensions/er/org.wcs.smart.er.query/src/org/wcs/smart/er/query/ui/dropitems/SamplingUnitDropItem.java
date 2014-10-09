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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.er.query.filter.SamplingUnitFilter;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.util.SmartUtils;

/**
 * Sampling unit drop item.  This works for both
 * fixed sampling unit items and mission tracks that
 * are sampling units.
 * 
 * @author Emily
 *
 */
public class SamplingUnitDropItem extends DropItem {

	private SamplingUnit su = null;
	private MissionTrack mt = null;
	
	
	public SamplingUnitDropItem(SamplingUnit su){
		this.su = su;
	}
	
	public SamplingUnitDropItem(MissionTrack mt){
		this.mt = mt;
	}
	
	@Override
	public String getText() {
		return MessageFormat.format(Messages.SamplingUnitDropItem_Label, new Object[]{su == null? mt.getId() : su.getId()});
	}

	@Override
	public String asQueryPart() {
		if (su != null){
			if (su == SamplingUnitFilter.NONE){
				return "s:samplingunit:" + SamplingUnitFilter.NONE_KEY; //$NON-NLS-1$
			}else{
				return "s:samplingunit:" + SmartUtils.encodeHex(su.getUuid()); //$NON-NLS-1$
			}
			
		}else if (mt != null){
			return "s:samplingunittrack:" + SmartUtils.encodeHex(mt.getUuid()); //$NON-NLS-1$
		}
		return null;
	}

	@Override
	public void initializeData(Object data) {
	}

	@Override
	protected void createComposite(Composite parent) {
		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new GridLayout());
		
		Label l = new Label(c, SWT.NONE);
		l.setText(getText());
		
		initDrag(l);
		
	}

}
