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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Session;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.StyleConfiguration;


/**
 * servlet to serve images from the database
 * 
 * @author Jeff
 *
 */

@WebServlet("/connect/getImage")
public class GetImage extends HttpServlet {

	private static final long serialVersionUID = 1L;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		StyleConfiguration style = null; 
		
		Session session = HibernateManager.getSession(request.getServletContext());
		session.beginTransaction();
		try{
			style = HibernateManager.getStyleConfiguration(session);
		}finally{
			session.getTransaction().rollback();
		}
		byte[] img = style.getBackgroundImage();
		

		response.reset();
		response.setContentType("image/jpg");
		
//		ByteArrayInputStream bais = new ByteArrayInputStream(img);
//		BufferedImage image = ImageIO.read(bais);
//		
//		ImageIO.write(image, "jpg", response.getOutputStream());
////		response.getOutputStream().write(image,0,image.);
//		response.getOutputStream().flush();  
		
		
		//TESTING ONLY------------------------
		byte[] imageInByte;
		BufferedImage originalImage = ImageIO.read(new File("C:/Myfiles/Connect_Website/header.jpg"));

		// convert BufferedImage to byte array
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(originalImage, "jpg", baos);
		baos.flush();
		imageInByte = baos.toByteArray();
		baos.close();
		//---------------------------
		
		
		InputStream in = new ByteArrayInputStream(img);
		BufferedImage bImageFromConvert = ImageIO.read(in);

		ImageIO.write(bImageFromConvert, "jpg", response.getOutputStream());
		response.getOutputStream().flush();

	}

}