

/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the "License").  You may not use this file except
 * in compliance with the License.
 *
 * You can obtain a copy of the license at
 * glassfish/bootstrap/legal/CDDLv1.0.txt or
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * HEADER in each file and include the License file at
 * glassfish/bootstrap/legal/CDDLv1.0.txt.  If applicable,
 * add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your
 * own identifying information: Portions Copyright [yyyy]
 * [name of copyright owner]
 *
 * Copyright 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Portions Copyright Apache Software Foundation.
 */ 
package org.apache.tomcat.util.net.jsse;

import java.io.*;
import java.net.*;
import java.util.Vector;
import java.security.KeyStore;
import java.security.Security;
import java.security.SecureRandom;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.HandshakeCompletedEvent;

/*
  1. Make the JSSE's jars available, either as an installed
     extension (copy them into jre/lib/ext) or by adding
     them to the Tomcat classpath.
  2. keytool -genkey -alias tomcat -keyalg RSA
     Use "changeit" as password ( this is the default we use )
 */

/**
 * SSL server socket factory. It _requires_ a valid RSA key and
 * JSSE. 
 *
 * @author Harish Prabandham
 * @author Costin Manolache
 * @author Stefan Freyr Stefansson
 * @author EKR -- renamed to JSSESocketFactory
 * @author Bill Barker
 */
public class JSSE13SocketFactory extends JSSESocketFactory
{
    public JSSE13SocketFactory () {
        super();
    }

    /**
     * Reads the keystore and initializes the SSL socket factory.
     *
     * NOTE: This method is identical in functionality to the method of the
     * same name in JSSE14SocketFactory, except that this method is used with
     * JSSE 1.0.x (which is an extension to the 1.3 JVM), whereas the other is
     * used with JSSE 1.1.x (which ships with the 1.4 JVM). Therefore, this
     * method uses classes in com.sun.net.ssl, which have since moved to
     * javax.net.ssl, and explicitly registers the required security providers,
     * which come standard in a 1.4 JVM.
     */
     void init() throws IOException {
        try {
            Security.addProvider (new sun.security.provider.Sun());
            Security.addProvider (new com.sun.net.ssl.internal.ssl.Provider());

            String clientAuthStr = (String)attributes.get("clientauth");
            if (clientAuthStr != null){
                clientAuth = Boolean.valueOf(clientAuthStr).booleanValue();
            }
            
            // SSL protocol variant (e.g., TLS, SSL v3, etc.)
            String protocol = (String)attributes.get("protocol");
            if (protocol == null) protocol = defaultProtocol;
            
            // Certificate encoding algorithm (e.g., SunX509)
            String algorithm = (String)attributes.get("algorithm");
            if (algorithm == null) algorithm = defaultAlgorithm;

            // Set up KeyManager, which will extract server key
            com.sun.net.ssl.KeyManagerFactory kmf = 
                com.sun.net.ssl.KeyManagerFactory.getInstance(algorithm);
            String keystorePass = getKeystorePassword();
            kmf.init(getKeystore(keystorePass),
                     keystorePass.toCharArray());

            // Set up TrustManager
            com.sun.net.ssl.TrustManager[] tm = null;
            KeyStore trustStore = getTrustStore();
            if (trustStore != null) {
                com.sun.net.ssl.TrustManagerFactory tmf =
                    com.sun.net.ssl.TrustManagerFactory.getInstance("SunX509");
                tmf.init(trustStore);
                tm = tmf.getTrustManagers();
            }

            // Create and init SSLContext
            com.sun.net.ssl.SSLContext context = 
                com.sun.net.ssl.SSLContext.getInstance(protocol); 
            context.init(kmf.getKeyManagers(), tm, new SecureRandom());

            // Create proxy
            sslProxy = context.getServerSocketFactory();

            // Determine which cipher suites to enable
            String requestedCiphers = (String)attributes.get("ciphers");
            if (requestedCiphers != null) {
                enabledCiphers = getEnabledCiphers
                    (requestedCiphers,
                     sslProxy.getSupportedCipherSuites());
            }

        } catch(Exception e) {
            if( e instanceof IOException )
                throw (IOException)e;
            throw new IOException(e.getMessage());
        }
    }
    protected String[] getEnabledProtocols(SSLServerSocket socket,
                                           String requestedProtocols){
        return null;
    }
    protected void setEnabledProtocols(SSLServerSocket socket, 
                                             String [] protocols){
    }

}
