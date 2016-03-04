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

import org.hibernate.Session;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.StyleConfiguration;

/**
 * Filter to load and set style options for
 * Web Server. 
 * 
 * @author Emily
 *
 */
@WebFilter(filterName="stylefilter", urlPatterns={ConnectRESTApplication.SERVLET_PATH + "*", "*.jsp", "/forgot", "/reset/*"})
public class StyleFilter implements Filter{

	@Override
	public void destroy() {
	}
	
	public void configureStyle(ServletRequest request, ServletResponse response){
		StyleConfiguration style;
		Session s = HibernateManager.getSession(request.getServletContext());
		s.beginTransaction();
		try{
			style = HibernateManager.getStyleConfiguration(s);
		}catch (Exception e){
			throw e;
		}finally{
			s.getTransaction().commit();
		}
		
		if (style != null){
			if (style.getServerName()  != null && !style.getServerName().isEmpty()){
				request.setAttribute("style_headername", style.getServerName()); //$NON-NLS-1$
			}
			String contextPath = request.getServletContext().getContextPath();
			if (style.getHeaderStyle()  != null && !style.getHeaderStyle().isEmpty()){
				request.setAttribute("style_headercss", style.getHeaderStyle().replaceAll("\\n\\r|\\n|\\r", "") + "; background-image:url('" + contextPath + "/getImage?locationId=1')"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			}else{
				request.setAttribute("style_headercss", "background-image:url('" + contextPath + "/getImage?locationId=1')"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			if (style.getBodyStyle() != null && !style.getBodyStyle().isEmpty()){
				request.setAttribute("style_bodycss", style.getBodyStyle().replaceAll("\\n\\r|\\n|\\r", "") + "; background-image:url('" + contextPath + "/getImage?locationId=2')"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
			}else{
				request.setAttribute("style_bodycss", "background-image:url('" + contextPath + "/getImage?locationId=2')"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
			if (style.getFooterText()  != null && !style.getFooterText().isEmpty()){
				request.setAttribute("style_footername", style.getFooterText()); //$NON-NLS-1$
			}
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
			FilterChain chain) throws IOException, ServletException {
		
		configureStyle(request, response);
		
		// pass the request along the filter chain
		chain.doFilter(request, response);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		
	}

}
