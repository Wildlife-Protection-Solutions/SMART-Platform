package org.wcs.smart.cybertracker.patrol.ui;


import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.ICtPackage;

public class PatrolMetadataPackageContribution implements IPackageUiContribution {

	@Override
	public Composite createUi(Composite parent, ICtPackage ctpackage, Listener onModified) {
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		
		Label l = new Label(main, SWT.NONE);
		l.setText("patrol metadata");

		return main;
	}
	
	@Override
	public String isValid() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updatePackage(ICtPackage ctpackage) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public boolean isTab() { return true; }
	
	@Override
	public String getTabName() { return "Patrol Metadata"; }
	

}
