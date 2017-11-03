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
package org.wcs.smart.asset.ui.views.asset;

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
public class AssetEditorInput implements IEditorInput {

	private UUID assetUuid;
	private String assetId;
	
	private UUID assetTypeUuid;
	
	public AssetEditorInput(UUID assetUuid, String assetId) {
		this.assetId = assetId;
		this.assetUuid = assetUuid;
	}
	
	public AssetEditorInput(UUID assetTypeUuid) {
		this.assetTypeUuid = assetTypeUuid;
	}
	
	
	public UUID getAssetTypeUuid() {
		return this.assetTypeUuid;
	}
	
	public UUID getAssetUuid() {
		return this.assetUuid;
	}
	
	public void setAssetUuid(UUID assetUuid) {
		this.assetUuid = assetUuid;
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
		return assetId;
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
		if (assetUuid == null) return false;
		return Objects.equals(assetUuid, ((AssetEditorInput)other).assetUuid); 
	}
	
	public int hashCode() {
		if (assetUuid == null) return super.hashCode();
		return Objects.hash(assetUuid);
	}
	
}
