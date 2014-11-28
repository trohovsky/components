/*
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.switchyard.component.sca;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.oasis_open.docs.ws_tx.wscoor._2006._06.CoordinationContextType;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.arjuna.mw.wst11.common.CoordinationContextHelper;
import com.arjuna.webservices11.wscoor.CoordinationConstants;

/**
 * Serialize/Deserialize the transaction context to be propagated via remote invocation.
 */
public class TransactionContextSerializer {

    /**
     * HTTP header used to communicate the transaction context to be propagated.
     */
    public static final String HEADER_TXCONTEXT = "switchyard-transaction-context";

    /**
     * Serialize the transaction context as a XML String.
     * @param cc CoordinationContextType
     * @return String representation of XML transaction context
     * @throws Exception if it fails to serialise
     */
    public String serialise(CoordinationContextType cc) throws Exception {
        SOAPEnvelope soapenv = MessageFactory.newInstance().createMessage().getSOAPPart().getEnvelope();
        SOAPHeader header = soapenv.getHeader();
        if (header == null) {
            header = soapenv.addHeader();
        }
        final Name name = soapenv.createName(CoordinationConstants.WSCOOR_ELEMENT_COORDINATION_CONTEXT, CoordinationConstants.WSCOOR_PREFIX, CoordinationConstants.WSCOOR_NAMESPACE);
        final SOAPHeaderElement root = header.addHeaderElement(name);
        root.addNamespaceDeclaration(CoordinationConstants.WSCOOR_PREFIX, CoordinationConstants.WSCOOR_NAMESPACE);
        
        /*
         * TODO Is it possible to craft plain DOM element but not to create whole SOAPMessage?
         * following code hits an NPE in the CoordinationContextHelper.serialise() when it invokes
         * getPrefix().equals("xmlns") against the attribute generated by JAXB marshaller.
         * 
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         Document doc = factory.newDocumentBuilder().newDocument();
         Element root = doc.createElement(CoordinationConstants.WSCOOR_PREFIX + ":" + CoordinationConstants.WSCOOR_ELEMENT_COORDINATION_CONTEXT);
         root.setAttribute("xmlns:" + CoordinationConstants.WSCOOR_PREFIX, CoordinationConstants.WSCOOR_NAMESPACE);
         */
        
         CoordinationContextHelper.serialise(cc, root);
         StringWriter sw = new StringWriter();
         Transformer transformer = TransformerFactory.newInstance().newTransformer();
         transformer.transform(new DOMSource(root), new StreamResult(sw));
         return sw.toString();
    }
    
    /**
     * Deserialize the transaction context from a XML String.
     * @param header XML String to be parsed
     * @return CoordinationContextType
     * @throws Exception if it fails to deserialise
     */
    public CoordinationContextType deserialise(String header) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(header)));
        return CoordinationContextHelper.deserialise(doc.getDocumentElement());
    }
}