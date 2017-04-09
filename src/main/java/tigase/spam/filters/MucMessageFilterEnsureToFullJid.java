package tigase.spam.filters;

import tigase.server.Message;
import tigase.server.Packet;
import tigase.spam.SpamFilter;
import tigase.xmpp.NotAuthorizedException;
import tigase.xmpp.StanzaType;
import tigase.xmpp.XMPPResourceConnection;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by andrzej on 08.04.2017.
 */
public class MucMessageFilterEnsureToFullJid
		implements SpamFilter {

	private static final Logger log = Logger.getLogger(MucMessageFilterEnsureToFullJid.class.getCanonicalName());

	@Override
	public String getId() {
		return "muc-message-ensure-to-full-jid";
	}

	@Override
	public boolean filter(Packet packet, XMPPResourceConnection session) {
		if (packet.getElemName() != Message.ELEM_NAME || packet.getType() != StanzaType.groupchat) {
			return true;
		}

		try {
			if (session != null && session.isAuthorized()) {
				if (session.isUserId(packet.getStanzaTo().getBareJID())) {
					return packet.getStanzaTo().getResource() != null;
				}
			} else {
				return false;
			}
		} catch (NotAuthorizedException ex) {
			log.log(Level.FINEST, "Could not compare packet " + packet +
					" destination with session bare jid as session is not authorized yet", ex);
			return false;
		}
		return true;
	}
}
