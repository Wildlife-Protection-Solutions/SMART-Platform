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
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.dataqueue.DataQueueAction;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.security.AdminAccountAction;
import org.wcs.smart.connect.security.AlertAction;
import org.wcs.smart.connect.security.CaAdminAccountAction;
import org.wcs.smart.connect.security.SecurityManager;

/**
 * Web filter to provide the menu items for each page in the web app.
 */
@WebFilter(filterName="userinfofolder",urlPatterns={ConnectRESTApplication.SERVLET_PATH + "*"})
public class MenuItemsFilter implements Filter {

	private enum Page{
		HOME("MenuItemsFilter.HomePageLabel", ConnectRESTApplication.SERVLET_PATH + "home", null, "house.png"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		DASHBOARDBETA("MenuItemsFilter.DashboardBetaLabel", ConnectRESTApplication.SERVLET_PATH + "dashboardbeta", null, "dashboard.png"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		ACCOUNT("MenuItemsFilter.MyAccountLabel", ConnectRESTApplication.SERVLET_PATH + "myaccount", null, "myaccount.png"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		CA("MenuItemsFilter.CaLabel", ConnectRESTApplication.SERVLET_PATH + "ca", null, "calist.png"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		ALERT("MenuItemsFilter.AlertLabel", ConnectRESTApplication.SERVLET_PATH + "alert", AlertAction.VIEW_ALERTS_KEY, "alert.png"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QUERY("MenuItemsFilter.QueryLabel", ConnectRESTApplication.SERVLET_PATH + "query", null, "query.png"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		REPORT("MenuItemsFilter.ReportLabel", ConnectRESTApplication.SERVLET_PATH + "report", null, "reports.png"),  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		SHAREDLINKS("MenuItemsFilter.SharedLinksLabel", ConnectRESTApplication.SERVLET_PATH + "sharedlinksadmin", CaAdminAccountAction.KEY, "share.png"),  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		SHAREDLINKSADMIN("MenuItemsFilter.SharedLinksLabel", ConnectRESTApplication.SERVLET_PATH + "sharedlinksadmin", AdminAccountAction.KEY, "share.png"),  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		QUEUE("MenuItemsFilter.DataQueueLabel", ConnectRESTApplication.SERVLET_PATH + "dataqueue", DataQueueAction.VIEW_KEY, "dataq.png"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		USERS("MenuItemsFilter.AccountsLabel", ConnectRESTApplication.SERVLET_PATH + "users", AdminAccountAction.KEY, "users.png"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		CAUSERS("MenuItemsFilter.CaAccountsLabel", ConnectRESTApplication.SERVLET_PATH + "causers", CaAdminAccountAction.KEY, "users.png"), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		SETTINGS("MenuItemsFilter.ConfigurationLabel", ConnectRESTApplication.SERVLET_PATH +"settings", AdminAccountAction.KEY, "settings.png"),   //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		CT("Cybertracker", ConnectRESTApplication.SERVLET_PATH + "cybertracker", AdminAccountAction.KEY, "cybertracker.png");
		//TODO: ct = admin or caadmin
		String nameKey;
		String url;
		String actionKey;
		String imageFileName;
		
		Page(String nameKey, String url, String actionKey, String imageFileName){
			this.nameKey = nameKey;
			this.url = url;
			this.actionKey = actionKey;
			this.imageFileName = imageFileName;
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
					if (!SecurityManager.INSTANCE.canAccessAtLeastOneResouce(s, ((HttpServletRequest)request).getUserPrincipal().getName(), p.actionKey)){
						//do not add this menu item; user does not have permission to access
						continue;
					}
				}
				;
				menuItems.add( new String[]{
					Messages.getString(p.nameKey, l),
					pathprefix + p.url,
					styleAll, request.getServletContext().getContextPath() + "/css/images/" + p.imageFileName}); //$NON-NLS-1$
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
