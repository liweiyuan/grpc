package io.grpc.examples.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TestDAO {
	Connection con = null;
	String url = "jdbc:mysql://192.168.2.129:3306/javatest?characterEncoding=utf-8";
	String username = "root";
	String password = "tingyun2o13";
	String sqld = "delete from testUser_zh where userName='test'";
	String sqli = "insert into testUser_zh (userName,password) values ('test','test')"; 
	
	

	public TestDAO() {
		try {
			Class.forName("com.mysql.jdbc.Driver");
			con = DriverManager.getConnection(url,username,password);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
	public void testMysql(){
		
		PreparedStatement pst;
		List<String> list = new ArrayList<String>();
		try {
			pst = con.prepareStatement(sqld);

			pst.executeUpdate();

			pst = con.prepareStatement(sqli);
			pst.executeUpdate();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	
	}
}
