package org.wcs.smart.cybertracker.export;

import java.nio.file.Path;

import org.eclipse.swt.widgets.Composite;

public interface IPackageContribution {

	public void createUi(Composite parent);
	
	public void updatePackageFiles(Path fileDirectory);

}
