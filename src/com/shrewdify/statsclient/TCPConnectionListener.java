/*
 * TCPConnectionListener.java
 *
 * Created on November 19, 2003, 10:38 AM
 */

package com.shrewdify.statsclient;

import java.net.ServerSocket;
import java.net.Socket;
/**
 * This class is a thread waiting for connection on this TCP port and ip address
 * As soon as it gets a connection it notifies the Adapter for RTP.
 * @author  DERUELLE Jean
 */
public class TCPConnectionListener implements Runnable {
    private ServerSocket serverSocket;
    private Socket socket;
    //private TCPReceiveAdapter adapter;
    private Thread listener;
    private boolean ctrl;
    private boolean connected;
    
    /** Creates a new instance of TCPConnectionListener.
     * @param serverSocket - ServerSocket that will be waiting for connection.
     * @param adapter - the TCP Adapter for RTP we have to notify that we get a new connection.
     * @param ctrl - boolean to know if it's RTP connection (false) or a RTCP connection (true).
     */
    public TCPConnectionListener(ServerSocket serverSocket, boolean ctrl) {
        this.serverSocket=serverSocket;
        //this.adapter=adapter;
        this.ctrl=ctrl;
        connected=false;
    }
    
    /**
     * Start the listener thread.
     */
    public void start(){
        if(listener==null){
			listener=new Thread(this);
			listener.setName("TCPConnectionListener Thread");
        }
            
        listener.start();
    }
    
    /**
     * Task of the listener thread.
     */
    public void run() {
        try{
            socket=serverSocket.accept(); 
            System.out.println("Socket from "+ socket.getInetAddress() +
                                " connected to the port "+socket.getLocalPort()+
                                " control : "+ctrl);
            //adapter.setRemoteSocket(socket,ctrl);
            connected=true;
            listener=null;
        }
        catch(java.io.IOException ioe){
            ioe.printStackTrace();
        }
    }
    
    /**
     * Tells if this listener for TCP Connection has received a connection
     * @return the socket when the listener has received a new connection
     */
    public Socket waitForConnections(){
        while(!connected){
            try{
                Thread.sleep(1);
            }
            catch(InterruptedException ie){
                ie.printStackTrace();
            }
        }
        return socket;
    }
}
