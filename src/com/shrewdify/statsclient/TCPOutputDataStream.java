/*
 * TCPOutputDataStream.java
 *
 * Created on November 19, 2003, 9:51 AM
 */

package com.shrewdify.statsclient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import javax.media.rtp.OutputDataStream;
//import gov.nist.applet.phone.ua.PushToTalkStatus;
/**
 * This class is an implementation of the OutputDataStream for
 * The TCP transport protocol underlying to RTP. 
 * @author  DERUELLE Jean
 */
public class TCPOutputDataStream implements OutputDataStream{    
    private Socket socket;
    private OutputStream out;  
    private boolean ctrl;
    /** 
     * Creates a new instance of TCPOutputDataStream.
     * @param socket - the socket from which we will get the output stream.     
     */
    public TCPOutputDataStream(Socket socket,boolean ctrl) {
        this.socket = socket;        
        this.ctrl=ctrl;
        try{            
            out=socket.getOutputStream();
        }
        catch(IOException ioe){
            ioe.printStackTrace();
        }               
    }
    
    /**
     * Write some data in the output stream.
     * @param data - data to write.
     * @param offset - the start offset in the data.
     * @param len - the number of bytes to write.
     * @return number of bytes written. -1 if there is any exception caught
     */
    public int write(byte data[], int offset, int len) {
        try {
            //if(!PushToTalkStatus.pushToTalk)
                out.write(data, offset, len);
            /*else
                if(PushToTalkStatus.talking)
                    out.write(data, offset, len);
                else
                    if(!ctrl)
                        return 0;
                    else
                        out.write(data, offset, len);*/
            //out.flush();
        } catch (Exception e) {               
            return -1;
        }        
        return len;
    }
    
}
