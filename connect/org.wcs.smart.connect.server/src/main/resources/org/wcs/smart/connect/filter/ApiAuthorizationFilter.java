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
import java.net.HttpURLConnection;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hibernate.Session;
import org.jboss.resteasy.util.Base64;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.SharedLink;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.query.QueryManager;
import org.wcs.smart.connect.servlet.UserRoleRequestWrapper;


/**
 * Servlet filter to authorize all calls to the /api/*.  This just ensure the
 * current username/password is valid.  It does not ensure the user has
 * access to the request action/resource.
 *  
 */
@WebFilter(filterName="api authorization files",urlPatterns={ConnectRESTApplication.ALL_APP_PATH})
public class ApiAuthorizationFilter implements Filter {

	private final Logger logger = Logger.getLogger(ApiAuthorizationFilter.class.getName());
	
    /**
     * Default constructor. 
     */
    public ApiAuthorizationFilter() {
    }

	/**
	 * @see Filter#destroy()
	 */
	public void destroy() {
	}

	 /**
     * Checks if this is a X-domain pre-flight request.
     * @param request
     * @return
     */
    private boolean isPreflight(HttpServletRequest request) {
        return "OPTIONS".equals(request.getMethod()); //$NON-NLS-1$
    }
    
	/**
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest r = (HttpServletRequest)request;
		HttpSession s = r.getSession(false);

		if (isPreflight(r)){
			//to support CORS requests
			chain.doFilter(request, response);
			return;
		}
		String tokenString= request.getParameter("token"); //$NON-NLS-1$
		
		if(tokenString != null){
			UUID token = UUID.fromString(tokenString);
			Session session = HibernateManager.getSession(request.getServletContext());
			session.beginTransaction();
			SharedLink link;
			try{
				link = QueryManager.INSTANCE.findSharedLink(token, session);
				if(link == null){
					((HttpServletResponse)response).sendError(HttpServletResponse.SC_NOT_FOUND, Messages.getString("SharedLinkServlet.InvalidUuid", request.getLocale())); //$NON-NLS-1$
					return;
				}
				link.setOwnerUsername( ((SmartUser)session.get(SmartUser.class, link.getOwnerUuid())).getUsername() );
			}finally{
				session.getTransaction().commit();
			}
			
			if (link != null){
				//check if link is still valid
				java.util.Date today = new java.util.Date();
				Timestamp now = new Timestamp(today.getTime());
				if(link.getExpiresAt().before(now)){
					((HttpServletResponse)response).sendError(HttpServletResponse.SC_NOT_FOUND, Messages.getString("SharedLinkServlet.LinkExpired", request.getLocale())); //$NON-NLS-1$
					return;
				}else{
					String userip = request.getRemoteAddr();
					if(link.getAllowedIp() == null || link.getAllowedIp().isEmpty() || link.getAllowedIp().equals(userip)){
						//Fill out the username with the link creator. This allows the Query Api to re-check if this user still has access to the query, or if it was revoked, which should revoke all links that user created.
						
						UserRoleRequestWrapper wrappedRequest = new UserRoleRequestWrapper(link.getOwnerUsername(), r);
						
						// pass the wrapped request along the filter chain
						chain.doFilter(wrappedRequest, response);
						return;
					}
				}
			}			
		}
		
		if (s == null || r.getRemoteUser() == null ){ 
			//here we want to try basic authentication
			boolean isOk = false;
			logger.finer("Attempting basic authorization"); //$NON-NLS-1$
			String auth = r.getHeader("Authorization"); //$NON-NLS-1$
			
			if (auth != null){
				auth = auth.trim();
				String[] bits = auth.split("\\s+"); //$NON-NLS-1$
						
				if (bits[0].equalsIgnoreCase("BASIC")){ //$NON-NLS-1$
					String info = new String(Base64.decode(bits[1]));
					int colon = info.indexOf(':');
					if (colon >= 0){
						String user = info.substring(0,colon);
						String pass = info.substring(colon+1);
						try{
							r.login(user, pass);
							isOk = true;
						}catch (ServletException se){
							logger.info("Basic Authenticiation User/Password Failed"); //$NON-NLS-1$
						}					
					}
				}			
			}
			
			if (!isOk){
				logger.finer("Basic authentication not found or invalid"); //$NON-NLS-1$
				((HttpServletResponse)response).sendError(HttpURLConnection.HTTP_UNAUTHORIZED);
				return;
			}
		}else{
			logger.finer("Using cookie session info"); //$NON-NLS-1$
		}
		
		// pass the request along the filter chain
		chain.doFilter(request, response);
	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
	}

}
