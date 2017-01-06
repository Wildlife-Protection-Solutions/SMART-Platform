package org.wcs.smart.i2.query;

import java.text.MessageFormat;
import java.util.Locale;

import org.wcs.smart.ca.datamodel.Attribute;

public class DataModelColumn extends AbstractQueryColumn{

	private int level = -1;
	private String key;
	
	public DataModelColumn(int level) {
		super(MessageFormat.format("Level {0}", level), "category:" + level);
		this.level = level;
	}

	public DataModelColumn(Attribute attribute){
		super(attribute.getName(), "attribute:" + attribute.getKeyId());
		this.key = attribute.getKeyId();
	}
	
	@Override
	public String getValue(IResultItem item, Locale l) {
		return null;
	}
	

}
