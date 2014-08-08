package org.wcs.smart.er.query.ui.dropitems;

import java.text.MessageFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.er.model.MissionTrack;
import org.wcs.smart.er.model.SamplingUnit;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.util.SmartUtils;

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
		return MessageFormat.format("Sampling Unit: {0}", new Object[]{su == null? mt.getId() : su.getId()});
	}

	@Override
	public String asQueryPart() {
		if (su != null){
			return "s:samplingunit:" + SmartUtils.encodeHex(su.getUuid());
		}else if (mt != null){
			return "s:samplingunittrack:" + SmartUtils.encodeHex(mt.getUuid());
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
