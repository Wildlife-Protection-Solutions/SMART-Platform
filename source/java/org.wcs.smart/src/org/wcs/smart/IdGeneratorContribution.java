package org.wcs.smart;

import org.eclipse.swt.widgets.Composite;
import org.hibernate.Session;

public interface IdGeneratorContribution {

	public String initComponent(Session session);
	
	public Composite createComposite(Composite parent);
	
	public void save(Session session);
}
