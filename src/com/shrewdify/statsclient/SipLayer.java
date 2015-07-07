package com.shrewdify.statsclient;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;

import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.Origin;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sdp.SessionName;
import javax.sdp.Time;
import javax.sdp.Version;
import javax.sip.ClientTransaction;
import javax.sip.Dialog;
import javax.sip.DialogTerminatedEvent;
import javax.sip.IOExceptionEvent;
import javax.sip.ListeningPoint;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TransactionTerminatedEvent;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;


public class SipLayer implements SipListener {
	private ContactHeader contactHeader;

	static  AddressFactory addressFactory;

	static MessageFactory messageFactory;

	static HeaderFactory headerFactory;

	static SipStack sipStack;

	static int logLevel  = 32;

	static String logFileDirectory = "";


	private ClientTransaction inviteTid;

	private int count;

	private SipProvider sipProvider;

	

	private String transport = "udp";

	private ListeningPoint listeningPoint;

	private static String unexpectedException = "Unexpected exception ";

	private static Logger logger = Logger.getLogger(SipLayer.class);

	private Dialog dialog;





	public void processRequest(RequestEvent requestReceivedEvent) {
		Request request = requestReceivedEvent.getRequest();
		ServerTransaction serverTransactionId = requestReceivedEvent
				.getServerTransaction();

		logger.info("\n\nRequest " + request.getMethod()
				+ " received at shootist " 
				+ " with server transaction id " + serverTransactionId);

		// We are the UAC so the only request we get is the BYE.
		if (request.getMethod().equals(Request.BYE))
			processBye(request, serverTransactionId);
		else 
			logger.info("Unexpected request ! : " + request);

	}

	public void processBye(Request request,
			ServerTransaction serverTransactionId) {
		try {
			logger.info("shootist:  got a bye .");
			if (serverTransactionId == null) {
				logger.info("shootist:  null TID.");
				return;
			}
			Dialog dialog = serverTransactionId.getDialog();
			logger.info("Dialog State = " + dialog.getState());
			Response response = messageFactory.createResponse(200, request);
			serverTransactionId.sendResponse(response);
			logger.info("shootist:  Sending OK.");
			logger.info("Dialog State = " + dialog.getState());

		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(0);

		}
	}

