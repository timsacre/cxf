/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.rs.security.jose.jws;

import java.util.Collections;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.rs.security.jose.JoseConstants;
import org.apache.cxf.rs.security.jose.JoseHeaders;
import org.apache.cxf.rs.security.jose.JoseHeadersReaderWriter;
import org.apache.cxf.rs.security.jose.JoseUtils;
import org.apache.cxf.rs.security.jose.jwk.JsonWebKey;


public class JwsJsonSignatureEntry {
    private String encodedJwsPayload;
    private String encodedProtectedHeader;
    private String encodedSignature;
    private JwsJsonProtectedHeader protectedHeader;
    private JwsJsonUnprotectedHeader unprotectedHeader;
    private JoseHeaders unionHeaders;
      
    public JwsJsonSignatureEntry(String encodedJwsPayload,
                                 String encodedProtectedHeader,
                                 String encodedSignature,
                                 JwsJsonUnprotectedHeader unprotectedHeader) {
        if (encodedProtectedHeader == null && unprotectedHeader == null || encodedSignature == null) {
            throw new SecurityException("Invalid security entry");
        }
        
        this.encodedJwsPayload = encodedJwsPayload;
        this.encodedProtectedHeader = encodedProtectedHeader;
        this.encodedSignature = encodedSignature;
        this.unprotectedHeader = unprotectedHeader;
        this.protectedHeader = new JwsJsonProtectedHeader(
            new JoseHeadersReaderWriter().fromJsonHeaders(JoseUtils.decodeToString(encodedProtectedHeader)));
        prepare();
    }
    private void prepare() {
        unionHeaders = new JoseHeaders();
        
        if (protectedHeader != null) {
            unionHeaders.asMap().putAll(protectedHeader.getHeaderEntries().asMap());
        }
        if (unprotectedHeader != null) {
            if (!Collections.disjoint(unionHeaders.asMap().keySet(), 
                                     unprotectedHeader.getHeaderEntries().asMap().keySet())) {
                throw new SecurityException("Protected and unprotected headers have duplicate values");
            }
            unionHeaders.asMap().putAll(unprotectedHeader.getHeaderEntries().asMap());
        }
    }
    public String getEncodedJwsPayload() {
        return encodedJwsPayload;
    }
    public String getDecodedJwsPayload() {
        return JoseUtils.decodeToString(encodedJwsPayload);
    }
    public byte[] getDecodedJwsPayloadBytes() {
        return StringUtils.toBytesUTF8(getDecodedJwsPayload());
    }
    public String getEncodedProtectedHeader() {
        return encodedProtectedHeader;
    }
    public JwsJsonProtectedHeader getProtectedHeader() {
        return protectedHeader;
    }
    public JwsJsonUnprotectedHeader getUnprotectedHeader() {
        return unprotectedHeader;
    }
    public JoseHeaders getUnionHeader() {
        return unionHeaders;
    }
    public String getEncodedSignature() {
        return encodedSignature;
    }
    public byte[] getDecodedSignature() {
        return JoseUtils.decode(getEncodedSignature());
    }
    public String getUnsignedEncodedSequence() {
        return getEncodedProtectedHeader() + "." + getEncodedJwsPayload();
    }
    public String getKeyId() {
        return getUnionHeader().getKeyId();
    }
    public boolean verifySignatureWith(JwsSignatureVerifier validator) {
        try {
            return validator.verify(getUnionHeader(),
                                    getUnsignedEncodedSequence(),
                                    getDecodedSignature());
        } catch (SecurityException ex) {
            // ignore
        }
        return false;
    }
    public boolean verifySignatureWith(JsonWebKey key) {
        return verifySignatureWith(JwsUtils.getSignatureVerifier(key));
    }
    public boolean validateCriticalHeaders() {
        if (this.getUnprotectedHeader().getHeader(JoseConstants.HEADER_CRITICAL) != null) {
            return false;
        }
        return JwsUtils.validateCriticalHeaders(getUnionHeader());
    }
    public String toJson() {
        return toJson(false);
    }
    public String toJson(boolean flattenedMode) {
        StringBuilder sb = new StringBuilder();
        if (!flattenedMode) {
            sb.append("{");
        }
        if (protectedHeader != null) {
            sb.append("\"protected\":\"" + protectedHeader.getEncodedHeaderEntries() + "\"");
        }
        if (unprotectedHeader != null) {
            if (protectedHeader != null) {
                sb.append(",");
            }
            sb.append("\"header\":\"" + unprotectedHeader.toJson());
        }
        sb.append(",");
        sb.append("\"signature\":\"" + encodedSignature + "\"");
        if (!flattenedMode) {
            sb.append("}");
        }
        return sb.toString();
    }
}
