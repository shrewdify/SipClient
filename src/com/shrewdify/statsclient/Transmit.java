/*
 * Transmit.java
 *
 * Created on November 19, 2003, 10:38 AM
 */
package com.shrewdify.statsclient;

import java.awt.Dimension;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Vector;

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.Codec;
import javax.media.Control;
import javax.media.Controller;
import javax.media.Format;
import javax.media.MediaLocator;
import javax.media.NoProcessorException;
import javax.media.Owned;
import javax.media.Player;
import javax.media.Processor;
import javax.media.control.QualityControl;
import javax.media.control.TrackControl;
import javax.media.format.AudioFormat;
import javax.media.format.VideoFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import javax.media.protocol.PushBufferDataSource;
import javax.media.protocol.PushBufferStream;
import javax.media.rtp.RTPManager;
import javax.media.rtp.SendStream;
import javax.media.rtp.SessionAddress;
import javax.media.rtp.rtcp.SourceDescription;

/**
 * Class to send RTP using the JMF RTP API.
 * @author DERUELLE Jean
 */
public class Transmit {
    private Processor processor = null;
    private RTPManager rtpMgrs[] = null;
    private DataSource dataOutput = null;
    private StateListener stateListener=null;
    private SessionDescription sessionDescription =null;
    private String session=null;
    private MediaLocator videoLocator=null;
    private MediaLocator audioLocator=null;
    private RTPManager rtpManager = null;
    private SessionAddress remoteAddress =null;
    private Receiver receiver=null;
    private Socket socketRTPTransmit=null;
    private Socket socketRTCPTransmit=null;
    /**
     * Constructor for Transmitter 
     * @param session - the concatened parameters of the session stored in a string
     */
    public Transmit(String session) throws IllegalArgumentException{
        this.session=session;
        stateListener=new StateListener();
        //the session Label containing the address, the port and the Time To Live
        try {
            //create a session label on the session given in argument
            // and parse the session address.
            sessionDescription =new SessionDescription(session);
            sessionDescription.setAudioFormat("mpegaudio/rtp, 44100.0 hz, 16-bit, mono");
            sessionDescription.setVideoFormat("h263/rtp");
            sessionDescription.setTransportProtocol("udp");
        } catch (IllegalArgumentException e) {
            System.err.println("Failed to parse the session address given: " + session);
            throw e;
        }
        if(!initialize()){
            System.out.println("Bad Session intialization");
            //System.exit(0);
        }
    }

    /**
     * Constructor for Transmitter 
     * @param session - the session Description containing the address, the port, the Time To Live
     * the video format, the audio format and the transport protocol
     */
    public Transmit(SessionDescription session,Receiver receiver) {
        this.receiver=receiver;
        this.sessionDescription=session;
        stateListener=new StateListener();

        if(!initialize()){
            System.out.println("Bad Session intialization");
            //System.exit(0);
        }
    }

    /**
     * get the devices for the capture and print their formats
     * @return true if all has been initialized correctly
     */
    protected boolean initialize() {
        CaptureDeviceInfo videoCDI=null;
        CaptureDeviceInfo audioCDI=null;
        Vector captureDevices=null;
        captureDevices= CaptureDeviceManager.getDeviceList(null);
        System.out.println("- number of capture devices: "+captureDevices.size() );
        CaptureDeviceInfo cdi=null;
        for (int i = 0; i < captureDevices.size(); i++) {
        cdi = (CaptureDeviceInfo) captureDevices.elementAt(i);
        //System.out.println("    - name of the capture device: "+cdi.getName() );
            Format[] formatArray=cdi.getFormats();
            for (int j = 0; j < formatArray.length; j++) {
                Format format=formatArray[j];
                if (format instanceof VideoFormat) {
                    //System.out.println("         - format accepted by this VIDEO device: "+
                    //format.toString().trim());
                    if (videoCDI ==null) {
                        videoCDI=cdi;
                    }
               }
               else if (format instanceof AudioFormat) {
                    //System.out.println("         - format accepted by this AUDIO device: "+
                    //format.toString().trim());
                    if (audioCDI == null) {
                        audioCDI=cdi;
                    }
               }
               //else
                   //System.out.println("         - format of type UNKNOWN");
            }
        }
        if(videoCDI!=null)
            videoLocator=videoCDI.getLocator();
        if(audioCDI!=null)
            audioLocator=audioCDI.getLocator();

        return true;
    }

