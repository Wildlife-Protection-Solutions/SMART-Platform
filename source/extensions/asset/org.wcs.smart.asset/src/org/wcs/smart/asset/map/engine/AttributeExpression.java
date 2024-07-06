/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.asset.map.engine;

/**
 * Attribute filter for asset overview map category column
 * 
 * @author Emily
 *
 */
public class AttributeExpression implements IExpression{

	public static final String UI_DATE_FORMAT = "YYYY-MM-DD"; //$NON-NLS-1$
	public static final String UI_TIME_FORMAT = "HH:MM:SS"; //$NON-NLS-1$
	public static final String JAVA_DATE_FORMAT = "yyyy-MM-dd"; //$NON-NLS-1$
	public static final String JAVA_TIME_FORMAT = "HH:mm:ss"; //$NON-NLS-1$
	
	public static AttributeExpression parse(String attributeKey, Operator op, String strValue) {
		return new AttributeExpression(attributeKey, op, strValue);
	}
	
	public static AttributeExpression parse(String attributeKey, Operator op, Double numberValue) {
		return new AttributeExpression(attributeKey, op, numberValue);
	}
	
	private String attributeKey;
	private Operator op;
	private String strValue;
	private Double numberValue;
	
	public AttributeExpression(String attributeKey, Operator op, String strValue) {
		this.attributeKey = attributeKey;
		this.op = op;
		this.strValue = strValue;
	}
	
	
	public AttributeExpression(String attributeKey, Operator op, Double numberValue) {
		this.attributeKey = attributeKey;
		this.op = op;
		this.numberValue = numberValue;
	}
	
	public String getAttributeKey() {
		return this.attributeKey;
	}
	
	public Operator getOperator() {
		return this.op;
	}
	
	public String getStringValue() {
		return this.strValue;
	}
	
	public Double getNumberValue() {
		return this.numberValue;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(attributeKey);
		sb.append(" "); //$NON-NLS-1$
		sb.append(op.operator.sql);
		sb.append(" " ); //$NON-NLS-1$
		if (strValue != null) {
			sb.append(strValue);
		}else if (numberValue != null) {
			sb.append(numberValue);
		}
		return sb.toString();
	}
	
	@Override
	public void accept(IExpressionVisitor visitor) {
		visitor.visit(this);
	}
}
