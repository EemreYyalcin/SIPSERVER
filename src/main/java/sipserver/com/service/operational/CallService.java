package sipserver.com.service.operational;

import java.util.Objects;

import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;

import com.mgcp.transport.MgcpSession;
import com.noyan.util.NullUtil;

import sipserver.com.domain.Extension;
import sipserver.com.executer.core.ServerCore;
import sipserver.com.executer.core.SipServerSharedProperties;
import sipserver.com.executer.sip.transaction.ClientTransaction;
import sipserver.com.executer.sip.transaction.ServerTransaction;
import sipserver.com.executer.sip.transaction.TransactionBuilder;
import sipserver.com.parameter.param.CallParam;
import sipserver.com.server.mediaserver.IncomingCallMediaSession;
import sipserver.com.server.mediaserver.OutgoingCallMediaSession;

public class CallService {

	private static Logger logger = Logger.getLogger(CallService.class);

	public static void bridgeCall(ServerTransaction serverTransaction, Extension toExten) {
		try {
			CallParam fromCallParam = serverTransaction.getCallParam();
			CallParam toCallParam = new CallParam();
			toCallParam.setExtension(toExten);
			if (SipServerSharedProperties.mediaServerActive) {
				IncomingCallMediaSession incomingCallMediaSession = new IncomingCallMediaSession(serverTransaction, toCallParam);
				MgcpSession mgcpSession = new MgcpSession(incomingCallMediaSession);
				if (Objects.isNull(mgcpSession)) {
					BridgeService.observeServerTransaction(serverTransaction, Response.BUSY_HERE, null);
					return;
				}
				fromCallParam.setMgcpSession(mgcpSession);
				mgcpSession.createBRIDGE(fromCallParam.getSdpRemoteContent());
				return;
			}

			toCallParam.setSdpLocalContent(fromCallParam.getSdpRemoteContent());
			beginCall(toCallParam, serverTransaction);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void beginCall(CallParam toCallParam, ServerTransaction serverTransaction) {
		try {

			if (!toCallParam.getExtension().isRegister()) {
				BridgeService.observeServerTransaction(serverTransaction, Response.BUSY_HERE, null);
				logger.debug("Not Route 1");
				return;
			}

			if (!toCallParam.getExtension().isAlive()) {
				BridgeService.observeServerTransaction(serverTransaction, Response.BUSY_HERE, null);
				logger.debug("Not Route 2 " + toCallParam.getExtension().getExten());
				return;
			}

			if (SipServerSharedProperties.mediaServerActive) {
				OutgoingCallMediaSession outgoingCallMediaSession = new OutgoingCallMediaSession(toCallParam, serverTransaction);

				MgcpSession mgcpSession = new MgcpSession(outgoingCallMediaSession);
				if (Objects.isNull(mgcpSession)) {
					return;
				}
				if (NullUtil.isNull(serverTransaction)) {
					// TODO: Only Out Call Without Bridge
					return;
				}
				toCallParam.setMgcpSession(mgcpSession);
				mgcpSession.createBRIDGEwithEndpointName(serverTransaction.getCallParam().getMgcpSession().getSpecificEndpointName());
				return;
			}

			Request request = ClientTransaction.createInviteMessage(toCallParam, serverTransaction.getCallParam());
			request.setContent(toCallParam.getSdpLocalContent(), ServerCore.getCoreElement().getHeaderFactory().createContentTypeHeader("application", "sdp"));
			ClientTransaction clientTransaction = TransactionBuilder.createClientTransaction(request, toCallParam.getExtension());
			if (Objects.isNull(clientTransaction)) {
				BridgeService.observeServerTransaction(serverTransaction, Response.BUSY_HERE, null);
				return;
			}
			toCallParam.setRequest(request);
			clientTransaction.setCallParam(toCallParam);
			clientTransaction.setBridgeTransaction(serverTransaction);
			serverTransaction.setBridgeTransaction(clientTransaction);
			clientTransaction.getTransport().sendSipMessage(clientTransaction.getRequest(), clientTransaction.getAddress(), clientTransaction.getPort(), clientTransaction.getSession());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