    /**
     * Starts the transmission. Returns null if transmission started ok.
     * Otherwise it returns a string with the reason why the setup failed.
     */
    public synchronized String start(String localIpAddress) {
        String result;

        // Create a processor for the specified media locator
        // and program it to output JPEG/RTP
        result = createProcessor();
        if (result != null)
            return result;

        // Create an RTP session to transmit the output of the
        // processor to the specified IP address and port no.
        result = createTransmitter(localIpAddress);
        if (result != null) {
            processor.close();
            processor = null;
            return result;
        }

        // Start the transmission
        processor.start();

        return null;
    }

    /**
     * Stops the transmission if already started
     */
    public void stop() {
        synchronized (this) {
            if (processor != null) {
                processor.stop();
                processor.close();
                processor = null;                
            }
            if(rtpMgrs!=null){            
				for (int i = 0; i < rtpMgrs.length; i++) {
					if(rtpMgrs[i]!=null){
						rtpMgrs[i].removeTargets( "Session ended.");
						rtpMgrs[i].dispose();
						rtpMgrs[i]=null;
					}				
				}
            }
        }
    }

    /**
     * Creates the processor
     */
    private String createProcessor() {
        DataSource audioDS=null;
        DataSource videoDS=null;
        DataSource mergeDS=null;
        StateListener stateListener=new StateListener();
        //create the DataSource
        //it can be a 'video' DataSource, an 'audio' DataSource
        //or a combination of audio and video by merging both
        if (videoLocator == null && audioLocator == null)
            return "Locator is null";
        if (audioLocator != null){
            try {	
                //create the 'audio' DataSource
                audioDS= javax.media.Manager.createDataSource(audioLocator);
            } catch (Exception e) {
                System.out.println("-> Couldn't connect to audio capture device");
            }
        }
        if (videoLocator != null){
            try {
                //create the 'video' DataSource
                videoDS = javax.media.Manager.createDataSource(videoLocator);				
            } catch (Exception e) {
                System.out.println("-> Couldn't connect to video capture device");
            }
        }
        if(videoDS!=null && audioDS!=null){
            try {
                //create the 'audio' and 'video' DataSource
                mergeDS = javax.media.Manager.createMergingDataSource(new DataSource [] {audioDS, videoDS});
            } catch (Exception e) {
                System.out.println("-> Couldn't connect to audio or video capture device");
            }
            try{
                //Create the processor from the merging DataSource
                processor = javax.media.Manager.createProcessor(mergeDS);
            }
            catch (NoProcessorException npe) {
                return "Couldn't create processor";
            } catch (IOException ioe) {
                return "IOException creating processor";
            } 
        }
        //if the processor has not been created from the merging DataSource
        if(processor==null){
            try {
                if(audioDS!=null)
                    //Create the processor from the 'audio' DataSource
                    processor = javax.media.Manager.createProcessor(audioDS);
                else
                    //Create the processor from the 'video' DataSource
                    processor = javax.media.Manager.createProcessor(videoDS);
            } catch (NoProcessorException npe) {
                return "Couldn't create processor";
            } catch (IOException ioe) {
                return "IOException creating processor";
            } 
        }

        // Wait for it to configure
        boolean result = stateListener.waitForState(processor, Processor.Configured);
        if (result == false)
            return "Couldn't configure processor";

        // Get the tracks from the processor
        TrackControl [] tracks = processor.getTrackControls();

        // Do we have atleast one track?
        if (tracks == null || tracks.length < 1)
            return "Couldn't find tracks in processor";

        // Set the output content descriptor to RAW_RTP
        // This will limit the supported formats reported from
        // Track.getSupportedFormats to only valid RTP formats.
        ContentDescriptor cd = new ContentDescriptor(ContentDescriptor.RAW_RTP);
        processor.setContentDescriptor(cd);

        Format supported[];
        Format chosen=null;
        boolean atLeastOneTrack = false;

        // Program the tracks.
        for (int i = 0; i < tracks.length; i++) {
            Format format = tracks[i].getFormat();
            if (tracks[i].isEnabled()) {
                /*if (tracks[i] instanceof VideoFormat) 
                    System.out.println("Supported Video Formats :");
                else
                    System.out.println("Supported Audio Formats :");*/
                supported = tracks[i].getSupportedFormats();
                /*System.out.println("track : "+ i);
                for(int j=0;j<supported.length;j++)
                System.out.println("Supported format : "+supported[j].getEncoding());*/
                // We've set the output content to the RAW_RTP.
                // So all the supported formats should work with RTP.            

                if (supported.length > 0) {
                    for(int j=0;j<supported.length;j++){
                        //System.out.println("Supported format : "+supported[j].toString().toLowerCase());
                        if (supported[j] instanceof VideoFormat) {
                            // For video formats, we should double check the
                            // sizes since not all formats work in all sizes.
                            if(sessionDescription.getVideoFormat()!=null)
                                if(supported[j].toString().toLowerCase().indexOf(
                                    sessionDescription.getVideoFormat().toLowerCase())!=-1)
                                    chosen = checkForVideoSizes(tracks[i].getFormat(), 
                                                                supported[j]);
                        } else {
                            if(sessionDescription.getAudioFormat()!=null)
                                if(supported[j].toString().toLowerCase().indexOf(
                                sessionDescription.getAudioFormat().toLowerCase())!=-1)
                                    chosen = supported[j];  
                        }
                    }
                    if(chosen!=null){
                        tracks[i].setFormat(chosen);                
                        System.err.println("Track " + i + " is set to transmit as:");
                        System.err.println("  " + chosen);
                        atLeastOneTrack = true;
                    }
                } else
                    tracks[i].setEnabled(false);
            } else
                tracks[i].setEnabled(false);
        }

        if (!atLeastOneTrack)
            return "Couldn't set any of the tracks to a valid RTP format";

        // Realize the processor. This will internally create a flow
        // graph and attempt to create an output datasource for JPEG/RTP
        // audio frames.
        result = stateListener.waitForState(processor, Controller.Realized);
        if (result == false)
            return "Couldn't realize processor";

        // Set the JPEG quality to .5.
        setJPEGQuality(processor, 0.25f);

        // Get the output data source of the processor
        dataOutput = processor.getDataOutput();

        return null;
    }


