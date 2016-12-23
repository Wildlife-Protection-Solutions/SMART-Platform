package org.wcs.smart.i2.ui.views.query;

import java.util.Locale;

import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextDropItem;

public class OperatorFilterItem extends BasicFilterItem{

	private Operator op;
	
	public OperatorFilterItem(Operator op){
		super(op.getLabel(Locale.getDefault()));
		this.op = op;
	}
	
	@Override
	public DropItem[] asDropItem() {
		if (op == Operator.BRACKETS){
			TextDropItem open = new TextDropItem(" ( ", "(");
			TextDropItem close = new TextDropItem(" ) ",")");
			return new DropItem[]{open, close};
		}
		return new DropItem[]{new TextDropItem(op.getLabel(Locale.getDefault()), op.getKey())};
	}
}
