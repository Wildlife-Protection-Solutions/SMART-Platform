package org.wcs.smart.connect.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.connect.security.UserAccountsAction;

/**
 * Applys session user info to each page
 */
@WebFilter(filterName="userinfofolder",urlPatterns={"/connect/*"})
public class MenuItemsFilter implements Filter {

	private enum Page{
		HOME("MenuItemsFilter.HomePageLabel", "/connect/home", null), //$NON-NLS-1$ //$NON-NLS-2$
		ACCOUNT("MenuItemsFilter.MyAccountLabel", "/connect/myaccount", null), //$NON-NLS-1$ //$NON-NLS-2$
		CA("MenuItemsFilter.CaLabel", "/connect/ca", null), //$NON-NLS-1$ //$NON-NLS-2$
		QUEUE("MenuItemsFilter.DataQueueLabel", "/connect/dataqueue", null), //$NON-NLS-1$ //$NON-NLS-2$
		ALERT("MenuItemsFilter.AlertLabel", "/connect/alert", null), //$NON-NLS-1$ //$NON-NLS-2$
		USERS("MenuItemsFilter.AccountsLabel", "/connect/users", UserAccountsAction.KEY), //$NON-NLS-1$ //$NON-NLS-2$
		QUERY("MenuItemsFilter.QueryFilter", "/connect/query", null); //$NON-NLS-1$ //$NON-NLS-2$
		
		String nameKey;
		String url;
		String actionKey;
		
		Page(String nameKey, String url, String actionKey){
			this.nameKey = nameKey;
			this.url = url;
			this.actionKey = actionKey;
		}
	}
    /**
     * Default constructor. 
     */
    public MenuItemsFilter() {
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
		String styleAll = "menuitem"; //$NON-NLS-1$
		String pathprefix = ((HttpServletRequest)request).getContextPath();
		Locale l = SmartUtils.getRequestLocale(request);
		
		List<String[]> menuItems = new ArrayList<String[]>();
		Session s = HibernateManager.getSession(request.getServletContext());
		s.beginTransaction();
		try{
			for (Page p : Page.values()){
				if (p.actionKey != null){
					if (!SecurityManager.INSTANCE.canAccess(s, ((HttpServletRequest)request).getUserPrincipal().getName(), p.actionKey)){
						//do not add this menu item; user does not have permission to access
						continue;
					}
				}
				menuItems.add( new String[]{
					Messages.getString(p.nameKey, l),
					pathprefix + p.url,
					styleAll});
		}
		}finally{
			s.getTransaction().commit();
		}
		
		String[][] pages = menuItems.toArray(new String[menuItems.size()][3]); 
		String path = ((HttpServletRequest)request).getRequestURI();
		if (path != null){
			for (int i = 0; i < pages.length; i ++){
				if (pages[i][1].endsWith(path)){
					pages[i][2] += " menuitem-active"; //$NON-NLS-1$
				}
			}
		}
			
		request.setAttribute("menuitems", pages); //$NON-NLS-1$
				
		// pass the request along the filter chain
		chain.doFilter(request, response);
	}

	/**
	 * @see Filter#init(FilterConfig)
	 */
	public void init(FilterConfig fConfig) throws ServletException {
	}

}