    /**
     * Use the RTPManager API to create sessions for each media 
     * track of the processor.
     */
    private String createTransmitter(String localIpAddress) {

        // Cheated.  Should have checked the type.
        PushBufferDataSource pbds = (PushBufferDataSource)dataOutput;
        PushBufferStream pbss[] = pbds.getStreams();

        rtpMgrs = new RTPManager[pbss.length];
        SessionAddress localAddr, destAddr;
        InetAddress ipAddr;
        SendStream sendStream;
        int localPort;
        int destPort;
        SourceDescription srcDesList[];

        for (int i = 0; i < pbss.length; i++) {
            try {
                //New instance of RTPManager
                //to handle the RTP and RTCP transmission
                rtpMgrs[i] = RTPManager.newInstance();	                    

                destPort = sessionDescription.getDestinationPort() + 2*i;
                ipAddr = InetAddress.getByName(sessionDescription.getAddress());
                destAddr = new SessionAddress( ipAddr, destPort);

                localPort = sessionDescription.getLocalPort() + 2*i;
                localAddr = new SessionAddress( 
                	InetAddress.getByName(localIpAddress),
                    localPort);
                //Establishing the connection with the remote host 
                //with the chosen underlying protocol to RTP (either UDP or TCP)
                if(sessionDescription.getTransportProtocol().toLowerCase().equals("tcp")){
                    if(receiver==null){
                        boolean connected=false;
                        System.out.println("Trying to connect to "+ipAddr+"/"+destPort);
                        //Trying to connect to the TCP Port of the remote host to establish a RTP connection
                        while(!connected){
                            try{
                                socketRTPTransmit = new Socket(ipAddr,destPort);
                                System.out.println("Socket connected to "+ipAddr+
                                                    " on port "+destPort);
                                connected=true;
                            }
                            catch(IOException ioe){

                            }
                        }
                        connected=false;
                        int rtcpDestPort=destPort+1;
                        int rtcpLocalPort=localPort+1;
                        System.out.println("Trying to connect to "+ipAddr+"/"+rtcpDestPort);
                        //Trying to connect to the TCP Port of the remote host to establish a RTCP connection
                        while(!connected){
                            try{
                                socketRTCPTransmit = new Socket(ipAddr, rtcpDestPort);	    
                                System.out.println("Control Socket connected to "+ipAddr+
                                                    " on port "+rtcpDestPort);
                                connected=true;
                            }
                            catch(IOException ioe){

                            }
                        }                             
                    }
                    else{
                        socketRTPTransmit=receiver.getSocketRTPReceiver();
                        socketRTCPTransmit=receiver.getSocketRTCPReceiver();
                    }
                    rtpMgrs[i].initialize( new TCPSendAdapter(socketRTPTransmit,socketRTCPTransmit));
                }
                else{
					SessionAddress localAddress = new SessionAddress(
						InetAddress.getByName(localIpAddress),
						destPort);
					rtpMgrs[i].initialize(localAddress);
					SessionAddress destAddress = new SessionAddress(
							InetAddress.getByName(sessionDescription.getAddress()), 
							destPort);                
					rtpMgrs[i].addTarget(destAddress);																	                	
                }
                System.err.println( 
					"Created RTP session: " + 
					sessionDescription.getAddress()+ 
					" dest "+ 
					destPort);
                //Start the transmission with the remote host
                sendStream = rtpMgrs[i].createSendStream(dataOutput, i);		
                sendStream.start();

            } catch (Exception  e) {
                e.printStackTrace();
            }
        }

        return null;
    }


