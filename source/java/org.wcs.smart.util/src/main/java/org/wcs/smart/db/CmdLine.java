/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * 
 * @author Emily
 * @since 1.0.0
 */
public class CmdLine {

	

	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception{
		if (args.length < 3){
			System.err.println("Usage: <database> <user> <password> <file> ... <file>");
			return;
		}
		String database = args[0];
		String user = args[1];
		String password = args[2];
		
		Connection c = DatabaseCmdRunner.createConnection(database, user, password);
		for (int i = 3; i < args.length; i ++){
			File f = new File(args[i]);
			if (!f.exists()){
				System.err.println("File not found - skipping: " + f.getAbsolutePath());
			}else{
				System.out.println("Processing: " + f.getAbsolutePath());
				DatabaseCmdRunner.processFile(c, f);
			}
		}
		System.out.println("Done");
	}

}
