package org.wcs.smart.asset.ui.views.asset;

import java.util.Objects;
import java.util.UUID;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

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
