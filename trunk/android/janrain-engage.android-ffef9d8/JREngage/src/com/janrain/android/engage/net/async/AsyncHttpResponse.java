/*
 *  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *  Copyright (c) 2011, Janrain, Inc.
 *
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification,
 *  are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation and/or
 *    other materials provided with the distribution.
 *  * Neither the name of the Janrain, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived from this
 *    software without specific prior written permission.
 *
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 */
package com.janrain.android.engage.net.async;

import com.janrain.android.engage.net.JRConnectionManager;

/**
 * @internal
 *
 * @class AsyncHttpResponseHolder
 * Wraps the possible data returned from a full asynchronous HTTP request.  This object will
 * contain either headers and data (if successful) or an Exception object (if failed).
 */
public class AsyncHttpResponse {
    private String mUrl;
    private HttpResponseHeaders mHeaders;
    private byte[] mPayload;
    private Exception mException;
    private JRConnectionManager.ConnectionData mConnectionData;

    /**
     * Creates a "success" instance of this object.
     *
     * @param url
     *      The URL that this response corresponds to.
     * @param headers
     *      The response headers resulting from the HTTP operation.
     * @param payload
     *      The data resulting from the HTTP operation.
     */
    public AsyncHttpResponse(String url, HttpResponseHeaders headers, byte[] payload) {
        mUrl = url;
        mHeaders = headers;
        mPayload = payload;
        mException = null;
    }

    /**
     * Creates a "failure" instance of this object.
     *
     * @param url
     *      The URL that this response corresponds to.
     * @param exception
     *      The exception that occurred during this HTTP operation.
     */
    public AsyncHttpResponse(String url, Exception exception) {
        mUrl = url;
        mHeaders = null;
        mPayload = null;
        mException = exception;
    }

    /**
     * Gets the URL that this HTTP response (or error) corresponds to.
     *
     * @return
     *      The URL that this HTTP response (or error) corresponds to.
     */
    public String getUrl() {
        return mUrl;
    }

    /**
     * Gets the headers object.  If the operation failed, it will return null.
     *
     * @return
     * 		The HttpResponseHeaders object synthesized from the HTTP response if
     * 		the operation was successful, null otherwise.
     */
    public HttpResponseHeaders getHeaders() {
        return mHeaders;
    }

    /**
     * Gets the payload array.  If the operation failed, it will return null.
     *
     * @return
     * 		The byte array containing the data (payload) from the HTTP response if
     * 		the operation was successful, null otherwise.
     */
    public byte[] getPayload() {
        return mPayload;
    }

    /**
     * Gets the exception object.  If the operation succeeded, it will return null.
     *
     * @return
     * 		The Exception that occurred as a result of the asynchronous HTTP request if
     * 		the operation failed, null otherwise.
     */
    public Exception getException() {
        return mException;
    }

    /**
     * Checks to see if the holder contains a valid (non-null) headers object.
     *
     * @return
     * 		<code>true</code> if the headers object is valid, <code>false</code> otherwise.
     */
    public boolean hasHeaders() {
        return (mHeaders != null);
    }

    /**
     * Checks to see if the holder contains a valid (non-null) payload array.
     *
     * @return
     * 		<code>true</code> if the payload array is valid, <code>false</code> otherwise.
     */
    public boolean hasPayload() {
        return (mPayload != null);
    }

    /**
     * Checks to see if the holder contains a valid (non-null) exception object.
     *
     * @return
     * 		<code>true</code> if the exception object is valid, <code>false</code> otherwise.
     */
    public boolean hasException() {
        return (mException != null);
    }

    public JRConnectionManager.ConnectionData getConnectionData() {
        return mConnectionData;
    }

    public void setConnectionData(JRConnectionManager.ConnectionData cd) {
        mConnectionData = cd;
    }
}