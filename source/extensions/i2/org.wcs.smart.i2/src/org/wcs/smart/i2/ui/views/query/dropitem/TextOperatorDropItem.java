package org.wcs.smart.i2.ui.views.query.dropitem;

import java.util.Locale;

import org.wcs.smart.i2.query.Operator;

public class TextOperatorDropItem extends TextDropItem {

	private Operator op;
	
	public TextOperatorDropItem(Operator op){
		super(op.getLabel(Locale.getDefault()), op.getKey());
		this.op = op;
	}

	public Operator getOperator(){
		return this.op;
	}
	
}
