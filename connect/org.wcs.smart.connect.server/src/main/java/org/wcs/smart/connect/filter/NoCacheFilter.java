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
package org.wcs.smart.connect.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletResponse;

import org.wcs.smart.connect.api.ConnectRESTApplication;

/**
 * Turns of all filters
 * @author Emily
 *
 */
@WebFilter(filterName="nocachefilter",urlPatterns={ConnectRESTApplication.SERVLET_PATH + "*", ConnectRESTApplication.ALL_APP_PATH})
public class NoCacheFilter implements Filter {

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain chain) throws IOException, ServletException {
		 
		HttpServletResponse response = (HttpServletResponse) res;

	    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1. //$NON-NLS-1$ //$NON-NLS-2$
	    response.setHeader("Pragma", "no-cache"); // HTTP 1.0. //$NON-NLS-1$ //$NON-NLS-2$
	    response.setDateHeader("Expires", 0); // Proxies. //$NON-NLS-1$

	    chain.doFilter(req, res);
	}

	@Override
	public void init(FilterConfig conf) throws ServletException {

	}

}
