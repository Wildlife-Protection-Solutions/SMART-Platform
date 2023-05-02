package org.wcs.smart.observation.model;

import org.hibernate.Session;

public interface IDataLinkCleanUp {

	public void doCleanUp(Session session);
}
