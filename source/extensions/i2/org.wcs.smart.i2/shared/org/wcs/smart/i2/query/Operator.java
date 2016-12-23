package org.wcs.smart.i2.query;

import java.util.Locale;


public enum Operator {

	/**
	 * Equals (=) operator 
	 */
	EQUALS("="), //$NON-NLS-1$
	/**
	 * Less Than (<) operator 
	 */
	LESSTHAN("<"), //$NON-NLS-1$
	/**
	 * Less Than Equals (<=) operator 
	 */
	LESSTHANEQUALS("<="), //$NON-NLS-1$
	/**
	 * Great Than (>) operator 
	 */
	GREATERTHAN(">"), //$NON-NLS-1$
	/**
	 * Great Than Equals (>=) operator 
	 */
	GREATERTHANEQUALS(">="), //$NON-NLS-1$
	/**
	 * Not Equals (!=) operator 
	 */
	NOTEQUALS("!="), //$NON-NLS-1$
	/**
	 * String like operator 
	 */
	STR_EQUALS("equals"), //$NON-NLS-1$
	/**
	 * String contains operator 
	 */
	STR_CONTAINS("contains"), //$NON-NLS-1$
	/**
	 * String not contains operator
	 */
	STR_NOTCONTAINS("notcontains"), //$NON-NLS-1$
	
	/**
	 * The between operator
	 */
	BETWEEN("between"), //$NON-NLS-1$
	
	/**
	 * The not between operator
	 */
	NOT_BETWEEN("not between"), //$NON-NLS-1$
	
	/**
	 * Boolean and operator
	 */
	AND("and"), //$NON-NLS-1$
	/**
	 * Boolean or operator
	 */
	OR("or"), //$NON-NLS-1$
	/**
	 * Boolean not operator
	 */
	NOT("not"), //$NON-NLS-1$

	/**
	 * Brackets items 
	 */
	BRACKETS("()"), //$NON-NLS-1$
	
	/**
	 * Open Bracket
	 */
	BRACKET_OPEN("("), //$NON-NLS-1$ 
	
	/**
	 * Close bracket
	 */
	BRACKET_CLOSE(")"); //$NON-NLS-1$ 
	/**
	 * Valid numeric operators
	 */
	public static Operator[] NUMERIC_OPS = {EQUALS, LESSTHAN, LESSTHANEQUALS, GREATERTHAN, GREATERTHANEQUALS, NOTEQUALS};
	/**
	 * Valid string operators
	 */
	public static Operator[] STRING_OPS = {STR_EQUALS, STR_CONTAINS, STR_NOTCONTAINS};
	/**
	 * Valid date operators
	 */
	public static Operator[] DATE_OPS = {BETWEEN, NOT_BETWEEN};
	/**
	 * Valid boolean operators
	 */
	public static Operator[] BOOLEANS_OPS = {AND, OR, NOT};
	
	private String key;
	
	private Operator(String key){
		this.key = key;
	}
	
	public String getLabel(Locale l){
		//TODO:
		return key;
	}
	
	public String getKey(){
		return this.key;
	}
}


