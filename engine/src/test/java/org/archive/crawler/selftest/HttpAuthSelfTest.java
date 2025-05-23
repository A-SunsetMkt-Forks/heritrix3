/*
 *  This file is part of the Heritrix web crawler (crawler.archive.org).
 *
 *  Licensed to the Internet Archive (IA) by one or more individual 
 *  contributors. 
 *
 *  The IA licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.archive.crawler.selftest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jetty.ee10.servlet.ServletHandler;
import org.eclipse.jetty.ee10.servlet.security.ConstraintMapping;
import org.eclipse.jetty.ee10.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.eclipse.jetty.util.security.Password;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test HTTP basic authentication
 *
 * @author stack
 * @author gojomo
 */
public class HttpAuthSelfTest
    extends SelfTestBase
{
    /**
     * Files to find as a list.
     */
    final private static Set<String> EXPECTED = Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList(new String[] {
            "index.html", "link1.html", "link2.html", "link3.html", 
            "basic/index.html", "basic/link1.html", "basic/link2.html", "basic/link3.html", 
            "robots.txt", "favicon.ico"
    })));
    
    @Override
    protected void verify() throws Exception {
        Set<String> found = this.filesInArcs();
        assertEquals(EXPECTED, found, "wrong files in ARCs");
    }

    @Override
    protected void startHttpServer() throws Exception {
        Server server = new Server();
        
        Constraint constraint = Constraint.from("user","admin","moderator");

        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/basic/*");

        UserStore userStore = new UserStore();
        userStore.addUser("Mr. Happy Pants", new Password("xyzzy"), new String[]{"rule"});
        HashLoginService loginService = new HashLoginService("Hyrule");
        loginService.setUserStore(userStore);
        
        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.setLoginService(loginService);
        securityHandler.setConstraintMappings(new ConstraintMapping[]{cm});
        
        ServerConnector sc = new ServerConnector(server);
        sc.setHost("127.0.0.1");
        sc.setPort(7777);
        server.addConnector(sc);
        ResourceHandler rhandler = new ResourceHandler();
        ResourceFactory resourceFactory = ResourceFactory.of(server);
        rhandler.setBaseResource(resourceFactory.newResource(getSrcHtdocs().toPath().toAbsolutePath()));
        
        server.setHandler(new Handler.Sequence(
                securityHandler,
                rhandler,
                new DefaultHandler()));

        this.httpServer = server;
        this.httpServer.start();
    }

    @Override
    protected String changeGlobalConfig(String config) {
        String newCredStore = 
            "<bean id=\"credentialStore\" class=\"org.archive.modules.credential.CredentialStore\">\n" + 
            "  <property name=\"credentials\">\n" + 
            "   <map>\n" + 
            "    <entry key=\"test\">\n" + 
            "     <bean class=\"org.archive.modules.credential.HttpAuthenticationCredential\">\n" + 
            "      <property name=\"domain\" value=\"127.0.0.1:7777\"/>\n" + 
            "      <property name=\"realm\" value=\"Hyrule\"/>\n" + 
            "      <property name=\"login\" value=\"Mr. Happy Pants\"/>\n" + 
            "      <property name=\"password\" value=\"xyzzy\"/>\n" + 
            "     </bean>\n" + 
            "    </entry>\n" + 
            "   </map>\n" + 
            "  </property>\n" + 
            "</bean>";
        config = config.replaceFirst(
                "(?s)<bean id=\"credentialStore\".*?</bean>", 
                newCredStore);
        return super.changeGlobalConfig(config);
    }

}

