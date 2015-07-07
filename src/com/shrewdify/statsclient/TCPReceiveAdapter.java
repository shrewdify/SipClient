/*
 * TCPReceiveAdapter.java
 *
 * Created on November 19, 2003, 11:55 AM
 */

package com.shrewdify.statsclient;

import java.io.IOException;
import java.net.Socket;

import javax.media.protocol.PushSourceStream;
import javax.media.rtp.OutputDataStream;
import javax.media.rtp.RTPConnector;
/**
 * An implementation of RTPConnector based on TCP sockets.
 * This implementation can only receive RTP data.
 * @author  DERUELLE Jean
 */
public class TCPReceiveAdapter  implements RTPConnector {
    private Socket remoteSocket;
    private Socket remoteCtrlSocket; 
    
    /*private ServerSocket serverSocket;
    private ServerSocket ctrlServerSocket;
    
    private InetAddress addr;
    private int localPort;            
    private int rtcpLocalPort;    */

    private TCPPushSourceStream dataInStrm = null, ctrlInStrm = null;
    private TCPOutputDataStream dataOutStrm = null, ctrlOutStrm = null;
    /** Creates a new instance of TCPReceiveAdapter 
     * @param addr - address of the remote host
     * @param localPort - port of the local host
     * @param destPort - port of the remote host
     */
    /*public TCPReceiveAdapter(InetAddress addr, int localPort, int destPort) throws IOException  {
        this(addr, localPort, destPort, 1);
    }*/
    
    /** Creates a new instance of TCPReceiveAdapter 
     * @param addr - address of the remote host
     * @param localPort - port of the local host
     * @param destPort - port of the remote host
     * @param ttl - time to live
     */
    /*public TCPReceiveAdapter(InetAddress addr, int localPort, int destPort, int ttl) throws IOException {
        try {	    
            //Start the serverSocket for the RTP 
            serverSocket = new ServerSocket(localPort);
        } catch(SocketException e) {
            System.out.println(addr+","+localPort+","+destPort);
	    throw new IOException(e.getMessage());
	}        
        System.out.println("TCP Listening Point created on port: "+localPort);
        TCPConnectionListener listener=new TCPConnectionListener(serverSocket, this,  false);
        listener.start();
        rtcpLocalPort=localPort+1;        
        try{
            //Start the serverSocket for the RTCP 
            ctrlServerSocket = new ServerSocket(rtcpLocalPort);
        } catch(SocketException e) {
            System.out.println(addr+","+rtcpLocalPort+","+destPort);
	    throw new IOException(e.getMessage());
	}        
        System.out.println("TCP Control Listening Point created on port: "+rtcpLocalPort);
        TCPConnectionListener ctrlListener=new TCPConnectionListener(ctrlServerSocket, this,  true);
        ctrlListener.start();
        //Wait for connections
        listener.waitForConnections();
        ctrlListener.waitForConnections(); 
	
	this.addr = addr;
	this.localPort = localPort;        
    }*/
    
    /** Creates a new instance of TCPReceiveAdapter 
     * @param socketRTP - socket for receiveing RTP media 
     * already connected to the remote host
     * @param socketRTCP - socket for receiveing RTCP media 
     * already connected to the remote host
     */
    public TCPReceiveAdapter(Socket socketRTP,Socket socketRTCP) {
        this.remoteSocket=socketRTP;
        this.remoteCtrlSocket=socketRTCP;
    }
    
    /**
     * Returns an input stream to receive the RTP data.
     * @return input stream to receive the RTP data.
     */
    public PushSourceStream getDataInputStream() throws IOException {
	if (dataInStrm == null) {
	    dataInStrm = new TCPPushSourceStream(remoteSocket,false);            
	    dataInStrm.start();
	}
	return dataInStrm;
    }

    /**
     * Returns an output stream to send the RTP data.
     * @return output stream to send the RTP data.
     */
    public OutputDataStream getDataOutputStream() throws IOException {
	if (dataOutStrm == null)
	    dataOutStrm = new TCPOutputDataStream(remoteSocket,false);            
	return dataOutStrm;
    }

