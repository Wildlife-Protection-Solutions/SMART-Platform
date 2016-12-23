package org.wcs.smart.i2.ui.views.query;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.ui.views.query.dropitem.DateDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.OptionDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextBoxDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextDropItem;

public class AttributeFilterItem extends BasicFilterItem {

	private UUID attributeUuid;
	private UUID entityTypeUuid;
	
	private IntelAttribute.IAttributeType type;
	
	private String dropItemName = null;
	private String queryKey = "";
	public AttributeFilterItem(IntelEntityTypeAttribute attribute) {
		super(attribute.getAttribute().getName());
		this.entityTypeUuid = attribute.getEntityType().getUuid();
		this.attributeUuid = attribute.getAttribute().getUuid();
		type = attribute.getAttribute().getType();
		
		dropItemName = MessageFormat.format("{0} ({1})", attribute.getAttribute().getName(), attribute.getEntityType().getName());
		
		queryKey = "entity_attribute:" + attribute.getAttribute().getKeyId() + ":" + attribute.getEntityType().getKeyId();
	}

	public AttributeFilterItem(IntelAttribute attribute) {
		super(attribute.getName());
		this.attributeUuid = attribute.getUuid();
		type = attribute.getType();
		dropItemName = attribute.getName();
		queryKey = "entity_attribute:" + attribute.getKeyId() + ":" ;
	}

	public IntelAttribute.IAttributeType getType(){
		return this.type;
	}
	
	@Override
	public DropItem[] asDropItem() {
		
		switch(type){
		case BOOLEAN:
			return new DropItem[]{new TextDropItem(dropItemName, queryKey)};
		case DATE:
			return new DropItem[]{new DateDropItem(dropItemName, queryKey)};
		case LIST:
			final List<String> labels = new ArrayList<String>();
			final List<String> keys = new ArrayList<String>();
			labels.add("<ANY>");
			keys.add("any");
			Job j = new Job("creating attribute drop item"){

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Session s = HibernateManager.openSession();
					try{
						IntelAttribute a = (IntelAttribute) s.get(IntelAttribute.class, attributeUuid);
						if (a.getAttributeList() != null){
							for (IntelAttributeListItem i : a.getAttributeList()){
								labels.add(i.getName());
								keys.add(i.getKeyId());
							}
						}
					}finally{
						s.close();
					}
					
					return Status.OK_STATUS;
				}
			};
			j.schedule();
			try {
				j.join();
			} catch (InterruptedException e) {
				Intelligence2PlugIn.displayLog("Error loading attribute list items", e);
			}
			
			return new DropItem[]{new OptionDropItem(dropItemName, queryKey, labels.toArray(new String[labels.size()]), keys.toArray(new String[keys.size()]))};
		case NUMERIC:
			return new DropItem[]{new TextBoxDropItem(dropItemName, queryKey, TextBoxDropItem.InputType.NUMERIC)};
		case TEXT:
			return new DropItem[]{new TextBoxDropItem(dropItemName, queryKey, TextBoxDropItem.InputType.TEXT)};
		default:
			break;
			
		}
		return null;
	}
}
