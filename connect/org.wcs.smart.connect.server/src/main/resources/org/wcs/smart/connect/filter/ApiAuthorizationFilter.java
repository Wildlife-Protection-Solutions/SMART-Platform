package org.wcs.smart.connect.filter;

import java.io.IOException;
import java.net.HttpURLConnection;
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

import org.jboss.resteasy.util.Base64;


/**
 * Servlet Filter implementation class ApiAuthorization
 */
@WebFilter(filterName="api authorization files",urlPatterns={"/api/*"})
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
	 * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
	 */
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest r = (HttpServletRequest)request;
		HttpSession s = r.getSession(false);
	
		if (s == null || r.getRemoteUser() == null){
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
				((HttpServletResponse)response).setStatus(HttpURLConnection.HTTP_UNAUTHORIZED);
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
