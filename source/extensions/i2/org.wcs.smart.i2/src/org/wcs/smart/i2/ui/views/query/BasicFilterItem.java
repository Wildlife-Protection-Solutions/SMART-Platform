package org.wcs.smart.i2.ui.views.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;

public class BasicFilterItem extends FilterItem {

	protected List<FilterItem> kids = null;
	protected FilterItem parent;
	protected ImageDescriptor image;
	
	public BasicFilterItem(String name){
		super(name);	
	}
	
	public ImageDescriptor getImage(){
		return image;
	}
	
	public void setImageDescriptor(ImageDescriptor img){
		this.image = img;
	}
	
	public void setParent(BasicFilterItem parent){
		this.parent = parent;
	}
	
	public void addChild(BasicFilterItem kid){
		if (kids == null) kids = new ArrayList<FilterItem>();
		kid.setParent(this);
		kids.add(kid);
	}
	@Override
	public List<FilterItem> getChildren() {
		if (kids == null || kids.isEmpty()) return null;
		return Collections.unmodifiableList(kids);
	}

	@Override
	public FilterItem getParent() {
		return parent;
	}
	
	@Override
	public DropItem[] asDropItem() {
		return null;
	}

}
