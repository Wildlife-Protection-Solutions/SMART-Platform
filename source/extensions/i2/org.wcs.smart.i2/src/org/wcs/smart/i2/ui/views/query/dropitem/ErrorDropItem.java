package org.wcs.smart.i2.ui.views.query.dropitem;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.SmartPlugIn;
public class ErrorDropItem extends DropItem {

	private Color redColor = null;
	private String errorMessage = null;
	
	public ErrorDropItem(String errorMessage){
		this.errorMessage = errorMessage;
	}
	
	@Override
	public String getText() {
		return errorMessage;
	}
	
	@Override
	public void dispose(){
		super.dispose();
		if (redColor != null){
			redColor.dispose();
		}
		
	}

	@Override
	public String asQueryPart() {
		return null;
	}

	@Override
	protected void createComposite(Composite parent) {
		redColor =  new Color(Display.getDefault(),new RGB(255, 210,210) );
		parent.setBackground(redColor);
		
		Composite c = new Composite(parent, SWT.NONE);
		c.setBackground(redColor);
		GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = gl.marginWidth = 0;
		c.setLayout(gl);
		
		Label lblImage = new Label(c, SWT.NONE);
		lblImage.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
		lblImage.setBackground(redColor);
		Label lbl = new Label(c, SWT.NONE);
		lbl.setText(errorMessage);
		lbl.setBackground(redColor);
	}

}
