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
package org.wcs.smart.event.filter;

import java.io.Reader;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.wcs.smart.SmartContext;
import org.wcs.smart.event.filter.parse.Parser;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.IWaypointSourceEngine;

/**
 * Parsed action fitler.
 * 
 * @author Emily
 *
 */
public class ParsedFilter {

	private static Logger logger = Logger.getLogger( ParsedFilter.class.getCanonicalName());

	public static final String SECTION_SPACER = "|"; //$NON-NLS-1$
	public static final String SOURCE_SPACER = ":"; //$NON-NLS-1$
	
	public static ParsedFilter parse(String filterString) throws Exception{
		List<IWaypointSource> sources = null;
		IFilter wpFilter = null;
		
		String[] parts = filterString.split("\\|"); //$NON-NLS-1$
		
		if (parts.length > 0 && !parts[0].trim().isEmpty()) {
			sources = new ArrayList<>();
			String[] srcs = parts[0].split(SOURCE_SPACER);
			for (String src : srcs) {
				IWaypointSource wpSource = SmartContext.INSTANCE.getClass(IWaypointSourceEngine.class).getSource(src);
				if (wpSource != null) {
					sources.add(wpSource);
				}else {
					//we don't need to throw an exception here
					//its still ok to continue; perhaps they uninstalled a module
					logger.log(Level.WARNING, MessageFormat.format("Waypoint source not found {0} for action filter.  Module likely uninstalled.", src)); //$NON-NLS-1$
				}
			}
		}
		
		if (parts.length > 1 && !parts[1].trim().isEmpty()) {
			try(Reader is = new StringReader(parts[1].trim())){
				wpFilter = (new Parser(is)).EventFilter();
			}catch (Throwable ex) {
				throw new Exception(ex);
			}
		}
		
		return new ParsedFilter(sources, wpFilter);
	}
	
	
	private List<IWaypointSource> sourceFilters;
	private IFilter waypointFilter;
	
	public ParsedFilter(List<IWaypointSource> sources, IFilter filter) {
		this.sourceFilters = sources;
		this.waypointFilter = filter;
	}
	
	/**
	 * Will return null if all sources should be included otherwise
	 * a list of sources that are valid
	 * @return
	 */
	public List<IWaypointSource> getSources(){
		return this.sourceFilters;
	}
	
	/**
	 * null if no filter (everything creates an event) otherwise the
	 * filter
	 * @return
	 */
	public IFilter getFilter() {
		return this.waypointFilter;
	}
}
