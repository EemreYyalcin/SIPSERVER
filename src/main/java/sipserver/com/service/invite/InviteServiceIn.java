package sipserver.com.service.invite;

import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.ServerTransaction;
import javax.sip.header.CallIdHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import gov.nist.core.CommonLogger;
import gov.nist.core.StackLogger;
import sipserver.com.domain.Extension;
import sipserver.com.executer.core.ServerCore;
import sipserver.com.parameter.param.CallParam;
import sipserver.com.server.SipServerTransport;
import sipserver.com.service.Service;
import sipserver.com.service.util.CreateService;
import sipserver.com.service.util.ExceptionService;

public class InviteServiceIn extends Service {

	private static StackLogger logger = CommonLogger.getLogger(InviteServiceIn.class);

	public InviteServiceIn() {
		super(logger);
	}

	@Override
	public void processRequest(RequestEvent requestEvent, SipServerTransport transport) throws Exception {
		ServerTransaction serverTransaction = getServerTransaction(transport.getSipProvider(), requestEvent.getRequest());
		if (serverTransaction == null) {
			return;
		}
		try {
			if (requestEvent.getRequest().getMethod().equals(Request.CANCEL)) {
				processCancelMessage(requestEvent, transport);
				return;
			}

			// logger.logFatalError("RegisterRequestProcess:\r\n" + message);
			ServerCore.getServerCore().getTransportService().sendResponseMessage(serverTransaction, requestEvent.getRequest(), Response.TRYING, null);

			CallIdHeader callIDHeader = (CallIdHeader) requestEvent.getRequest().getHeader(CallIdHeader.NAME);
			if (callIDHeader == null) {
				ServerCore.getServerCore().getTransportService().sendResponseMessage(serverTransaction, requestEvent.getRequest(), Response.BAD_EVENT, null);
				return;
			}

			ViaHeader viaHeader = (ViaHeader) requestEvent.getRequest().getHeader(ViaHeader.NAME);
			if (viaHeader == null) {
				ServerCore.getServerCore().getTransportService().sendResponseMessage(serverTransaction, requestEvent.getRequest(), Response.BAD_EVENT, null);
				return;
			}

			if (viaHeader.getBranch() == null || viaHeader.getBranch().length() == 0) {
				ServerCore.getServerCore().getTransportService().sendResponseMessage(serverTransaction, requestEvent.getRequest(), Response.NOT_ACCEPTABLE_HERE, null);
				return;
			}

			if (getChannel(viaHeader.getBranch()) != null) {
				return;
			}

			FromHeader fromHeader = (FromHeader) requestEvent.getRequest().getHeader(FromHeader.NAME);
			if (fromHeader == null) {
				ServerCore.getServerCore().getTransportService().sendResponseMessage(serverTransaction, requestEvent.getRequest(), Response.BAD_EVENT, null);
				return;
			}

			ToHeader toHeader = (ToHeader) requestEvent.getRequest().getHeader(ToHeader.NAME);
			if (toHeader == null) {
				ServerCore.getServerCore().getTransportService().sendResponseMessage(serverTransaction, requestEvent.getRequest(), Response.BAD_EVENT, null);
				return;
			}
			Extension extension = CreateService.createExtension(fromHeader);
			if (extension == null) {
				ServerCore.getServerCore().getTransportService().sendResponseMessage(serverTransaction, requestEvent.getRequest(), Response.FORBIDDEN, null);
				return;
			}

			CallParam callParam = new CallParam();
			callParam.setExtension(extension).setTransaction(serverTransaction).setRequest(requestEvent.getRequest());

			if (requestEvent.getRequest().getRawContent() != null) {
				callParam.setSdpRemoteContent(new String(requestEvent.getRequest().getRawContent()));
			}

			putChannel(viaHeader.getBranch(), callParam);
			System.out.println("Incoming callId: " + callIDHeader.getCallId());

			ServerCore.getServerCore().getRouteService().route(callParam, toHeader);

		} catch (Exception e) {
			e.printStackTrace();
			logger.logFatalError("Message Error. Message:" + requestEvent.getRequest().toString());
			ServerCore.getServerCore().getTransportService().sendResponseMessage(serverTransaction, requestEvent.getRequest(), Response.BAD_EVENT, null);
		}
	}

	@Override
	public void processResponse(ResponseEvent responseEvent, SipServerTransport transport) {
		// NON
	}

	private void processCancelMessage(RequestEvent requestEvent, SipServerTransport transport) throws Exception {
		try {
			ServerTransaction serverTransaction = requestEvent.getServerTransaction();
			if (serverTransaction == null) {
				serverTransaction = getServerTransaction(transport.getSipProvider(), requestEvent.getRequest());
				ExceptionService.checkNullObject(serverTransaction);
			}
			ViaHeader viaHeader = (ViaHeader) requestEvent.getRequest().getHeader(ViaHeader.NAME);
			if (viaHeader == null || viaHeader.getBranch() == null) {
				ServerCore.getServerCore().getTransportService().sendResponseMessage(serverTransaction, requestEvent.getRequest(), Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST, null);
				return;
			}

			CallParam callParam = takeChannel(viaHeader.getBranch());
			if (callParam == null) {
				ServerCore.getServerCore().getTransportService().sendResponseMessage(serverTransaction, requestEvent.getRequest(), Response.CALL_OR_TRANSACTION_DOES_NOT_EXIST, null);
				return;
			}
			ServerCore.getServerCore().getTransportService().sendResponseMessage(serverTransaction, requestEvent.getRequest(), Response.OK, null);
			ServerCore.getServerCore().getStatusService().cancel(callParam);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}