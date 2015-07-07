/*
 * TCPSendAdapter.java
 *
 * Created on November 19, 2003, 11:55 AM
 */

package com.shrewdify.statsclient;

import java.io.IOException;
import java.net.Socket;

import javax.media.protocol.PushSourceStream;
import javax.media.rtp.RTPConnector;
import javax.media.rtp.OutputDataStream;
/**
 * An implementation of RTPConnector based on TCP sockets.
 * This implementation can only send RTP data.
 * @author  DERUELLE Jean
 */
public class TCPSendAdapter implements RTPConnector {
    private Socket socket;
    private Socket ctrlSocket;    
       
    /*private InetAddress addr;
    private int localPort;    
    private int destinationPort;    
    private int rtcpLocalPort;
    private int rtcpDestPort;*/
    
    private TCPOutputDataStream dataOutStrm = null, ctrlOutStrm = null;
    private TCPPushSourceStream dataInStrm = null, ctrlInStrm = null;
        
    /** Creates a new instance of TCPSendAdapter 
     * @param addr - address of the remote host
     * @param localPort - port of the local host
     * @param destPort - port of the remote host
     */
    /*public TCPSendAdapter(InetAddress addr, int localPort, int destPort) throws IOException  {
        this(addr, localPort, destPort, 1);
    }*/
    
    /** Creates a new instance of TCPSendAdapter 
     * @param addr - address of the remote host
     * @param localPort - port of the local host
     * @param destPort - port of the remote host
     * @param ttl - time to live
     */
    /*public TCPSendAdapter(InetAddress addr, int localPort, int destPort, int ttl) throws IOException {       
        boolean connected=false;
        System.out.println("Trying to connect to "+addr+"/"+destPort);
        //Trying to connect to the TCP Port of the remote host to establish a RTP connection
        while(!connected){
            try{
                socket = new Socket(addr,destPort);
                System.out.println("Socket connected to "+addr+
                                    " on port "+destPort);
                connected=true;
            }
            catch(IOException ioe){

            }
        }
        connected=false;
        rtcpDestPort=destPort+1;
        rtcpLocalPort=localPort+1;
        System.out.println("Trying to connect to "+addr+"/"+rtcpDestPort);
        //Trying to connect to the TCP Port of the remote host to establish a RTCP connection
        while(!connected){
            try{
                ctrlSocket = new Socket(addr, rtcpDestPort);	    
                System.out.println("Control Socket connected to "+addr+
                                    " on port "+rtcpDestPort);
                connected=true;
            }
            catch(IOException ioe){

            }
        }        
	this.addr = addr;
	this.localPort = localPort;
        this.destinationPort = destPort;
    }*/
    
    /** Creates a new instance of TCPSendAdapter 
     * @param addr - address of the remote host
     * @param localPort - port of the local host
     */
    public TCPSendAdapter(Socket socketRTPTransmit,Socket socketRTCPTransmit) {       
        this.socket=socketRTPTransmit;
        this.ctrlSocket=socketRTCPTransmit;
    }
    
    /**
     * Returns an input stream to receive the RTP data.
     * @return input stream to receive the RTP data.
     */
    public PushSourceStream getDataInputStream() throws IOException {
        if (dataInStrm == null) {            
	    dataInStrm = new TCPPushSourceStream(socket,false);            
	    dataInStrm.start();
	}
	return dataInStrm;
    }

    /**
     * Returns an output stream to send the RTP data.
     * @return output stream to send the RTP data.
     */
    public OutputDataStream getDataOutputStream() throws IOException {
	if (dataOutStrm == null){            
	    dataOutStrm = new TCPOutputDataStream(socket,false);            
        }
	return dataOutStrm;
    }

    /**
     * Returns an input stream to receive the RTCP data.
     * @return input stream to receive the RTCP data.
     */
    public PushSourceStream getControlInputStream() throws IOException {	
        if (ctrlInStrm == null) {            
	    ctrlInStrm = new TCPPushSourceStream(ctrlSocket,true);            
	    ctrlInStrm.start();
	}
	return ctrlInStrm;
    }

    /**
     * Returns an output stream to send the RTCP data.
     * @return output stream to send the RTCP data.
     */
    public OutputDataStream getControlOutputStream() throws IOException {
	if (ctrlOutStrm == null){            
	    ctrlOutStrm = new TCPOutputDataStream(ctrlSocket,true);            
            
        }
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
        try{
            socket.close();
        }
        catch(IOException ioe){
            ioe.printStackTrace();
        }
        try{
            ctrlSocket.close();
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
	socket.setReceiveBufferSize(size);        
    }

    /**
     * Get the receive buffer size set on the RTP data channel.
     * Return -1 if the receive buffer size is not applicable for
     * the implementation.
     * @return receive buffer size 
     */
    public int getReceiveBufferSize() {
	try {
	    return socket.getReceiveBufferSize();            
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
	socket.setSendBufferSize(size);        
    }

    /**
     * Get the send buffer size set on the RTP data channel.
     * Return -1 if the send buffer size is not applicable for
     * the implementation.
     * @return send buffer size 
     */
    public int getSendBufferSize() {
	try {
	    return socket.getSendBufferSize();            
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
}
