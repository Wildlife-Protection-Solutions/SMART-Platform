package org.wcs.smart.query.common.model.udig;

import java.io.IOException;

import net.refractions.udig.catalog.IService;

import org.eclipse.core.runtime.IProgressMonitor;

public abstract class IQueryService extends IService{

	public abstract void refresh(IProgressMonitor monitor) throws IOException;
}
