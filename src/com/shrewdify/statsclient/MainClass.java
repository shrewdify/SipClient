package com.shrewdify.statsclient;


public class MainClass {

	public static void main(String[] args) {
		new MainClass();
	}
	
	public MainClass() {
		try{
			SipLayer sl=new SipLayer();
			sl.init("NITIN", true);
			sl.sendInvite();
			
			
		}catch(Exception e){
			System.out.println(e.getMessage());
		}
	}

}
