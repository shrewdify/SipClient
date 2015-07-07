/*
 * TCPPushSourceStream.java
 *
 * Created on November 19, 2003, 9:51 AM
 */

package com.shrewdify.statsclient;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import javax.media.protocol.SourceTransferHandler;
import javax.media.protocol.PushSourceStream;
import javax.media.protocol.ContentDescriptor;
//import gov.nist.applet.phone.ua.PushToTalkStatus;
/**
 * This class is an implementation of the PushSourceStream for
 * The TCP transport protocol underlying to RTP. 
 * @author  DERUELLE Jean
 */
public class TCPPushSourceStream extends Thread implements PushSourceStream{
    private Socket socket;    
    private InputStream in;    
    private boolean done = false;
    private boolean dataRead = false;    
    private SourceTransferHandler sth = null;    
    private boolean ctrl;
    /** 
     * Creates a new instance of TCPPushSourceStream
     * @param socket - the socket from which we will get the input stream.
     * @param ctrl - used to know if we are reading the rtcp packets     
     */
    public TCPPushSourceStream(Socket socket,boolean ctrl) {
        this.socket = socket;                     
        this.ctrl=ctrl;
        try{
            //if(ctrl)
                //in=socket.getInputStream();
            //else
                in=socket.getInputStream();
                //in=new MyInputStream(socket.getInputStream());
        }
        catch(IOException ioe){
            ioe.printStackTrace();
        }                
    }
    /**
     * Read some data from the input stream.
     * @param buffer - the buffer into which the data is read.
     * @param offset - the start offset in array buffer at which the data is written.
     * @param len - the maximum number of bytes to read.
     * @return the total number of bytes read into the buffer,
     * or -1 if there is no more data because the end of the stream has been reached.
     */
    public int read(byte buffer[], int offset, int length) {              
        int byteRead=0;        
        
        try 
        {
            //if(ctrl)
            //    byteRead=in.read(buffer,offset,length);
            //else{
                //if(!PushToTalkStatus.pushToTalk)
                    byteRead=in.read(buffer,offset,length);
                /*else{
                    if(PushToTalkStatus.talking)
                        if(!ctrl)
                            return 0;
                        else
                            byteRead=in.read(buffer,offset,length);
                    else
                        byteRead=in.read(buffer,offset,length);            
                }*/
            //}
        }
        catch (IOException e) {
            return -1;
        }
        synchronized (this) {
            dataRead = true;
            notify();
        }
        return byteRead;
    }
    /**
     * Start this thread
     */
    public synchronized void start() {
        super.start();
        if (sth != null) {
            dataRead = true;
            notify();
        }
    }
    /**
     * Kill this thread
     */
    public synchronized void kill() {
        done = true;
        notify();
    }
    /**
     * Determine the size of the buffer needed for the data transfer. 
     * This method is provided so that a transfer handler can determine how much data, 
     * at a minimum, will be available to transfer from the source. 
     * Overflow and data loss is likely to occur if this much data isn't read at transfer time.
     * @return The size of the data transfer.
     */
    public int getMinimumTransferSize() {
        return 2 * 1024;	// twice the MTU size, just to be safe.
    }
    
    /**
     * Register an object to service data transfers to this stream.
     *
     * If a handler is already registered when setTransferHandler is called, 
     * the handler is replaced; there can only be one handler at a time.
     * @param sth - The handler to transfer data to.
     */
    public synchronized void setTransferHandler(SourceTransferHandler sth) {
        this.sth = sth;
        dataRead = true;
        notify();
    }
    
    /**
     * Get the current content type for this stream.
     * @return The current ContentDescriptor for this stream.
     */
    // Not applicable.
    public ContentDescriptor getContentDescriptor() {
        return null;
    }
    /**
     * Get the size, in bytes, of the content on this stream. 
     * LENGTH_UNKNOWN is returned if the length is not known.
     * @return The content length in bytes.
     */
    // Not applicable.
    public long getContentLength() {
        return LENGTH_UNKNOWN;
    }
    /**
     * Find out if the end of the stream has been reached.
     * @return Returns true if there is no more data.
     */
    // Not applicable.
    public boolean endOfStream() {
        return false;
    }

    /**
     * Obtain the collection of objects that control the object that implements this interface.
     * If no controls are supported, a zero length array is returned.
     * @return the collection of object controls
     */
    // Not applicable.
    public Object[] getControls() {
        return new Object[0];
    }
    /**
     * Obtain the object that implements the specified Class or Interface  The full class or interface name must be used.
     * If the control is not supported then null is returned.
     * @return the object that implements the control, or null.
     */
    // Not applicable.
    public Object getControl(String type) {
        return null;
    }

    /**
     * Loop and notify the transfer handler of new data.
     */
    public void run() {
        while (!done) {
            synchronized (this) {
                while (!dataRead && !done) {
                    try {
                        wait();
                    } catch (InterruptedException e) { }
                }
                dataRead = false;
            }
            if (sth != null && !done) {
                sth.transferData(this);
            }
        }
    }    
    
    class MyInputStream  extends InputStream {        
        private int ptr;
        private int maxBufferRead;
        private  byte[] buffer;
        private  byte[] sockBuffer;
        private boolean bufferFull;
        private InputStream in;
        private static final int MAX_BUFFER_SIZE=131072;
        
        public MyInputStream (InputStream in) {
            super();
            ptr =0;
            maxBufferRead=0;
            bufferFull=false;
            buffer=new byte[MAX_BUFFER_SIZE];
            sockBuffer=new byte[MAX_BUFFER_SIZE];
            this.in = in;
        }


        public int read() throws IOException {    
            //if(!PushToTalkStatus.pushToTalk){
                byte content[]=new byte[1];
                int nbBytesRead=in.read(content);                
                if(nbBytesRead==-1)
                    return -1;
                else
                    return (int)content[0];
            /*}
            else{                                
                while(!bufferFull){
                    int nbBytesRead=in.read(sockBuffer);   
                    //System.out.println("nbBytesRead: "+nbBytesRead+" ctrl"+ctrl);
                    if(nbBytesRead==-1)
                        return -1;            
                    if(nbBytesRead<MAX_BUFFER_SIZE){
                        //System.out.println("ptr+nbBytesRead: "+ptr+" ctrl"+ctrl);
                        if(ptr+nbBytesRead<MAX_BUFFER_SIZE){
                            System.arraycopy(sockBuffer, 0, buffer, ptr, nbBytesRead);
                            ptr+=nbBytesRead;                   
                        }
                        else{                            
                            maxBufferRead=ptr;
                            ptr=0;
                            System.out.println("buffer fulled: size read: "+maxBufferRead);                    
                            System.out.println("ptr on : "+ptr);
                            bufferFull=true;
                        }
                    }
                }
                //We read the buffer                
                int byteVal= (int) buffer[ptr++];
                System.out.println("ptr on : "+ptr);
                //The buffer has been read we have to fill it up again
                if(ptr>=maxBufferRead){
                    ptr=0;
                    bufferFull=false;
                    maxBufferRead=0;
                    System.out.println("buffer emptied");
                }
                return byteVal;
            }*/
        }

    }
    
}
