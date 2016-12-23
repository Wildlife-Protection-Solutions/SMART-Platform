package org.wcs.smart.i2.ui.views.query;

import java.util.Locale;

import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextOperatorDropItem;

public class OperatorFilterItem extends BasicFilterItem{

	private Operator op;
	
	public OperatorFilterItem(Operator op){
		super(op.getLabel(Locale.getDefault()));
		this.op = op;
	}
	
	@Override
	public DropItem[] asDropItem() {
		if (op == Operator.BRACKETS){
			return new DropItem[]{ new TextOperatorDropItem(Operator.BRACKET_OPEN), new TextOperatorDropItem(Operator.BRACKET_CLOSE)};
		}
		return new DropItem[]{new TextOperatorDropItem(op)};
	}
}
