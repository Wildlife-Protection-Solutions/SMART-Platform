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
package org.wcs.smart.plan.query;

import java.util.HashMap;

import org.hibernate.Session;
import org.wcs.smart.query.parser.IPatrolQueryOption;
import org.wcs.smart.query.parser.filter.EmptyFilter;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.DropItemFactory;
import org.wcs.smart.util.SmartUtils;

/**
 * "Patrol is a part of a plan" Query Filter
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolPlanQueryFilter extends EmptyFilter {

	private IPatrolQueryOption option;
	private Object value;

	public PatrolPlanQueryFilter(IPatrolQueryOption option, Object value) {
		this.option = option;
		this.value = value;
	}

	@Override
	public String asString() {
		return option.getKey();
	}

	@Override
	public String asSql(HashMap<Class<?>, String> tableMapping) {
		//TODO: need to check for all sub-plans as well
		String prefix = tableMapping.get(option.getPatrolAttributeClass());
		String v = SmartUtils.stripQuotes((String)value);
		String sql = "EXISTS (SELECT * FROM smart.patrol_plan pa2pl WHERE pa2pl.patrol_uuid = "+prefix+".uuid AND pa2pl.plan_uuid = x'"+v+"')"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		return sql;
	}

	@Override
	public DropItem[] getDropItems(Session session) throws Exception {
		DropItem it = DropItemFactory.INSTANCE.createPatrolFilterDropItem(option);
		return new DropItem[]{it};
	}

}
