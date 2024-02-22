package org.wcs.smart.connect.servlet;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationAreaProperty;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.security.CaAction;
import org.wcs.smart.connect.security.SecurityManager;
import org.wcs.smart.util.UuidUtils;

@WebServlet(ConnectRESTApplication.SERVLET_PATH + "earthranger")
public class EarthRangerServlet extends HttpServlet{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	
		List<Object[]> data = new ArrayList<>();
		String homeurl = null;
		Session s = HibernateManager.getSession(request.getServletContext());
		try{
			s.beginTransaction();
			
			List<ConservationAreaProperty> cas = s.createQuery("FROM ConservationAreaProperty WHERE key = :key and value is not null", ConservationAreaProperty.class) //$NON-NLS-1$
					.setParameter("key", ConservationAreaProperty.EARTH_RANGER_URL) //$NON-NLS-1$
					.list();
			
			String username = request.getUserPrincipal().getName();
			SmartUser su = HibernateManager.getUser(s, username);
			
			UUID homeca = null;
			
			
			if (su != null){
				homeca = su.getHomeCaUuid();
			}
			
			for (ConservationAreaProperty prop : cas) {
				if (SecurityManager.INSTANCE.canAccess(s, username, CaAction.VIEWCA_KEY, prop.getConservationArea().getUuid())){
					boolean ishome = prop.getConservationArea().getUuid().equals(homeca);
					data.add(new Object[] {
							prop.getConservationArea().getName(), 
							ishome, 
							prop.getValue()});
					if (ishome) homeurl = prop.getValue();
				}
			}
			
			data.sort((a,b)-> Collator.getInstance().compare(a[0], b[0]));
		}finally{
			s.getTransaction().rollback();
		}
		
		if (homeurl == null && data.size() > 0) {
			//pick the first one as the home
			homeurl = (String) data.get(0)[2];
			data.get(0)[1] = true;
		}
		
		Object[][] caswither = data.toArray(new Object[data.size()][]);
		request.setAttribute("caswither", caswither); //$NON-NLS-1$
		request.setAttribute("homeurl", homeurl); //$NON-NLS-1$

		request.getRequestDispatcher("/WEB-INF/earthranger.jsp").forward(request, response); //$NON-NLS-1$
	}
}