    /**
     * Returns an input stream to receive the RTCP data.
     * @return input stream to receive the RTCP data.
     */
    public PushSourceStream getControlInputStream() throws IOException {
	if (ctrlInStrm == null) {
	    ctrlInStrm = new TCPPushSourceStream(remoteCtrlSocket,true);            
	    ctrlInStrm.start();
	}
	return ctrlInStrm;
    }

    /**
     * Returns an output stream to send the RTCP data.
     * @return output stream to send the RTCP data.
     */
    public OutputDataStream getControlOutputStream() throws IOException {	
	if (ctrlOutStrm == null)
	    ctrlOutStrm = new TCPOutputDataStream(remoteCtrlSocket,true);            
	return ctrlOutStrm;
    }

    /**
     * Close all the RTP, RTCP streams.
     */
    public void close() {
	if (dataInStrm != null)
	    dataInStrm.kill();
	if (ctrlInStrm != null)
	    ctrlInStrm.kill();
        /*try{
            serverSocket.close();
        }
        catch(IOException ioe){
            ioe.printStackTrace();
        }
        try{
            ctrlServerSocket.close();
        }
        catch(IOException ioe){
            ioe.printStackTrace();
        }*/
        try{
            remoteSocket.close();
        }
        catch(IOException ioe){
            ioe.printStackTrace();
        }
        try{
            remoteCtrlSocket.close();
        }
        catch(IOException ioe){
            ioe.printStackTrace();
        }
    }

    /**
     * Set the receive buffer size of the RTP data channel.
     * This is only a hint to the implementation.  The actual implementation
     * may not be able to do anything to this.
     * @param size - receive buffer size 
     */
    public void setReceiveBufferSize( int size) throws IOException {
	remoteSocket.setReceiveBufferSize(size);        
    }

    /**
     * Get the receive buffer size set on the RTP data channel.
     * Return -1 if the receive buffer size is not applicable for
     * the implementation.
     * @return receive buffer size 
     */
    public int getReceiveBufferSize() {
	try {
	    return remoteSocket.getReceiveBufferSize();            
	} catch (Exception e) {
	    return -1;
	}
    }

    /**
     * Set the send buffer size of the RTP data channel.
     * This is only a hint to the implementation.  The actual implementation
     * may not be able to do anything to this.
     * @param size - send buffer size.
     */
    public void setSendBufferSize( int size) throws IOException {
	remoteSocket.setSendBufferSize(size);        
    }

    /**
     * Get the send buffer size set on the RTP data channel.
     * Return -1 if the send buffer size is not applicable for
     * the implementation.
     * @return send buffer size 
     */
    public int getSendBufferSize() {
	try {
	    return remoteSocket.getSendBufferSize();            
	} catch (Exception e) {
	    return -1;
	}
    }

    /**
     * Return the RTCP bandwidth fraction.  This value is used to
     * initialize the RTPManager.  Check RTPManager for more detauls.
     * @return RTCP bandwidth fraction. -1 to use the default values.
     */
    public double getRTCPBandwidthFraction() {
	return -1;
    }

    /**
     * Return the RTCP sender bandwidth fraction.  This value is used to
     * initialize the RTPManager.  Check RTPManager for more detauls.
     * Return -1 to use the default values.
     * @return RTCP sender bandwidth fraction. -1 to use the default values.
     */
    public double getRTCPSenderBandwidthFraction() {
	return -1;
    }
    
    /**
     * Set the remote socket connected to this Adapter TCP Listening port 
     * @param socket - remote socket connected to this Adapter TCP Listening port 
     * @param ctrl - tells if this socket is for the RTP or RTCP
     */
    public void setRemoteSocket(Socket socket, boolean ctrl){
        if(ctrl)
            this.remoteCtrlSocket=socket;
        else
            this.remoteSocket=socket;
    }
    
}
