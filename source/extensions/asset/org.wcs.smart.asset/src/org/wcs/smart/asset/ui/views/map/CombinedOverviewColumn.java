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
package org.wcs.smart.asset.ui.views.map;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

import org.json.simple.JSONObject;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.map.engine.IExpression;
import org.wcs.smart.asset.map.engine.parser.Parser;

/**
 * Asset overview column that combines the values of two other columns
 * 
 * @author Emily
 *
 */
public class CombinedOverviewColumn implements IOverviewTableColumn{

	public static CombinedOverviewColumn ratio(String name, String key, IOverviewTableColumn column1, IOverviewTableColumn column2) {
		String formula = "[" + column1.getKey() + "] / [" + column2.getKey() + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		CombinedOverviewColumn c = new CombinedOverviewColumn(name, formula);
		c.key = key;
		return c;
	}
	
	private String name;
	private String definition;
	private String key;
	
	private IExpression expression;
	
	public CombinedOverviewColumn(String name, String formula) {
		this.name = name;
		this.definition = formula;
	}
	
	public void updateValues(String name, String definition) {
		this.name = name;
		this.definition = definition;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setKey(String key) {
		this.key = key;
	}
	
	public String getDefinition() {
		return this.definition;
	}
	
	/**
	 * Returns the parsed expression
	 * 
	 * @return
	 * @throws Exception
	 */
	public IExpression getParsedExpression() throws Exception{
		if (expression != null) return expression;		
		try(Reader is = new StringReader(definition)){
			Parser p = new Parser(is);
			expression = p.CombinedExpression();
		}
		return expression;
	}
	
	@Override
	public Object getValue(StationData data) {		
		return data.getColumnValue(this);
	}

	@Override
	public ColumnType getType() {
		return IOverviewTableColumn.ColumnType.NUMBER;
	}

	@Override
	public String getKey() {
		return key;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public JSONObject serialize() {
		JSONObject json = new JSONObject();
		json.put("type", "combined"); //$NON-NLS-1$ //$NON-NLS-2$
		json.put("name", getName()); //$NON-NLS-1$
		json.put("key", getKey()); //$NON-NLS-1$
		json.put("definition", definition); //$NON-NLS-1$
		return json;
	}
	
	/**
	 * Converts a json object to a new CombinedOverviewColumn object.
	 * 
	 * @param json
	 * @param columns the source columns
	 * @return
	 */
	public static CombinedOverviewColumn deserialize(JSONObject json) {
		if (json.containsKey("type") && json.get("type").equals("combined")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			CombinedOverviewColumn cc = new CombinedOverviewColumn((String)json.get("name"), (String)json.get("definition")); //$NON-NLS-1$ //$NON-NLS-2$
			cc.key = (String) json.get("key"); //$NON-NLS-1$
			return cc;
		}
		return null;
	}
	
	/**
	 * 
	 * @param columns
	 * @return the default combined columns
	 */
	public static List<CombinedOverviewColumn> getDefaultColumns(List<IOverviewTableColumn> columns){
		IOverviewTableColumn c1 = null;
		IOverviewTableColumn c2 = null;
		
		String key1 = (new FixedColumn(FixedColumn.Column.INCIDENTS)).getKey();
		String key2 = (new FixedColumn(FixedColumn.Column.ASSET_DAYS)).getKey();
		
		for (IOverviewTableColumn c : columns) {
			if (c.getKey().equals(key1)) c1 = c;
			if (c.getKey().equals(key2)) c2 = c;
		}
		if (c1 == null || c2 == null) return Collections.emptyList();
		
		CombinedOverviewColumn i = CombinedOverviewColumn.ratio(Messages.CombinedOverviewColumn_IncidentsPerDayName, "incidentsperday", c1, c2); //$NON-NLS-1$
		return Collections.singletonList(i);
		
		
	}
}
