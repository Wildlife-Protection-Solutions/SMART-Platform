/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.connect.cybertracker.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.util.UuidUtils;

/**
 * Internal model for smart connect alerts for
 * cybertracker packages
 */
public class CtPackageAlert {

	private static final String SEPERATOR = ","; //$NON-NLS-1$

	/*
	 * Values for connect alert level
	 */
	public static enum Level {
		ONE(1), TWO(2), THREE(3), FOUR(4), FIVE(5);

		public int value;

		private Level(int value) {
			this.value = value;
		}
		
		public static Level fromLevel(int v) {
			for (Level l : Level.values()) {
				if (l.value == v) return l;
			}
			return Level.ONE;
		}
	}

	private ICtPackage ctpackage;

	// this is cmnode
	private CmNode cmNode;
	private CmAttribute attribute;
	private CmAttributeTreeNode attributeTreeNode;
	private CmAttributeListItem attributeListItem;

	private UUID type;
	private Level level;

	public ICtPackage getPackage() {
		return ctpackage;
	}

	public void setPackage(ICtPackage ctpackage) {
		this.ctpackage = ctpackage;
	}

	public CmNode getCmNode() {
		return cmNode;
	}

	public void setCmNode(CmNode cmNode) {
		this.cmNode = cmNode;
	}

	public CmAttribute getCmAttribute() {
		return attribute;
	}

	public void setCmAttribute(CmAttribute attribute) {
		this.attribute = attribute;
	}

	public CmAttributeListItem getCmAttributeListItem() {
		return attributeListItem;
	}

	public void setCmAttributeListItem(CmAttributeListItem attributeListItem) {
		this.attributeListItem = attributeListItem;
	}

	public CmAttributeTreeNode getCmAttributeTreeNode() {
		return attributeTreeNode;
	}

	public void setCmAttributeTreeNode(CmAttributeTreeNode attributeTreeNode) {
		this.attributeTreeNode = attributeTreeNode;
	}

	public UUID getType() {
		return type;
	}

	public void setType(UUID type) {
		this.type = type;
	}

	public Level getLevel() {
		return level;
	}

	public void setLevel(Level level) {
		this.level = level;
	}
	
	
	public static List<CtPackageAlert> fromString(AbstractCtPackage ctpackage, Session session) {
		List<CtPackageAlert> items = new ArrayList<>();
		for (MetadataFieldValue v : ctpackage.getMetadataValues()) {
			if (!v.getMetadataKey().equals(CtConnectPackageMetadata.Properties.CONNECT_ALERT.name())) continue;
			CtPackageAlert alert = new CtPackageAlert();
			alert.setPackage(ctpackage);
			
			//level
			String[] bits = v.getStringValue().split(SEPERATOR);
			alert.setLevel(Level.fromLevel(Integer.valueOf(bits[0])));
			//type
			alert.setType(UuidUtils.stringToUuid(bits[1]));
			//node
			if (bits[2].length() > 1) {
				UUID node = UuidUtils.stringToUuid(bits[2]);
				CmNode cmnode = session.get(CmNode.class, node);
				alert.setCmNode(cmnode);
				while (cmnode != null) {
					cmnode.getName();
					if (cmnode.getCategory() != null) cmnode.getCategory().getName();
					cmnode= cmnode.getParent();
				}
			}
			//cmattribute
			if (bits.length > 3 && bits[3].length() > 1) {
				UUID node = UuidUtils.stringToUuid(bits[3]);
				CmAttribute attribute = session.get(CmAttribute.class, node);
				attribute.getName();
				if (attribute.getAttribute() != null) attribute.getAttribute().getName();
				alert.setCmAttribute(attribute);
			}
			//cmattribute list item
			if (bits.length > 4 && bits[4].length() > 1) {
				UUID node = UuidUtils.stringToUuid(bits[4]);
				CmAttributeListItem attribute = session.get(CmAttributeListItem.class, node);
				attribute.getName();
				if (attribute.getListItem() != null) attribute.getListItem().getName();
				alert.setCmAttributeListItem(attribute);
			}
			//cmattribute tree node
			if (bits.length > 5 && bits[5].length() > 1) {
				UUID node = UuidUtils.stringToUuid(bits[5]);
				CmAttributeTreeNode attribute = session.get(CmAttributeTreeNode.class, node);
				attribute.getName();
				if (attribute.getDmTreeNode() != null) attribute.getDmTreeNode().getName();
				alert.setCmAttributeTreeNode(attribute);
			}
			items.add(alert);
		}
		return items;
	}
	
	public MetadataFieldValue toMetadataField() {
		MetadataFieldValue md = new MetadataFieldValue();
		md.setMetadataKey(CtConnectPackageMetadata.Properties.CONNECT_ALERT.name());
		md.setConservationArea(getPackage().getConservationArea());
		md.setCtPackage((AbstractCtPackage)getPackage());
		
		StringBuilder sb = new StringBuilder();
		sb.append(getLevel().value);
		sb.append(SEPERATOR);
		sb.append(UuidUtils.uuidToString( getType() ));
		sb.append(SEPERATOR);
		if (getCmNode() != null) {
			sb.append(UuidUtils.uuidToString( getCmNode().getUuid() ));
		}
		sb.append(SEPERATOR);
		if (getCmAttribute() != null) {
			sb.append(UuidUtils.uuidToString( getCmAttribute().getUuid() ));
		}
		sb.append(SEPERATOR);
		if (getCmAttributeListItem() != null) {
			sb.append(UuidUtils.uuidToString( getCmAttributeListItem().getUuid() ));
		}
		sb.append(SEPERATOR);
		if (getCmAttributeTreeNode() != null) {
			sb.append(UuidUtils.uuidToString( getCmAttributeTreeNode().getUuid() ));
		}
		md.setStringValue(sb.toString());
		return md;
	}
}
