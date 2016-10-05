package org.wcs.smart.i2.ui.views;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LayerVisibleEvent {

	public List<UUID> allVisible = new ArrayList<UUID>();
	public List<UUID> partVisible = new ArrayList<UUID>();
	public List<UUID> notVisible = new ArrayList<UUID>();
}
