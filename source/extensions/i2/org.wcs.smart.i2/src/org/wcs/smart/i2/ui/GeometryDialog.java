package org.wcs.smart.i2.ui;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ui.properties.DialogConstants;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTReader;

public class GeometryDialog extends TitleAreaDialog {

	private Text txtGeometry;
	
	private Geometry geometry;
	private Geometry newGeometry;
	private WKTReader parser = new WKTReader();
	
	public GeometryDialog(Shell parentShell, Geometry  geometry) {
		super(parentShell);
		this.geometry = geometry;
	}

	@Override
	protected org.eclipse.swt.graphics.Point getInitialSize() {
		org.eclipse.swt.graphics.Point p = super.getInitialSize();
		return new org.eclipse.swt.graphics.Point(Math.min(p.x, 600), Math.min(400,  p.y));
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		parent = new Composite(parent, SWT.NONE);
		parent.setLayout(new GridLayout());
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		txtGeometry = new Text(parent, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		String text = geometry.toText();
		text = text.replaceAll(", ", ", \n ");
		txtGeometry.setText(text);
		
		txtGeometry.addListener(SWT.Modify, e-> validate());
		txtGeometry.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		setTitle("Edit Geometry");
		setMessage("Enter the well-known text representation of the geometry.");
		getShell().setText("Location");
		return parent;
	}
	
	private void validate(){
		String message = null;
		try{
			newGeometry = null;
			newGeometry = parser.read(txtGeometry.getText());
			if (!(newGeometry instanceof Point || newGeometry instanceof Polygon)){
				message = "Geometry must be of type Point or Polygon";
			}
		}catch (Exception ex){
			message = "Invalid geometry";
		}
		setErrorMessage(message);
		getButton(IDialogConstants.OK_ID).setEnabled(message == null);
	}
	
	public Geometry getNewGeometry(){
		return this.newGeometry;
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		
		getButton(IDialogConstants.OK_ID).setEnabled(false);
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
}