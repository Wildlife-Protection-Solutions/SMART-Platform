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
package org.wcs.smart.intelligence.query;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.patrol.query.model.IExtensionOption;
import org.wcs.smart.patrol.query.model.PatrolDropItemFactory;
import org.wcs.smart.patrol.query.parser.IPatrolQueryOption;
import org.wcs.smart.query.ui.model.DropItem;

/**
 * Patrol intelligence extension option
 * @author Emily
 *
 */
public class PatrolIntelligenceOption implements IExtensionOption {

	private	IPatrolQueryOption option;
	
	public PatrolIntelligenceOption(IPatrolQueryOption option){
		this.option = option;
	}
	
	@Override
	public String getName() {
		return option.getGuiName();
	}

	@Override
	public DropItem asDropItem() {
		return PatrolDropItemFactory.INSTANCE.createPatrolFilterDropItem(option);
	}
	
	@Override
	public Image getImage() {
		return option.getImage();
	}
	
	public IPatrolQueryOption getOption(){
		return this.option;
	}

}