    /**
     * For JPEG and H263, we know that they only work for particular
     * sizes.  So we'll perform extra checking here to make sure they
     * are of the right sizes.
     */
    private Format checkForVideoSizes(Format original, Format supported) {

        int width, height;
        Dimension size = ((VideoFormat)original).getSize();
        Format jpegFmt = new Format(VideoFormat.JPEG_RTP);
        Format h263Fmt = new Format(VideoFormat.H263_RTP);

        if (supported.matches(jpegFmt)) {
            // For JPEG, make sure width and height are divisible by 8.
            width = (size.width % 8 == 0 ? size.width :
                            (int)(size.width / 8) * 8);
            height = (size.height % 8 == 0 ? size.height :
                            (int)(size.height / 8) * 8);
        } else if (supported.matches(h263Fmt)) {
            // For H.263, we only support some specific sizes.
            if (size.width < 128) {
            width = 128;
            height = 96;
            } else if (size.width < 176) {
            width = 176;
            height = 144;
            } else {
            width = 352;
            height = 288;
            }
        } else {
            // We don't know this particular format.  We'll just
            // leave it alone then.
            return supported;
        }

        return (new VideoFormat(null, 
                                new Dimension(width, height), 
                                Format.NOT_SPECIFIED,
                                null,
                                Format.NOT_SPECIFIED)).intersects(supported);
    }


    /**
     * Setting the encoding quality to the specified value on the JPEG encoder.
     * 0.5 is a good default.
     */
    void setJPEGQuality(Player p, float val) {

        Control cs[] = p.getControls();
        QualityControl qc = null;
        VideoFormat jpegFmt = new VideoFormat(VideoFormat.JPEG);


        // Loop through the controls to find the Quality control for
        // the JPEG encoder.
        for (int i = 0; i < cs.length; i++) {
            if (cs[i] instanceof QualityControl && cs[i] instanceof Owned) {
                Object owner = ((Owned)cs[i]).getOwner();

                // Check to see if the owner is a Codec.
                // Then check for the output format.
                if (owner instanceof Codec) {
                    Format fmts[] = ((Codec)owner).getSupportedOutputFormats(null);
                    for (int j = 0; j < fmts.length; j++) {
                    if (fmts[j].matches(jpegFmt)) {
                        qc = (QualityControl)cs[i];
                        qc.setQuality(val);
                        System.err.println("- Setting quality to " + 
                                val + " on " + qc);
                        break;
                    }
                }
            }
            if (qc != null)
                break;
            }
        }
    }
    /**
     * Start the RTP transmission
     */
    public void transmit(String localIpAddress){
        Format fmt = null;
        int i = 0;
		
        // Start the transmission
        String result = this.start(localIpAddress);

        // result will be non-null if there was an error. The return
        // value is a String describing the possible error. Print it.
        if (result != null) {
            System.err.println("Error : " + result);
            //System.exit(0);
        }

        System.err.println("Start transmission... ");
    }       
    
    public Socket getSocketRTPTransmit(){
        return socketRTPTransmit;
    }
    
    public Socket getSocketRTCPTransmit(){
        return socketRTCPTransmit;
    }
    /****************************************************************
     * Sample Usage for AVTransmit class                            *
     ****************************************************************/
    public static void main(String [] args) {
        if (args.length < 1) {
            prUsage();
        }

        // Create a transmit object with the specified params.
        Transmit transmit = new Transmit(args[0]);
        // Start the media transmission
        String result = transmit.start("127.0.0.1");

        // result will be non-null if there was an error. The return
        // value is a String describing the possible error. Print it.
        if (result != null) {
            System.err.println("Error : " + result);
            //System.exit(0);
        }

        System.err.println("Start transmission for 60 seconds...");

        // Transmit for 60 seconds and then close the processor
        // This is a safeguard when using a capture data source
        // so that the capture device will be properly released
        // before quitting.
        // The right thing to do would be to have a GUI with a
        // "Stop" button that would call stop on AVTransmit2
        try {
            Thread.sleep(60000);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        // Stop the transmission
        transmit.stop();

        System.err.println("...transmission ended.");
    }


    static void prUsage() {
        System.err.println("Usage: AVTransmit <IPAddress>/<LocalPort>/<DestPort>");
        //System.exit(0);
    }
}

