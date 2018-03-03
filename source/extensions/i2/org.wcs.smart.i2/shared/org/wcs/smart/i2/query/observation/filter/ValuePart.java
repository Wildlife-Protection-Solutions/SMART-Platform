package org.wcs.smart.i2.query.observation.filter;

import com.ibm.icu.text.MessageFormat;

public class ValuePart {
	
	public static ValuePart parse(String option) {
		for (ValueOption op : ValueOption.values()) {
			if (op.getKey().equals(option))
				return new ValuePart(op);
		}
		throw new IllegalStateException(MessageFormat.format("{0} is not a supported value", option));
	}
	
	
	public enum ValueOption{
		NUMBER_ENTITIES("numentities");
		
		String key;
		
		ValueOption(String key){
			this.key = key;
		}
		
		public String getKey() { return this.key; }
		
	}
	
	private ValueOption op;
	
	public ValuePart(ValueOption op) {
		this.op = op;
	}
	
	public ValueOption getValueOption() {
		return this.op;
	}
	
	public String asString() {
		return op.key;
	}
}
