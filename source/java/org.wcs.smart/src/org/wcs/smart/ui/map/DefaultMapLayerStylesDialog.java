package org.wcs.smart.ui.map;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ui.SmartStyledTitleDialog;
import org.wcs.smart.ui.properties.DialogConstants;

public class DefaultMapLayerStylesDialog extends SmartStyledTitleDialog {

	private DefaultMapLayerStylesComposite layerComp;
	
	/**
	 * 
	 * @param parentShell
	 * @param current current projection or null if does not exist
	 */
	public DefaultMapLayerStylesDialog(Shell parentShell) {
		super(parentShell);
	}
	
	@Override
	public Point getInitialSize() {
		Point p = super.getInitialSize();
		return new Point(p.x, 500);
	}
	 
	@Override
	protected void okPressed(){
		try {
			layerComp.save();
			getButton(IDialogConstants.OK_ID).setEnabled(false);
		}catch (Exception ex) {
			SmartPlugIn.displayLog(ex.getMessage(), ex);
		}
		
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		 Composite composite = (Composite)super.createDialogArea(parent);
		 
		 layerComp = new DefaultMapLayerStylesComposite(composite, SWT.NONE);
		 layerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		 layerComp.addListener(SWT.Modify, e->{
			 getButton(IDialogConstants.OK_ID).setEnabled(true);
		 });
		 getShell().setText("Default Map Layer Styles");
		 setTitle("Default Map Layer Styles");
		 setMessage("Configure the default styles for specific map layers");
		 return composite;
	 }
	
	@Override
	protected void createButtonsForButtonBar(Composite parent){
		createButton(parent, IDialogConstants.OK_ID, DialogConstants.SAVE_TEXT, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CLOSE_LABEL, false);
		
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}
