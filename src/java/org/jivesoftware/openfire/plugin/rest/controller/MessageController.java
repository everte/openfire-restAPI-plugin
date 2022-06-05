/*
 * Copyright (c) 2022.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.openfire.plugin.rest.controller;

import javax.ws.rs.core.Response;

import org.jivesoftware.openfire.SessionManager;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.plugin.rest.entity.MessageEntity;
import org.jivesoftware.openfire.plugin.rest.exceptions.ExceptionType;
import org.jivesoftware.openfire.plugin.rest.exceptions.ServiceException;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;

import java.util.UUID;

/**
 * The Class MessageController.
 */
public class MessageController {
    /**
     * The Constant INSTANCE.
     */
    public static final MessageController INSTANCE = new MessageController();

    /**
     * Gets the single instance of MessageController.
     *
     * @return single instance of MessageController
     */
    public static MessageController getInstance() {
        return INSTANCE;
    }

    /**
     * Send broadcast message.
     *
     * @param messageEntity the message entity
     * @throws ServiceException the service exception
     */
    public void sendBroadcastMessage(MessageEntity messageEntity) throws ServiceException {
        if (messageEntity.getBody() != null && !messageEntity.getBody().isEmpty()) {
            SessionManager.getInstance().sendServerMessage(null, messageEntity.getBody());
        } else {
            throw new ServiceException("Message content/body is null or empty", "",
                ExceptionType.ILLEGAL_ARGUMENT_EXCEPTION,
                Response.Status.BAD_REQUEST);
        }
    }

    /**
     * Send broadcast message to a user.
     *
     * @param messageEntity the message entity
     * @param address       the recipient address
     * @param resource      the recipient resource (optional)
     * @param from          the from address (optional)
     * @throws ServiceException the service exception
     */
    public void sendMessageToUser(MessageEntity messageEntity, String address, String resource, String from) throws ServiceException {
        if (messageEntity.getBody() == null || messageEntity.getBody().isEmpty()) {
            throw new ServiceException("Message content/body is null or empty", "",
                ExceptionType.ILLEGAL_ARGUMENT_EXCEPTION,
                Response.Status.BAD_REQUEST);
        }

        if (address == null) {
            throw new ServiceException("Invalid recipient", "",
                ExceptionType.ILLEGAL_ARGUMENT_EXCEPTION,
                Response.Status.BAD_REQUEST);
        }

        JID toJID = new JID(address);
        if (resource != null) {
            toJID = new JID(toJID.getNode(), toJID.getDomain(), resource);
        }

        // if we have a from address, use this to send the message
        if (from != null) {
            Message msg = new Message();
            msg.setBody(messageEntity.getBody());
            msg.setTo(toJID);
            msg.setFrom(from);
            // TODO: !!
            msg.setType(Message.Type.chat);
            XMPPServer.getInstance().getMessageRouter().route(msg);
            return;
        }

        // else send message from server (as headline)
        SessionManager.getInstance().sendServerMessage(toJID, null, messageEntity.getBody());
    }

    public void sendMessageToUser(MessageEntity messageEntity) throws ServiceException {
        if (messageEntity.getBody() == null || messageEntity.getBody().isEmpty()) {
            throw new ServiceException("Message content/body is null or empty", "",
                ExceptionType.ILLEGAL_ARGUMENT_EXCEPTION,
                Response.Status.BAD_REQUEST);
        }

        if (messageEntity.getTo() == null || messageEntity.getTo().isEmpty()) {
            throw new ServiceException("Invalid recipient", "",
                ExceptionType.ILLEGAL_ARGUMENT_EXCEPTION,
                Response.Status.BAD_REQUEST);
        }

        JID toJID = new JID(messageEntity.getTo());
        if (toJID.getResource() != null && !toJID.getResource().isEmpty()) {
            toJID = new JID(toJID.getNode(), toJID.getDomain(), toJID.getResource());
        }

        // if we have a from address, use this to send the message
        if (messageEntity.getFrom() != null) {
            Message message = new Message();
            message.setBody(messageEntity.getBody());
            message.setTo(toJID);
            message.setFrom(messageEntity.getFrom());
            message.setID(UUID.randomUUID().toString());
            setMessageType(messageEntity, message);
            XMPPServer.getInstance().getMessageRouter().route(message);
            return;
        }

        // else send message from server (as headline)
        SessionManager.getInstance().sendServerMessage(toJID, null, messageEntity.getBody());
    }

    private void setMessageType(MessageEntity messageEntity, Message message) throws ServiceException {
        if (messageEntity.getType() != null) {
            try {
                message.setType(Message.Type.valueOf(messageEntity.getType()));
            } catch (IllegalArgumentException e) {
                throw new ServiceException("Message type is invalid", "",
                    ExceptionType.ILLEGAL_ARGUMENT_EXCEPTION,
                    Response.Status.BAD_REQUEST);
            }
        }
    }
}
