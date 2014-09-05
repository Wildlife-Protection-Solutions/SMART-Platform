/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.er.query.filter.summary;

import org.hibernate.Session;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.ui.dropitems.SurveyDropItemFactory;
import org.wcs.smart.query.model.filter.IValueVisitor;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.ui.model.DropItem;

/**
 * Total mission track length value item.
 * 
 * @author Emily
 *
 */
public class MissionLengthValueItem implements IValueItem {


	public static final MissionLengthValueItem createValueItem(){
		return new MissionLengthValueItem();
	}
	
	
	@Override
	public String asString() {
		return "s:missiontracklength"; //$NON-NLS-1$
	}

	@Override
	public String getName(Session session) {
		return Messages.MissionLengthValueItem_Label;
	}

	@Override
	public String getFullName(Session session) {
		return getName(session);
	}

	@Override
	public DropItem asDropItem(Session session) throws Exception {
		return SurveyDropItemFactory.INSTANCE.createMissionLengthValueItem();
	}

	@Override
	public Object getDropItemInitializeData() {
		return null;
	}

	@Override
	public void accept(IValueVisitor visitor) {
		visitor.visit(this);
	}

}
