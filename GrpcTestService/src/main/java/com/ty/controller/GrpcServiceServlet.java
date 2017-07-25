package com.ty.controller;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.grpc.examples.routeguide.RouteGuideServer;
import io.grpc.examples.routeguide.RouteGuideServer2;


public class GrpcServiceServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    public GrpcServiceServlet() {
        super();
    }


	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String serverStr = request.getParameter("server");
		int i = Integer.parseInt(serverStr);
		PrintWriter pw =  response.getWriter();
		if(i==1){
			RouteGuideServer server = new RouteGuideServer(50051);
			String a = server.start();
			pw.println(a);
		}if(i==2){
			RouteGuideServer2 server2 = new RouteGuideServer2(50052);
			String b = server2.start();
			pw.println(b);
		}
	    
	   
	}

}
