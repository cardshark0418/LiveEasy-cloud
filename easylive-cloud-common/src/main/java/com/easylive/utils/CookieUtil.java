package com.easylive.utils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CookieUtil {
    public static void setToken2Cookie(HttpServletResponse response,String token){
        Cookie cookie = new Cookie("token",token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(3600*24*7);
        response.addCookie(cookie);
    }
    public static void adminSetToken2Cookie(HttpServletResponse response,String token){
        Cookie cookie = new Cookie("adminToken",token);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(3600*24*7);
        response.addCookie(cookie);
    }

    public static String getCookieToken(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        if(cookies!=null){
            for (Cookie cookie : cookies) {
                if(cookie.getName().equals("token")){
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    public static String adminGetCookieToken(HttpServletRequest request){
        Cookie[] cookies = request.getCookies();
        if(cookies!=null){
            for (Cookie cookie : cookies) {
                if(cookie.getName().equals("adminToken")){
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
