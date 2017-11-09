/*
 * Copyright (C) 2017 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.asset.ui.views.stationlocation;

import java.util.Objects;
import java.util.UUID;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * Editor input for asset editor
 * 
 * @author Emily
 *
 */
public class StationLocationEditorInput implements IEditorInput {

	private UUID locationUuid;
	private String locationId;
	
	public StationLocationEditorInput(UUID locationUuid, String locationId) {
		this.locationId = locationId;
		this.locationUuid = locationUuid;
	}
	
	public UUID getStationLocationUuid() {
		return this.locationUuid;
	}
	
	public void setStationLocationUuid(UUID locationUuid) {
		this.locationUuid = locationUuid;
	}
	
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		return locationId;
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return null;
	}

	public boolean equals(Object other) {
		if (this == other) return true;
		if (other == null) return false;
		if (getClass() != other.getClass()) return false;
		if (locationUuid == null) return false;
		return Objects.equals(locationUuid, ((StationLocationEditorInput)other).locationUuid); 
	}
	
	public int hashCode() {
		if (locationUuid == null) return super.hashCode();
		return Objects.hash(locationUuid);
	}
	
}
