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
package com.janrain.android.engage.utils;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @internal
 *
 * @class IOUtils
 **/
public final class IOUtils {
	private static final String TAG = IOUtils.class.getSimpleName();

    private IOUtils() {
    }

	/**
	 * Reads the entire contents of the specified stream to a byte array.
	 *
	 * @param in
	 * 		The input stream to read the contents of.
	 *
	 * @return
	 * 		A byte array representing the full contents of the stream, null if stream is null or
	 * 		operation failed.
	 */
	public static byte[] readFromStream(InputStream in) {
		try {
			return  readFromStream(in, false);
		} catch (IOException ignore) {
			// will never happen because we're sending 'false', but need for compilation
            throw new RuntimeException("unexpected IOException");
		}
	}

	/**
	 * Reads the entire contents of the specified stream to a byte array.
	 *
	 * @param in
	 * 		The input stream to read the contents of.
	 *
	 * @param shouldThrowOnError
	 *		Flag indicating whether or not the user wants to handle exceptions that are thrown
	 *		during this operation.
	 *
	 * @return
	 * 		A byte array representing the full contents of the stream, null if stream is null.
	 *
	 * @throws IOException
	 * 		If the user passed <code>true</code> for 'shouldThrowOnError' and an IOException has
	 * 		occurred.
	 */
	public static byte[] readFromStream(InputStream in, boolean shouldThrowOnError) throws IOException {
		if (in != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				byte[] buffer = new byte[1024];
				int len;
				while ((len = in.read(buffer)) != -1) baos.write(buffer, 0, len);
				return baos.toByteArray();
			} catch (IOException e) {
				Log.e(TAG, "[readFromStream] problem reading from input stream.", e);
				if (shouldThrowOnError) throw e;
			} finally {
                baos.close();
			}
		} else {
            Log.e(TAG, "[readFromStream] unexpected null InputStream");
        }

        return null;
	}
}