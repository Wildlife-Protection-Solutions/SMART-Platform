package org.wcs.smart.asset.map.engine;

public class AttributeFilter implements IFilter{

	public static AttributeFilter parse(String attributeKey, Operator op, String strValue) {
		return new AttributeFilter(attributeKey, op, strValue);
	}
	
	public static AttributeFilter parse(String attributeKey, Operator op, Double numberValue) {
		return new AttributeFilter(attributeKey, op, numberValue);
	}
	
	private String attributeKey;
	private Operator op;
	
	private String strValue;
	private Double numberValue;
	
	public AttributeFilter(String attributeKey, Operator op, String strValue) {
		this.attributeKey = attributeKey;
		this.op = op;
		this.strValue = strValue;
	}
	
	
	public AttributeFilter(String attributeKey, Operator op, Double numberValue) {
		this.attributeKey = attributeKey;
		this.op = op;
		this.numberValue = numberValue;
	}
	
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(attributeKey);
		sb.append(" ");
		sb.append(op.operator.sql);
		sb.append(" " );
		if (strValue != null) {
			sb.append(strValue);
		}else if (numberValue != null) {
			sb.append(numberValue);
		}
		return sb.toString();
	}
}
