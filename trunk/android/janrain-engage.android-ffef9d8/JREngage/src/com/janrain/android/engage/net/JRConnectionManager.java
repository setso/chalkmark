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
package com.janrain.android.engage.net;

import android.os.Handler;
import android.util.Config;
import android.util.Log;
import com.janrain.android.engage.JREngage;
import com.janrain.android.engage.net.async.AsyncHttpClient;
import com.janrain.android.engage.net.async.AsyncHttpResponse;
import org.apache.http.NameValuePair;

import java.util.ArrayList;
import java.util.List;

/**
 * @internal
 *
 * @class JRConnectionManager
 **/
public class JRConnectionManager implements AsyncHttpClient.AsyncHttpResponseListener {
    private static final String TAG = JRConnectionManager.class.getSimpleName();
	private static JRConnectionManager sInstance;

    private JRConnectionManager() {}

    public static synchronized JRConnectionManager getInstance() {
		if (sInstance == null) sInstance = new JRConnectionManager();
		return sInstance;
	}

    /**
     * Creates a managed HTTP GET connection.
     *
     * @param requestUrl
     *      The URL to be executed. May not be null.
     *
     * @param delegate
     *      The delegate (listener) class instance. May be null.
     * @param tag
     *      Optional tag for the connection, later passed to the delegate for the purpose of distinguishing
     *      multiple connections handled by a single delegate.
     */
	public static void createConnection(String requestUrl,
                                        JRConnectionManagerDelegate delegate,
                                        Object tag) {
        createConnection(requestUrl, delegate, tag, null);
	}

    /**
     * Creates a managed HTTP GET connection.
     *
     * @param requestUrl
     *      The URL to be executed. May not be null.
     * @param delegate
     *      The delegate (listener) class instance. May be null. Callback methods will be invoked on the UI
     *      thread.
     * @param tag
     *      Optional tag for the connection, later passed to the delegate for the purpose of distinguishing
     *      multiple connections handled by a single delegate.
     * @param requestHeaders
     *      Optional list of HTTP headers to add to the request.
     *
     */
    public static void createConnection(String requestUrl,
                                        JRConnectionManagerDelegate delegate,
                                        Object tag,
                                        List<NameValuePair> requestHeaders) {
        if (requestHeaders == null) requestHeaders = new ArrayList<NameValuePair>();

        ConnectionData connectionData = new ConnectionData(delegate, tag);
        connectionData.setRequestUrl(requestUrl);
        connectionData.setRequestHeaders(requestHeaders);

        new AsyncHttpClient.HttpSender(
                connectionData,
                new Handler(),
                new AsyncHttpClient.HttpCallbackWrapper(getInstance(), connectionData)
        ).start();
        JREngage.logd(TAG, "[executeHttpRequest] invoked");
    }

    /**
     *  Creates a managed HTTP POST connection.
     *
     * @param requestUrl
     *      The URL to be executed. May not be null.
     * @param delegate
     *      The delegate (listener) class instance. May be null. Callback methods will be invoked on the UI
     *      thread.
     * @param tag
     *      Optional tag for the connection, later passed to the delegate for the purpose of distinguishing
     *      multiple connections handled by a single delegate.
     * @param requestHeaders
     *      Optional list of HTTP headers to add to the request.
     * @param postData
     *      Optional data to post.
     */
    public static void createConnection(String requestUrl,
                                        JRConnectionManagerDelegate delegate,
                                        Object tag,
                                        List<NameValuePair> requestHeaders,
                                        byte[] postData) {
        if (postData == null) postData = new byte[0];
        if (requestHeaders == null) requestHeaders = new ArrayList<NameValuePair>();

        ConnectionData connectionData = new ConnectionData(delegate, tag);
        connectionData.setRequestUrl(requestUrl);
        connectionData.setPostData(postData);
        connectionData.setRequestHeaders(requestHeaders);

        new AsyncHttpClient.HttpSender(
                connectionData,
                new Handler(),
                new AsyncHttpClient.HttpCallbackWrapper(getInstance(), connectionData)
        ).start();
        JREngage.logd(TAG, "[executeHttpRequest] invoked");
    }


	public void onResponseReceived(AsyncHttpResponse response) {
        String requestUrl = response.getUrl();
        ConnectionData connectionData = response.getConnectionData();

        JRConnectionManagerDelegate delegate = connectionData.mDelegate;

        if (delegate == null) return;

        if (response.hasException()) {
            delegate.connectionDidFail(response.getException(), requestUrl, connectionData.mTag);
        } else {
            delegate.connectionDidFinishLoading(
                    response.getHeaders(),
                    response.getPayload(),
                    requestUrl,
                    connectionData.mTag);
        }
	}

    public static class ConnectionData {
        private Object mTag;
        private JRConnectionManagerDelegate mDelegate;
        private String mRequestUrl;
        private byte[] mPostData;

        public String getRequestUrl() {
            return mRequestUrl;
        }

        public byte[] getPostData() {
            return mPostData;
        }

        public List<NameValuePair> getRequestHeaders() {
            return mRequestHeaders;
        }

        private List<NameValuePair> mRequestHeaders;

        public ConnectionData(JRConnectionManagerDelegate delegate,
                              Object tag) {
            mDelegate = delegate;
            mTag = tag;
        }

        public void setRequestUrl(String requestUrl) {
            mRequestUrl = requestUrl;
        }

        public void setPostData(byte[] data) {
            mPostData = data;
        }

        public void setRequestHeaders(List<NameValuePair> requestHeaders) {
            mRequestHeaders = requestHeaders;
        }
    }
}