	public synchronized void processResponse(ResponseEvent responseReceivedEvent) {
		logger.info("Got a response");

		Response response = (Response) responseReceivedEvent.getResponse();
		ClientTransaction tid = responseReceivedEvent.getClientTransaction();
		CSeqHeader cseq = (CSeqHeader) response.getHeader(CSeqHeader.NAME);

		logger.info("Response received : Status Code = "
				+ response.getStatusCode() + " " + cseq);

		Dialog dialog = responseReceivedEvent.getDialog();

		if (tid != null)
			logger.info("transaction state is " + tid.getState());
		else 
			logger.info("transaction = " + tid);

		logger.info("Dialog = " + dialog  + " count " + count);

		if (dialog != null) {
			logger.info("Dialog state is " + dialog.getState());
		} else {
			logger.info("Dialog is null -- ignoring response!");
			return;
		}


		try {
			if (response.getStatusCode() == Response.OK) {
				if (cseq.getMethod().equals(Request.INVITE)) {
					Request ackRequest = dialog.createAck( cseq.getSeqNumber() );
					logger.info("dialog = " + dialog);

					// Proxy will fork. I will accept the second dialog
					// but not the first. 
					logger.info("count = " + count);
					if (count == 1) {
						//assertTrue(dialog != this.dialog);
						logger.info("Sending ACK");
						dialog.sendAck(ackRequest);
						
						

					} else {
						// Kill the first dialog by sending a bye.
						//assertTrue (dialog == this.dialog);
						count++;
						logger.info("count = " + count);
						dialog.sendAck(ackRequest);
						MediaManager mediaManager = new MediaManager();
						System.out.println("ContentType:21");
						mediaManager.prepareMediaSession(new String(response.getRawContent()));
						
						System.out.println("ContentType:22");
						mediaManager.startMediaSession(true);
						System.out.println("ContentType:3");
						/*
						SipProvider sipProvider = (SipProvider) responseReceivedEvent.getSource();
						Request byeRequest = dialog.createRequest(Request.BYE);
						ClientTransaction ct = sipProvider.getNewClientTransaction(byeRequest);
						dialog.sendRequest(ct);
						*/	
					}

				} else {
					logger.info("Response method = " + cseq.getMethod());
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			System.exit(0);
		}

	}
	public SipProvider createSipProvider() {
		try {
			listeningPoint = sipStack.createListeningPoint(Configuration.APartyIp,Configuration.APartyPort, transport);
			sipProvider = sipStack.createSipProvider(listeningPoint);
			return sipProvider;
		} catch (Exception ex) {
			logger.error(unexpectedException, ex);
			return null;
		}
	}

	public boolean checkState() {
		return  this.count == 1;
	}

	public void processTimeout(javax.sip.TimeoutEvent timeoutEvent) {

		logger.info("Transaction Time out");
	}

	public void sendInvite() {
		try {

			
			// create >From Header
			SipURI fromAddress = addressFactory.createSipURI(
					Configuration.APartyUser,Configuration.APartyIp);

			Address fromNameAddress = addressFactory.createAddress(fromAddress);
			FromHeader fromHeader = headerFactory.createFromHeader(fromNameAddress, "12345");

			// create To Header
			SipURI toAddress = addressFactory.createSipURI(
					Configuration.BPartyUser,Configuration.BPartyIp);
			Address toNameAddress = addressFactory.createAddress(toAddress);
			ToHeader toHeader = headerFactory.createToHeader(toNameAddress, null);

			// create Request URI
			String peerHostPort = Configuration.BPartyIp + ":" + Configuration.BPartyPort;
			SipURI requestURI = addressFactory.createSipURI(
					Configuration.BPartyUser, peerHostPort);

			// Create ViaHeaders

			ArrayList viaHeaders = new ArrayList();
			ViaHeader viaHeader = headerFactory
					.createViaHeader(Configuration.APartyIp, sipProvider.getListeningPoint(
							transport).getPort(), transport, null);

			// add via headers
			viaHeaders.add(viaHeader);

			SipURI sipuri = addressFactory.createSipURI(null,
					Configuration.APartyIp);
			sipuri.setPort(Configuration.BPartyPort);
			sipuri.setLrParam();

			RouteHeader routeHeader = headerFactory
					.createRouteHeader(addressFactory
							.createAddress(sipuri));

			// Create ContentTypeHeader

			// Create a new CallId header
			CallIdHeader callIdHeader = sipProvider.getNewCallId();

			// Create a new Cseq header
			CSeqHeader cSeqHeader = headerFactory
					.createCSeqHeader(1L, Request.INVITE);

			// Create a new MaxForwardsHeader
			MaxForwardsHeader maxForwards = headerFactory
					.createMaxForwardsHeader(70);

			// Create the request.
			Request request = messageFactory.createRequest(
					requestURI, Request.INVITE, callIdHeader, cSeqHeader,
					fromHeader, toHeader, viaHeaders, maxForwards);
			// Create contact headers

			SipURI contactUrl = addressFactory.createSipURI(
					Configuration.APartyUser, Configuration.APartyIp);
			contactUrl.setPort(listeningPoint.getPort());

			// Create the contact name address.
			SipURI contactURI = addressFactory.createSipURI(
					Configuration.APartyUser, Configuration.APartyIp);
			contactURI.setPort(sipProvider.getListeningPoint("udp").getPort());

			Address contactAddress = addressFactory
					.createAddress(contactURI);

			// Add the contact address.
			contactAddress.setDisplayName(Configuration.APartyUser);

			contactHeader = headerFactory
					.createContactHeader(contactAddress);
			request.addHeader(contactHeader);

			// Dont use the Outbound Proxy. Use Lr instead.
			request.setHeader(routeHeader);
			
			ContentTypeHeader contentTypeHeader =    headerFactory.createContentTypeHeader("application","sdp");
                
//                
//            
//			
//
//			String sdpData="v=0\r\n"
//					+"o=nitinp 1995 1995 IN IP4 10.14.1.129\r\n"
//					+"s=Talk\r\n"
//					+"c=IN IP4 10.14.1.129\r\n"
//					+"t=0 0\r\n"
//					+"m=audio 7078 RTP/AVP 112 111 110 3 0 8 101\r\n"
//					+"a=rtpmap:112 speex/32000\r\n"
//					+"a=fmtp:112 vbr=on\r\n"
//					+"a=rtpmap:111 speex/16000\r\n"
//					+"a=fmtp:111 vbr=on\r\n"
//					+"a=rtpmap:110 speex/8000\r\n"
//					+"a=fmtp:110 vbr=on\r\n"
//					+"a=rtpmap:101 telephone-event/8000\r\n"
//					+"a=fmtp:101 0-11\r\n";
//			byte[] contents = sdpData.getBytes();
			String sdpBody = createSDPBody(Configuration.MediaPort);
			request.setContent(sdpBody, contentTypeHeader);
			Header callInfoHeader = headerFactory.createHeader(
					"Call-Info", "<http://www.antd.nist.gov>");
			request.addHeader(callInfoHeader);
			System.out.println(request.toString());
			// Create the client transaction.
			inviteTid = sipProvider.getNewClientTransaction(request);
			// send the request out.
			inviteTid.sendRequest();
			this.dialog = inviteTid.getDialog();

		} catch (Exception ex) {
			logger.error(unexpectedException, ex);
		}
	}
	
	private String createSDPBody(int audioPort) {
        try {
            SdpFactory sdpFactory = SdpFactory.getInstance();
            SessionDescription sessionDescription =
            sdpFactory.createSessionDescription();
            //Protocol version
            Version version = sdpFactory.createVersion(0);
            sessionDescription.setVersion(version);
            //Owner
            long sdpSessionId=(long)(Math.random() * 1000000);
            Origin origin =
            sdpFactory.createOrigin(Configuration.BPartyUser,sdpSessionId,sdpSessionId+1369,"IN","IP4",
            Configuration.BPartyIp);
            sessionDescription.setOrigin(origin);
            //Session Name
            SessionName sessionName = sdpFactory.createSessionName("-");
            sessionDescription.setSessionName(sessionName);
            //Connection
            Connection connection =
            sdpFactory.createConnection(Configuration.BPartyIp);
            sessionDescription.setConnection(connection);
            //Time
            Time time = sdpFactory.createTime();
            Vector timeDescriptions = new Vector();
            timeDescriptions.add(time);
            sessionDescription.setTimeDescriptions(timeDescriptions);
            //Media Description
            MediaDescription mediaDescription =
            sdpFactory.createMediaDescription("audio",audioPort,1,"RTP/AVP",
            MediaManager.getSdpAudioSupportedCodecs());
            Vector mediaDescriptions = new Vector();
            mediaDescriptions.add(mediaDescription);
            sessionDescription.setMediaDescriptions(mediaDescriptions);
            return sessionDescription.toString();
        } catch (SdpException se) {
            se.printStackTrace();
        }
        return null;
    }
 
	public void processIOException(IOExceptionEvent exceptionEvent) {
		logger.info("IOException happened for "
				+ exceptionEvent.getHost() + " port = "
				+ exceptionEvent.getPort());

	}

	public void processTransactionTerminated(
			TransactionTerminatedEvent transactionTerminatedEvent) {
		logger.info("Transaction terminated event recieved");
	}

	public void processDialogTerminated(
			DialogTerminatedEvent dialogTerminatedEvent) {
		logger.info("dialogTerminatedEvent");

	}

	public void init(String stackname, boolean autoDialog) 
	{
		SipFactory sipFactory = null;
		sipFactory = SipFactory.getInstance();
		sipFactory.setPathName("gov.nist");
		Properties properties = new Properties();
		// If you want to try TCP transport change the following to

		// If you want to use UDP then uncomment this.
		properties.setProperty("javax.sip.STACK_NAME", stackname);

		// The following properties are specific to nist-sip
		// and are not necessarily part of any other jain-sip
		// implementation.
		properties.setProperty("gov.nist.javax.sip.DEBUG_LOG",
				logFileDirectory +	stackname + "debuglog.txt");
		properties.setProperty("gov.nist.javax.sip.SERVER_LOG",
				logFileDirectory + stackname + "log.txt");

		properties.setProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT",(autoDialog? "on": "off"));

		// Set to 0 in your production code for max speed.
		// You need 16 for logging traces. 32 for debug + traces.
		// Your code will limp at 32 but it is best for debugging.
		properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", new Integer(logLevel).toString());

		try {
			// Create SipStack object
			sipStack = sipFactory.createSipStack(properties);

			System.out.println("createSipStack " + sipStack);
		} catch (Exception e) {
			// could not find
			// gov.nist.jain.protocol.ip.sip.SipStackImpl
			// in the classpath
			e.printStackTrace();
			System.err.println(e.getMessage());
			throw new RuntimeException("Stack failed to initialize");
		} 

		try {
			headerFactory = sipFactory.createHeaderFactory();
			addressFactory = sipFactory.createAddressFactory();
			messageFactory = sipFactory.createMessageFactory();

			logger.addAppender(new ConsoleAppender(new SimpleLayout()));
			
			MediaManager.detectSupportedCodecs();
			createSipProvider();
			sipProvider.addSipListener(this);
			System.out.println("Init done");

		} catch (Exception ex) {
			ex.printStackTrace();
			throw new RuntimeException ( ex);
		}
	}

	public static void destroy() {
		sipStack.stop();
	}

	public static void start() throws Exception  {
		sipStack.start();

	}


}
