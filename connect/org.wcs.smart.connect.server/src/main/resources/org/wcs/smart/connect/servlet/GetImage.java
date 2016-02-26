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

package org.wcs.smart.connect.servlet;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.StyleConfiguration;

/**
 * servlet to serve images from the database
 * 
 * @author Jeff
 *
 */

@WebServlet("/getImage")
public class GetImage extends HttpServlet {

	/*
	 * @ parameter locationID
	 * value of this parameter picks the right styling image, 1=header, 2=background, 3-login image
	 * 
	 */
	
	private static final long serialVersionUID = 1L;
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String id = request.getParameter("locationId");
		
		StyleConfiguration style = null; 
		
		Session session = HibernateManager.getSession(request.getServletContext());
		session.beginTransaction();
		try{
			style = HibernateManager.getStyleConfiguration(session);
		}finally{
			session.getTransaction().rollback();
		}
		if(style == null ){
			response.reset();
			response.getOutputStream().flush();
			return;
		}
		
		byte[] img =null;
		if(id.equals("1")){//header image
			img = style.getHeaderImage();
		}else if(id.equals("2")){//background image
			img = style.getBackgroundImage();
		}else if(id.equals("3")){//login image
			img = style.getLoginImage();
		}
		
		
		
		
		response.reset();
		response.setContentType("image/jpg");
		
		InputStream in = new ByteArrayInputStream(img);
		BufferedImage bImageFromConvert = ImageIO.read(in);
		if(img == null || bImageFromConvert == null){
			response.reset();
			response.getOutputStream().flush();
			return;
		}
		ImageIO.write(bImageFromConvert, "jpg", response.getOutputStream());
		response.getOutputStream().flush();
	}
}