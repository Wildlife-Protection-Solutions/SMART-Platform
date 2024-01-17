package org.wcs.smart.i2.udig;

public class DefaultVisibilityProperty {

	private boolean isVisible = false;
	public DefaultVisibilityProperty() {
		this(true);
	}

	public DefaultVisibilityProperty(boolean isVisible) {
		this.isVisible = isVisible;
	}
	
	public boolean isVisibleByDefault() {
		return this.isVisible;
	}
}
