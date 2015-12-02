/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.apache;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Manager to get system configured environment variables.
 * 
 * @author Emily
 *
 */
public enum EnvironmentVariables {
	INSTANCE;
	
	public enum Variable{
		DATASTORE_LOCATION("filestorelocation"),
		NUM_BACK_THREADS("number_background_threads"),
		CLEANUP_TASK_INTERVAL("cleanup_task_interval_hours"),
		SYNC_DOWNLOAD_AVAILABLE("sync_download_hours_available"),
		WORK_HISTORY_ITEM_AVAILABLE("work_item_history_days_available"),
		CA_EXPORT_AVAILABLE("ca_export_days_available"),
		CHANGELOG_CLEAN_UP_DAYS("changelog_cleanup_days"),
		SPATIAL_REF_SYS_TABLE("spatial_ref_sys_table");
		
		public String key;
		
		private Variable(String key){
			this.key = key;
		}
	}
	
	private Context ctx;
	
	public Object getEnvironmentVariable(Variable v) throws NamingException{
		return getContext().lookup(v.key);
	}
	
	private Context getContext() throws NamingException{
		if (ctx == null){
			ctx = (Context)(new InitialContext()).lookup("java:comp/env");
		}
		return ctx;
	}
}
