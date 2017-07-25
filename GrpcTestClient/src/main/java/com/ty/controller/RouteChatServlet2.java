package com.ty.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.grpc.examples.routeguide.Feature;
import io.grpc.examples.routeguide.RouteGuideClient;
import io.grpc.examples.routeguide.RouteGuideUtil;


public class RouteChatServlet2 extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	
       

    public RouteChatServlet2() {
        super();

    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		
		PrintWriter pw = response.getWriter();
		response.setCharacterEncoding("utf-8");
		


	    RouteGuideClient client = new RouteGuideClient("127.0.0.1", 50052);
	    try{
		    try {
				
					//双向Stream
					long startTimeSmove=System.currentTimeMillis();
					CountDownLatch finishLatch = client.routeChat();
					if (!finishLatch.await(1, TimeUnit.MINUTES)) {
				        client.warning("routeChat can not finish within 1 minutes");
				      }
					long endTimeSmove=System.currentTimeMillis();
			        long durationSmove=endTimeSmove-startTimeSmove;
			        pw.println("Bi-directional Grpc Communication time:"+durationSmove);
			        pw.println("Jedis:");
			        pw.println("sadd");
			        pw.println("smembers");
			        pw.println("srem");
			        pw.println("sismember");
			        pw.println("scard");
			        pw.println("sinter");
			        pw.println("sdiff");
				
		    }finally {
			      client.shutdown();
		    }
	    }catch(InterruptedException e){
	    	e.printStackTrace();
	    }
		
	}

	
}
