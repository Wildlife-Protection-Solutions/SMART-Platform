package org.wcs.smart.i2.udig;

import java.util.UUID;

import org.wcs.smart.i2.model.IntelWorkingSetCategory;

public interface IWorkingSetResource {

	public UUID getResourceId();
	
	public IntelWorkingSetCategory getResourceType();
}
