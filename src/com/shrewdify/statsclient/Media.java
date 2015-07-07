package com.shrewdify.statsclient;

import java.net.ServerSocket;
import java.net.Socket;

public class Media implements Runnable{

	int port;
	public Media(int port){
		this.port=port;
		
	}
	@Override
	public void run() {
		try{
			System.out.println("Started");
			ServerSocket sc=new ServerSocket(port);
			Socket s=sc.accept();
			System.out.println("Accepted");
		while(true){
			System.out.println(s.getInputStream().read());
		}
		}catch(Exception e){System.out.println("Error:"+e.getMessage());}
		
	}

}
