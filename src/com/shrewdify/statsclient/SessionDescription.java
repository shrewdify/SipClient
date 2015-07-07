/*
 * SessionDescription.java
 *
 * Created on November 19, 2003, 10:38 AM
 */
package com.shrewdify.statsclient;

/**
 * A utility class to parse the session addresses.
 * @author DERUELLE Jean
 */
public class SessionDescription {

    private String address = null;
    private int destPort;
    private int localPort;
    private int timeToLive = 1;
    private String audioFormat=null;
    private String videoFormat=null;
    private String transportProtocol=null;
    
    /**
     * Constructor Parse the session given in parameter
     * and initializes the address, the port and the Time To Live
     * @param session - the session from which to parse the session
     */
    public SessionDescription (String session) {
      parseSessionDescription (session);
    }

    /**
     * Constructor      
     */
    public SessionDescription () {      
    }
    
    /**
     * Parse the session in a Session Label which contains the address,
     * the port and the time to live
     * @param session - the session to parse
     * @throws java.lang.IllegalArgumentException if the session is not valid
     */
    public void parseSessionDescription (String session) throws IllegalArgumentException {
        //If the session exists
        session=session.trim();
        if (session != null && session.length() > 0) {
          int endAddress = session.indexOf('/');
          //if there is no slash, the session is not valid
          if (endAddress == -1)
            throw new IllegalArgumentException();
          else {
            //Get the address
            this.address = session.substring(0, endAddress);			
            int destEndPort = session.indexOf('/', endAddress+1);
            //if there is no second slash, the session is invalid
            if (destEndPort != -1) {
              //Get the destination port
              try{
                this.destPort = Integer.parseInt(session.substring(endAddress+1,destEndPort));
              }
              catch(NumberFormatException nfe){
                System.out.println(session.substring(endAddress+1) +" is not a valid port");
                throw new IllegalArgumentException();
              }
                int localEndPort = session.indexOf('/', destEndPort+1);
                //if there is no third slash, we get the local port and let the
                //time to live by default
                if (localEndPort == -1) {
                        //Get the port
                        try{
                                this.localPort = Integer.parseInt(session.substring(destEndPort+1));
                        }
                        catch(NumberFormatException nfe){
                                System.out.println(session.substring(destEndPort+1) +" is not a valid port");
                                throw new IllegalArgumentException();
                        }
                }
    			//if there is a third slash
                else {
                  try{
                        //Get the port
                        this.localPort = Integer.parseInt(session.substring(destEndPort+1, localEndPort));
                  }
                  catch(NumberFormatException nfe){
                        System.out.println(session.substring(destEndPort+1, localEndPort) +" is not a valid port");
                        throw new IllegalArgumentException();
                  }
                  try{
                        //Get the Time To Live
                        this.timeToLive = Integer.parseInt(session.substring(localEndPort + 1));
                  }
                  catch(NumberFormatException nfe){
                        System.out.println(session.substring(localEndPort + 1) +" is not a valid Time To Live");
                        throw new IllegalArgumentException();
                  }
                }
            }
			else{
                throw new IllegalArgumentException();
			}
          }
        }
        else
          throw new IllegalArgumentException();
    }

    /**
     * Return the address of the session
     * @return the address of the session
     */
    public String getAddress(){
      return address;
    }

    /**
     * Set the address of the session
     * @param address - the address of the session
     */
    public void setAddress(String address){
      this.address=address;
    }

    /**
     * Return the destination port of the session
     * @return the destination port of the session
     */
    public int getDestinationPort() {
      return destPort;
    }

    /**
     * Set the destination port of the session
     * @param port - the destination port of the session
     */
    public void setDestinationPort(int port) {
      this.destPort = port;
    }

    /**
     * Return the local port of the session
     * @return the local port of the session
     */
    public int getLocalPort() {
      return localPort;
    }

    /**
     * Set the local port of the session
     * @param port - the local port of the session
     */
    public void setLocalPort(int port) {
      this.localPort = port;
    }


    /**
     * Return the Time To Live of the session
     * @return the Time To Live of the session
     */
    public int getTimeToLive(){
      return timeToLive;
    }

    /**
     * Set the Time To Live of the session
     * @param timeToLive - the Time To Live of the session
     */
    public void setTimeToLive (int timeToLive ){
      this.timeToLive=timeToLive;
    }
    
    /**
     * Retrieve the Audio Format of the session
     * @return the Audio Format of the session
     */
    public String getAudioFormat (){
      return audioFormat;
    }

    /**
     * Set the Audio Format of the session
     * @param audioFormat - the Audio Format of the session
     */
    public void setAudioFormat (String audioFormat ){
      this.audioFormat=MediaManager.findCorrespondingJmfFormat(audioFormat);
    }
    
    /**
     * Retrieve the Video Format of the session
     * @return the Video Format of the session
     */
    public String getVideoFormat (){
      return videoFormat;
    }

    /**
     * Set the Video Format of the session
     * @param videoFormat - video Format the of the session
     */
    public void setVideoFormat(String videoFormat ){
      this.videoFormat=MediaManager.findCorrespondingJmfFormat(videoFormat);
    }
    
    /**
     * Retrieve the transport protocol underlying to RTP of the session
     * @return the transport protocol underlying to RTP of the session
     */
    public String getTransportProtocol(){
      return transportProtocol;
    }

    /**
     * Set the transport protocol underlying to RTP of the session
     * @param transportProtocol - the transport protocol underlying to RTP of the session
     */
    public void setTransportProtocol(String transportProtocol){
      this.transportProtocol=transportProtocol;
    }        
}
