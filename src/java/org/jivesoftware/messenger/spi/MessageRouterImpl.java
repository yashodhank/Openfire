/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2004 Jive Software. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */

package org.jivesoftware.messenger.spi;

import org.jivesoftware.messenger.container.BasicModule;
import org.jivesoftware.messenger.*;
import org.jivesoftware.messenger.auth.UnauthorizedException;
import org.jivesoftware.util.Log;
import org.xmpp.packet.Message;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

/**
 * Generic message routing base class.
 *
 * @author Iain Shigeoka
 */
public class MessageRouterImpl extends BasicModule implements MessageRouter {

    private OfflineMessageStrategy messageStrategy;
    private RoutingTable routingTable;
    private SessionManager sessionManager;

    /**
     * <p>Create a packet router.</p>
     */
    public MessageRouterImpl() {
        super("XMPP Message Router");
    }

    public void route(Message packet) {
        if (packet == null) {
            throw new NullPointerException();
        }
        Session session = sessionManager.getSession(packet.getFrom());
        if (session == null
                || session.getStatus() == Session.STATUS_AUTHENTICATED)
        {
            JID recipientJID = packet.getTo();

            try {
                routingTable.getBestRoute(recipientJID).process(packet);
            }
            catch (Exception e) {
                try {
                    messageStrategy.storeOffline(packet);
                }
                catch (Exception e1) {
                    Log.error(e1);
                }
            }

        }
        else {
            packet.setTo(session.getAddress());
            packet.setFrom((JID)null);
            packet.setError(PacketError.Condition.not_authorized);
            try {
                session.process(packet);
            }
            catch (UnauthorizedException ue) {
                Log.error(ue);
            }
        }
    }

    public void initialize(XMPPServer server) {
        super.initialize(server);
        messageStrategy = server.getOfflineMessageStrategy();
        routingTable = server.getRoutingTable();
        sessionManager = server.getSessionManager();
    }
}