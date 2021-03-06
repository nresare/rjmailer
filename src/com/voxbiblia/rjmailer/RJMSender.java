package com.voxbiblia.rjmailer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RJMSender is the main entry point to the rjmailer library. It is used to send
 * emails. The typical use case is to construct an instance of this class, use
 * the setter methods to configure it and reuse it to send multiple mail
 * messages. Once this class is configured it's send methods can be used
 * concurrently by multiple threads.
 *
 * @author Noa Resare (noa@voxbiblia.com)
 */
public class RJMSender
{
    private String ehloHostname;
    private String smtpServer;
    private String nameServer;
    private int smtpPort = -1;
    private FieldGenerator fieldGenerator;

    private Resolver resolver;
    private ConversationFactory conversationFactory;
    private boolean calledAfterPropertiesSet = false;

    /** the number of minutes to cache resolver results */
    private static final int RESOLVER_CACHE_TIMEOUT_MINS = 3;
    private SocketFactory socketFactory;

    /**
     * Constructs a new RJMSender instance, that uses the specified ehloHostname
     * as the EHLO command parameters when connecting to remote SMTP servers.
     *
     * @param ehloHostname the hostname of this computer. 
     */
    public RJMSender(String ehloHostname)
    {
        this.ehloHostname = ehloHostname;
        fieldGenerator = new FieldGenerator(ehloHostname);
    }

    /**
     * Sends <code>message</code> to one recipient. If the sum of all values
     * of properties <code>to</code> and <code>bcc</code> is more than one (1),
     * an IllegalArgumentException is thrown.
     *
     *
     * @param message the message to send.
     * @return an RJMResult instance containing tracking information about
     * @throws IllegalArgumentException if there is more than one recipient
     * of <code>message</code>
     * @throws RJMException if there was a failure sending the message
     */
    public RJMResult send(RJMMessage message)
    {
        List<String> tos = AddressUtil.getToAddresses(message);
        if (tos.size() > 1) {
            throw new IllegalArgumentException("Please use the sendMulti() " +
                    "method to send messages with multiple recipients");
        }
        SendResult o = sendMulti(message).get(tos.get(0));
        if (o instanceof RJMException) {
            throw (RJMException)o;
        }
        return (RJMResult)o;
    }

    /**
     * Sends <code>message</code> to multiple recipients. The difference
     * compared to the simplified {@link RJMSender#send send()} method is that
     * it can return multiple results for different recipients. Please note
     * that the results returned can be both {@link RJMException} and
     * {@link RJMResult} instances. 
     *
     * @param message the messge to send.
     * @return a Map mapping recipient addresses to SendResult instances
     */
    public Map<String, SendResult> sendMulti(RJMMessage message)
    {
        if (!calledAfterPropertiesSet) {
            afterPropertiesSet();
        }
        MessageValidator.validate(message);
        List<String> tos = AddressUtil.getToAddresses(message);

        if (resolver != null) {
            return resolveAndSend(message, tos);
        }
        SendState ss = new SendState(smtpServer, tos);


        Conversation c = conversationFactory.getConversation(smtpServer);
        c.sendMail(message, tos, ss);
        return ss.getResults();
    }

    private void afterPropertiesSet()
    {
        calledAfterPropertiesSet = true;
        if (resolver != null) {
            // this means that setResolver was called and the caller knows
            // what she is doing
            return;
        }

        if (socketFactory == null) {
            socketFactory = new TCPSocketFactory();
        }

        if (nameServer != null || smtpServer == null) {
            resolver = new ResolverImpl(nameServer, RESOLVER_CACHE_TIMEOUT_MINS);
        }

        if (conversationFactory == null) {
            ConversationFactoryImpl cf = new ConversationFactoryImpl();
            cf.setEhloHostname(ehloHostname);
            cf.setSocketFactory(socketFactory);
            cf.setFieldGenerator(fieldGenerator);
            if (smtpPort != -1) {
                cf.setSmtpPort(smtpPort);
            }
            conversationFactory = cf;
        }
    }

    private Map<String, SendResult> resolveAndSend(RJMMessage message, List<String> tos)
    {
        SendState ss = new SendState(resolver, tos);
        
        MXData d = ss.nextMXData();
        if (d == null) {
            throw new Error("Invalid state, no MXData");
        }
        while (d != null) {
            Conversation conversation = conversationFactory.getConversation(d.getServer());
            conversation.sendMail(message, d.getRecipients(), ss);

            /*
            for (int i = 0; i < tos.size(); i++) {
                ss.success((String)l.get(i),d.getServer(), result);
            }
            */


            d = ss.nextMXData();
        }
        return ss.getResults();
    }

    // returns a map with string keys and list values where the value is a list
    // of recipients
    Map<String, List<String>> makeMXMap(String[] tos)
    {
        Map<String, List<String>> m = new HashMap<String,List<String>>();

        for (String to : tos) {
            String domain = AddressUtil.getDomain(to);
            String mx = resolver.resolveMX(domain).get(0);
            List<String> l = m.get(mx);
            if (l == null) {
                l = new ArrayList<String>();
                m.put(mx, l);
            }
            l.add(to);
        }
        return m;
    }

    /**
     * Sets an relay SMTP server used for all outgoing messages, or as a
     * fallback if synchronized sending fails.
     *
     * @param smtpServer the hostname or ip address of the SMTP server
     */
    public void setSmtpServer(String smtpServer)
    {
        this.smtpServer = smtpServer;
    }

    /**
     * Sets the DNS nameserver that is used to determine what mailservers to
     * contact when sending email messages.
     *
     * @param nameServer the host name or ip number of the DNS name server
     */
    public void setNameServer(String nameServer)
    {
        this.nameServer = nameServer;
    }

       /**
     * This method can be used to indicate what port to try to connect to
     * when contacting an email server. This is mainly used for testing on
     * networks where the standard SMTP port 25 is blocked by the upstream
     * network service provider.
     *
     * @param smtpPort An alternate SMTP port number to use when connecting
     */
    public void setSmtpPort(int smtpPort)
    {
        this.smtpPort = smtpPort;
    }


    void setSocketFactory(SocketFactory socketFactory)
    {
        this.socketFactory = socketFactory;
    }

    // this method is for testing purposes
    void setResolver(Resolver resolver)
    {
        this.resolver = resolver;
    }

    void setConversationFactory(ConversationFactory conversationFactory)
    {
        this.conversationFactory = conversationFactory;
    }

    FieldGenerator getFieldGenerator()
    {
        return fieldGenerator;
    }
}

