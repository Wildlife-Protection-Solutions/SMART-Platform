package org.wcs.smart.query.common.model.udig;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;

import net.refractions.udig.catalog.IService;

public abstract class IQueryService extends IService{

	public abstract void refresh(IProgressMonitor monitor) throws IOException;
}
