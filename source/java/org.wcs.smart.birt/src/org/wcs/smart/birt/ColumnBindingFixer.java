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
package org.wcs.smart.birt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.report.designer.data.ui.dataset.DataSetUIUtil;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.birt.report.model.api.PropertyHandle;
import org.eclipse.birt.report.model.api.StructureFactory;
import org.eclipse.birt.report.model.api.elements.structures.CachedMetaData;
import org.eclipse.birt.report.model.api.elements.structures.ColumnHint;
import org.eclipse.birt.report.model.api.elements.structures.OdaResultSetColumn;
import org.eclipse.birt.report.model.api.elements.structures.ResultSetColumn;
import org.eclipse.birt.report.model.elements.interfaces.IDataSetModel;
import org.eclipse.swt.widgets.Display;

public class ColumnBindingFixer {

	@SuppressWarnings("unchecked")
	public static void fixBindings(final DataSetHandle dataset) throws Exception{
		// refresh the columns in the query
		//not running in display thread causes invalid thread access
		final Exception[] errors = new Exception[]{null};
		runInDisplayThread(new Callable(){
			@Override
			public void execute() {
				try{
					if ( dataset.getCachedMetaDataHandle( ) != null && dataset.getCachedMetaDataHandle( ).getResultSet( ) != null )
						dataset.getCachedMetaDataHandle( ).getResultSet( ).clearValue( );
					
					if ( dataset instanceof OdaDataSetHandle && dataset.getPropertyHandle( OdaDataSetHandle.RESULT_SET_PROP ).isLocal( ) )
							dataset.getPropertyHandle( OdaDataSetHandle.RESULT_SET_PROP ).setValue( new ArrayList( ) );
					
					DataSetUIUtil.updateColumnCache(dataset, false);
				}catch (Throwable ex){
					errors[0] = new Exception(ex);	
				}
			}	
		});
			
		if (errors[0] != null) throw errors[0];
		
		// update the column hints to include any new columns (in
		// particular the geometry columns); and remove any old
		//columns
		List<ResultSetColumn> metadata = (List<ResultSetColumn>) ((CachedMetaData) dataset
				.getProperty(IDataSetModel.CACHED_METADATA_PROP))
				.getProperty(dataset.getModule(), IDataSetModel.RESULT_SET_PROP);
		
		ArrayList<OdaResultSetColumn> resultset = (ArrayList<OdaResultSetColumn>) dataset.getProperty(IDataSetModel.RESULT_SET_PROP);
		final PropertyHandle columnHint = dataset.getPropertyHandle(IDataSetModel.COLUMN_HINTS_PROP);
		ArrayList<ColumnHint> hints = (ArrayList<ColumnHint>) dataset.getProperty(IDataSetModel.COLUMN_HINTS_PROP);
			
		ArrayList<ColumnHint> newHints = new ArrayList<ColumnHint>();
		for (OdaResultSetColumn c : resultset) {
			boolean found = false;
			if (hints != null){
				for (ColumnHint h : hints) {
					if (((String) h.getProperty(dataset.getModule(),ColumnHint.COLUMN_NAME_MEMBER)).equalsIgnoreCase(c.getColumnName())) {
						found = true;
						newHints.add(h);
						break;
					}
				}
			}
			if (!found) {
				// create a new column hint
				ColumnHint newHint = StructureFactory.createColumnHint();
				// metadata.get
				String name = c.getColumnName();
				for (ResultSetColumn cc : metadata) {
					if (cc.getPosition().equals(c.getPosition())) {
						name = cc.getColumnName();
						break;
					}
				}
				newHint.setProperty(ColumnHint.COLUMN_NAME_MEMBER,c.getColumnName());
				newHint.setProperty(ColumnHint.ALIAS_MEMBER, name);
				newHint.setProperty(ColumnHint.DISPLAY_NAME_MEMBER,name);
				newHint.setProperty(ColumnHint.HEADING_MEMBER, name);
				newHints.add(newHint);
			}
		}
		
		List<String> aliases = new ArrayList<String>();
			runInDisplayThread(new Callable(){
			@Override
			public void execute() {
				//not running in display thread causes invalid thread access
				try{
					columnHint.clearValue();
				}catch (Exception ex){
					errors[0] = ex;
				}
			}	
		});
		if (errors[0] != null) throw errors[0];
			
		for (final ColumnHint h : newHints){
			String alias = (String)h.getProperty(dataset.getModule(), ColumnHint.ALIAS_MEMBER);
			if (alias != null){
				//ensure duplicate aliases are not used
				if (aliases.contains(alias)){
					String root = alias;
					int cnt = 1;
					while(aliases.contains(alias)){
						alias = root + "_" + (cnt++); //$NON-NLS-1$
					}
					h.setProperty(ColumnHint.ALIAS_MEMBER, alias);
				}
				aliases.add(alias);
			}
			runInDisplayThread(new Callable(){
				@Override
				public void execute() {
					//not running in display thread causes invalid thread access
					try{
						columnHint.addItem(h);
					}catch (Exception ex){
						errors[0] = ex;
					}
				}	
			});
			if (errors[0] != null) throw errors[0];
		}

	}
	
	private static void runInDisplayThread(final Callable c) throws Exception{
		final Exception[] ex = {null};
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				try {
					c.execute();
				} catch (Exception e) {
					ex[0] = e;
				}		
			}
		});
		if (ex[0] != null) throw ex[0];
	}
	
	private interface Callable{
		public void execute();
	}
}
