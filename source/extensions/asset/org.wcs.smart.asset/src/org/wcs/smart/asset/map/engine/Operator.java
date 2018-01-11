package org.wcs.smart.asset.map.engine;

public class Operator {

	enum Op{
		AND("AND", "AND"),
		OR("OR", "OR"),
		LG("<", "<"),
		GT(">", ">"),
		LGE("<=", "<="),
		GTE(">=", ">="),
		EQ("=", "="),
		NOTEQ("<>", "!="),
		STR_EQUAL("equals", "="),
		STR_CONTAINS("contains", "like"),
		BEFORE("before", "<"),
		AFTER("after", ">");
		
		String key;
		String sql;
		
		Op(String key, String sql) {
			this.key = key;
			this.sql = sql;
			
		}
		
	}
	
	Op operator;
	
	private Operator(Op operator) {
		this.operator = operator;
	}
	
	public static Operator parse(String key){
		for (Op o : Op.values()) {
			if (o.key.equalsIgnoreCase(key)) return new Operator(o);
		}
		return null;
	}
	
}
