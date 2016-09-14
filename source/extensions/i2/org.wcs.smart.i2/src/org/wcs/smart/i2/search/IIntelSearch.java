package org.wcs.smart.i2.search;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.i2.model.IntelEntity;

public interface IIntelSearch {

	public List<IntelEntity> doSearch(Session session);
}
