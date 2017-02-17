package org.wcs.smart.connect.servlet;


import java.security.Principal;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
 
/**
 * An extension for the HTTPServletRequest that overrides the getUserPrincipal() 
 *  We supply these implementations here, where they are not normally populated unless we are going through
 *  the facility provided by the container.
 * <p>If he user or roles are null on this wrapper, the parent request is consulted to try to fetch what ever the container has set for us.

 *
 */
public class UserRoleRequestWrapper extends HttpServletRequestWrapper {
 
  String user;
  HttpServletRequest realRequest;
   
  public UserRoleRequestWrapper(String user, HttpServletRequest request) {
    super(request);
    this.user = user;
    this.realRequest = request;
  }
 
 
  @Override
  public Principal getUserPrincipal() {
    if (this.user == null) {
      return realRequest.getUserPrincipal();
    }
     
    // make an anonymous implementation to just return our user
    return new Principal() {
      @Override
      public String getName() {     
        return user;
      }
    };
  }
}

