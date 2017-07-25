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


public class ListFeaturesServlet2 extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	
       

    public ListFeaturesServlet2() {
        super();

    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		
		PrintWriter pw = response.getWriter();
		response.setCharacterEncoding("utf-8");
		


	    RouteGuideClient client = new RouteGuideClient("127.0.0.1", 50052);
	    try{
		    try {
				
					//服务端Stream
					long startTimeSmove=System.currentTimeMillis();
					client.listFeatures(400000000, -750000000, 420000000, -730000000);
					long endTimeSmove=System.currentTimeMillis();
			        long durationSmove=endTimeSmove-startTimeSmove;
			        pw.print("Server-Stream Grpc Communication time:"+durationSmove);
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
