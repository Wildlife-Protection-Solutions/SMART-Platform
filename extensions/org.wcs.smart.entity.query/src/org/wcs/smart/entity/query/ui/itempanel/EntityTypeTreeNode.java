package org.wcs.smart.entity.query.ui.itempanel;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.query.common.ui.itempanel.IItemTreeNode;
import org.wcs.smart.query.common.ui.itempanel.SummaryDataModelContentProvider;

public class EntityTypeTreeNode implements IItemTreeNode {
	public static final String KEY = "entitytype";

	private ITreeContentProvider provider;
	private LabelProvider labelprovider;

	private String name;

	public enum Type {
		FILTER, GROUPBY, VALUE
	};

	/**
	 * type of node
	 * 
	 * @param type
	 */
	public EntityTypeTreeNode(Type type) {
		if (type == Type.FILTER) {
			provider = new EntityTypeFilterContentProvider();
			labelprovider =EntityTypeFilterContentProvider.lblProvider;
			name = "Entity Type Filters";
		} else if (type == Type.GROUPBY) {
			provider = new EntityTypeSummaryContentProvider(
					EntityTypeSummaryContentProvider.Type.GROUPBY);
			labelprovider = ((EntityTypeSummaryContentProvider) provider)
					.getLabelProvider();
			name = "Entity Type Group Bys";
		} else if (type == Type.VALUE) {
			provider = new EntityTypeSummaryContentProvider(
					EntityTypeSummaryContentProvider.Type.VALUE);
			labelprovider = ((EntityTypeSummaryContentProvider) provider)
					.getLabelProvider();
			name = "Entity Type Values";
		}

	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ITreeContentProvider getContentProvider() {
		return provider;
	}

	@Override
	public ILabelProvider getLabelProvider() {
		return labelprovider;
	}

	@Override
	public Image getImage() {
		return EntityPlugIn.getDefault().getImageRegistry()
				.get(EntityPlugIn.ENTITY_TYPE_ICON);
	}

	@Override
	public String getKey() {
		return KEY;
	}

}
