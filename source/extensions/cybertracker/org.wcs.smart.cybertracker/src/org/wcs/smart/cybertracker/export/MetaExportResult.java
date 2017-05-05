package org.wcs.smart.cybertracker.export;

import java.util.ArrayList;
import java.util.List;

import org.wcs.smart.cybertracker.export.CyberTrackerUtil.CyberTrackerId;
import org.wcs.smart.cybertracker.export.data.CtDataKeyValueRecord;
import org.wcs.smart.cybertracker.model.screens.Node;

/**
 * Contains CT data generated based of metadata used for export
 * @author elitvin
 * @since 4.0.0
 */
public class MetaExportResult {
	public List<Node> screenNodes = new ArrayList<>();
	public Node nextTaskNode = null;
	public List<IdNamePair> resultElements = new ArrayList<>();
	public CyberTrackerId rootId = null;
	public List<CtDataKeyValueRecord> defaultValues = new ArrayList<>();
	public String tripUniqueElementId;

	public static class IdNamePair {
		public String id;
		public String name;
		public IdNamePair(String id, String name) {
			super();
			this.id = id;
			this.name = name;
		}
	}
}

