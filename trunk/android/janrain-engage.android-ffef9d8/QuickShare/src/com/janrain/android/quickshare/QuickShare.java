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

package com.janrain.android.quickshare;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.EndTextElementListener;
import android.sax.RootElement;
import android.text.Editable;
import android.text.Html;
import android.util.Config;
import android.util.Log;
import android.util.Pair;
import android.util.Xml;
import android.widget.Toast;
import com.google.android.filecache.FileResponseCache;
import com.google.android.imageloader.ImageLoader;
import com.janrain.android.engage.JREngage;
import com.janrain.android.engage.JREngageDelegate;
import com.janrain.android.engage.JREngageError;
import com.janrain.android.engage.net.JRConnectionManager;
import com.janrain.android.engage.net.JRConnectionManagerDelegate;
import com.janrain.android.engage.net.async.HttpResponseHeaders;
import com.janrain.android.engage.types.JRActivityObject;
import com.janrain.android.engage.types.JRDictionary;
import com.janrain.android.engage.utils.AndroidUtils;
import com.janrain.android.engage.utils.Archiver;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.ContentHandler;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class QuickShare extends Application {
    private static QuickShare sInstance;

    private static final String TAG = QuickShare.class.getSimpleName();

    private static final Uri FEED_URL = Uri.parse("http://www.janrain.com/feed/blogs");
    private static final SimpleDateFormat FEED_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-ddzzzhh:mm:ss'-:00'");
    private static final SimpleDateFormat MORE_READABLE_DATE_FORMAT =
            new SimpleDateFormat("EEEE, MMMM d, yyyy h:mm aa");

    private static final String ARCHIVE_STORIES_ARRAY = "storiesArray";
    private static final String ARCHIVE_STORY_LINKS_HASH = "storyLinksHash";

    private static String ENGAGE_APP_ID = QuickShareEnvironment.getAppId();
    private static String ENGAGE_TOKEN_URL = QuickShareEnvironment.getTokenUrl();

    private HashSet<String> mStoryLinks;
    final private ArrayList<Story> mStories = new ArrayList<Story>();
    private FeedReaderListener mListener;

    private JREngage mEngage;

    private ImageLoader mImageLoader;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "[onCreate]");

        JREngage.sLoggingEnabled = true;

        sInstance = this;

        JRFileResponseCache jrfrc = new JRFileResponseCache(this);
        ResponseCache.setDefault(jrfrc);
        java.net.ContentHandler bmch = JRFileResponseCache.capture(new JRBitmapContentHandler(), null);
        java.net.ContentHandler pfch = JRFileResponseCache.capture(JRFileResponseCache.sink(), null);
        mImageLoader = new ImageLoader(ImageLoader.DEFAULT_TASK_LIMIT, null, bmch, pfch,
                ImageLoader.DEFAULT_CACHE_SIZE, null);

        /* If the Story class changes, then the Archiver can't load the new stories, which is fine,
            They'll just get re-downloaded/added, but we also have to clear the links hash, so that
            the new stories get added. */
        try {
            ArrayList<Story> loadedStories = Archiver.load(ARCHIVE_STORIES_ARRAY, this);
            mStories.clear();
            mStories.addAll(loadedStories);
            mStoryLinks = Archiver.load(ARCHIVE_STORY_LINKS_HASH, this);
            logd(TAG, "[ctor] loaded " + mStories.size() + " stories from disk");
        } catch (Archiver.LoadException e) {
            mStories.clear();
            mStoryLinks = new HashSet<String>();
            logd(TAG, "[ctor] stories reset");
        }
    }

    public static QuickShare getInstance() {
        return sInstance;
    }

    public JREngage getJREngage() {
        return mEngage;
    }

    public void setFeedReaderListener(FeedReaderListener feedReaderListener) {
        mListener = feedReaderListener;
    }

    private static class JRFileResponseCache extends FileResponseCache {
        private Context mContext;
        private File mCacheDir;
        private MessageDigest mNameDigest;

        public JRFileResponseCache(Context c) {
            mContext = c;
            mCacheDir = new File(mContext.getCacheDir(), "filecache");
            try {
                mNameDigest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        protected File getFile(URI uri, String s, Map<String, List<String>> stringListMap, Object o) {
            try {
                mNameDigest.reset();
                mNameDigest.update(uri.toString().getBytes("UTF-8"));
                byte[] output = mNameDigest.digest();
                StringBuilder builder = new StringBuilder("jr_cache_");
                for (byte anOutput : output) builder.append(Integer.toHexString(0xFF & anOutput));
                builder.append(AndroidUtils.urlEncode(uri.toString()));
                return new File(mCacheDir, builder.toString());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void initJREngage(Activity activity) {
        mEngage = JREngage.initInstance(activity, ENGAGE_APP_ID, ENGAGE_TOKEN_URL, mJrEngageDelegate);
    }

    private void logd(String function, String message) {
        if (Config.LOGD && function != null && message != null) Log.d(TAG, "[" + function + "] " + message);
    }

    public ImageLoader getImageLoader() {
        return mImageLoader;
    }

    public interface FeedReaderListener {
        void asyncFeedReadSucceeded();

        void asyncFeedReadFailed(Exception e);
    }

    public void loadJanrainBlog() {
        JRConnectionManager.createConnection(FEED_URL.toString(),
                new JRConnectionManagerDelegate.SimpleJRConnectionManagerDelegate() {
                    @Override
                    public void connectionDidFinishLoading(HttpResponseHeaders headers,
                                                           byte[] payload,
                                                           String requestUrl,
                                                           Object tag) {
                        processBlogLoad(payload);
                    }

                    @Override
                    public void connectionDidFail(Exception ex, String requestUrl, Object tag) {
                        mListener.asyncFeedReadFailed(ex);
                    }
                }, null);
    }

    @SuppressWarnings("unchecked")
    private void processBlogLoad(final byte[] payload) {
        new AsyncTask<Void, Void, Pair<Boolean, Exception>>() {
            protected Pair<Boolean, Exception> doInBackground(Void... v) {
                logd("loadJanrainBlog", "loading blog");

                final Story currentStory = new Story();
                RootElement root = new RootElement("rss");
                Element channel = root.getChild("channel");
                Element item = channel.getChild("item");
                item.setEndElementListener(new EndElementListener(){
                    public void end() {
                        if (mStoryLinks.contains(currentStory.getLink())) return;

                        Log.d(TAG, "[addStoryOnlyIfNew] story hasn't been added");

                        synchronized (mStories) {
                            mStories.add(currentStory.copy());
                            Collections.sort(mStories);
                            mStoryLinks.add(currentStory.getLink());
                        }
                    }
                });
                item.getChild("title").setEndTextElementListener(new EndTextElementListener() {
                    public void end(String body) {
                        currentStory.setTitle(body);
                    }
                });
                item.getChild("link").setEndTextElementListener(new EndTextElementListener(){
                    public void end(String body) {
                        currentStory.setLink(body);
                    }
                });
                item.getChild("description").setEndTextElementListener(new EndTextElementListener(){
                    public void end(String body) {
                        currentStory.setDescription(body);

                        final ArrayList<String> imageUrls = new ArrayList<String>();
                        /* The description is in HTML, so we decode it to display it as plain text,
                            and while decoding it we yoink out a link to an image if there is one. */
                        String plainText = Html.fromHtml(body, new Html.ImageGetter() {
                            public Drawable getDrawable(String s) {
                                imageUrls.add(FEED_URL.getScheme() + "://" + FEED_URL.getHost() + s);
                                return null;
                            }
                        }, new Html.TagHandler() {
                            public void handleTag(boolean opening, String tag, Editable output,
                                                  XMLReader xmlReader) {
                                /* Remove <style> tags because the WebView we display them with doesn't
                                   handle them. */
                                if (tag.equalsIgnoreCase("style")) output.clear();
                            }
                        }).toString();

                        /* Remove Unicode object characters */
                        plainText = plainText.replaceAll("\ufffc", "");
                        currentStory.setPlainText(plainText);
                        currentStory.setImageUrls(imageUrls);
                    }
                });
                item.getChild("pubDate").setEndTextElementListener(new EndTextElementListener(){
                    public void end(String body) {
                        /* Parse the blog dates, and then reformat them to be more readable.
                           Example: 2011-04-25PDT04:30:00-:00 */
                        try {
                            Date parsedDate = FEED_DATE_FORMAT.parse(body);
                            currentStory.setRawDate(parsedDate);
                            body = MORE_READABLE_DATE_FORMAT.format(parsedDate);
                        } catch (ParseException e) {
                            /* If there's an error do nothing to the formatted date, leaving it as it was
                             * and set the "raw" date to now. */
                            currentStory.setRawDate(new Date());
                        }

                        currentStory.setFormattedDate(body);
                    }
                });
                item.getChild("http://purl.org/dc/elements/1.1/", "creator")
                        .setEndTextElementListener(new EndTextElementListener() {
                    public void end(String body) {
                        currentStory.setCreator(body);
                    }
                });

                try {
                    Xml.parse(new ByteArrayInputStream(payload),
                            Xml.Encoding.UTF_8,
                            root.getContentHandler());
                } catch (SAXException e) {
                    return new Pair<Boolean, Exception>(false, e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                logd("loadJanrainBlog", "saving stories");
                Archiver.save(ARCHIVE_STORIES_ARRAY, mStories);
                Archiver.save(ARCHIVE_STORY_LINKS_HASH, mStoryLinks);
                logd("loadJanrainBlog", "stories saved");

                return new Pair<Boolean, Exception>(true, null);
            }

            @Override
            protected void onPostExecute(Pair<Boolean, Exception> loadSuccess) {
                logd("onPostExecute", "blog loader onPostExecute, successful: " + loadSuccess.first);

                if (loadSuccess.first) {
                    mListener.asyncFeedReadSucceeded();
                } else {
                    mListener.asyncFeedReadFailed(loadSuccess.second);
                }
            }
        }.execute();
    }

    public ArrayList<Story> getFeed() {
        /* Returning a copy of the mStories array list so that adding stories while the list
            is being used as the list adapter for the Feed Summary activity doesn't crash the app. */
        synchronized (mStories) {
            return new ArrayList<Story>(mStories);
        }
    }

    public void deleteAllStories() {
        synchronized (mStories) {
            mStories.clear();
            mStoryLinks.clear();
        }

        if (Config.LOGD) {
            Log.d(TAG, "[deleteAllStories] " + ((Integer) mStories.size()).toString() + " stories remain");
        }

        Archiver.save(ARCHIVE_STORIES_ARRAY, mStories);
        Archiver.save(ARCHIVE_STORY_LINKS_HASH, mStoryLinks);
    }

    JREngageDelegate mJrEngageDelegate = new JREngageDelegate() {
        public void jrEngageDialogDidFailToShowWithError(JREngageError error) {
            String toastText;
            if (error.hasException()) {
                //noinspection ThrowableResultOfMethodCallIgnored
                toastText = error.getException().toString();
            } else {
                toastText = error.getMessage();
            }

            Toast.makeText(JREngage.getActivity(), toastText, Toast.LENGTH_LONG).show();
        }

        public void jrAuthenticationDidNotComplete() {}

        public void jrAuthenticationDidSucceedForUser(JRDictionary auth_info, String provider) {}

        public void jrAuthenticationDidFailWithError(JREngageError error, String provider) {}

        public void jrAuthenticationDidReachTokenUrl(String tokenUrl,
                                                     HttpResponseHeaders headers,
                                                     String tokenUrlPayload,
                                                     String provider) {}

        public void jrAuthenticationCallToTokenUrlDidFail(String tokenUrl,
                                                          JREngageError error,
                                                          String provider) {}

        public void jrSocialDidNotCompletePublishing() {}

        public void jrSocialDidCompletePublishing() {}

        public void jrSocialDidPublishJRActivity(JRActivityObject activity, String provider) {}

        public void jrSocialPublishJRActivityDidFail(JRActivityObject activity,
                                                     JREngageError error,
                                                     String provider) {}
    };
}

class JRBitmapContentHandler extends ContentHandler {
    @Override
    public Bitmap getContent(URLConnection connection) throws IOException {
        InputStream input = connection.getInputStream();
        try {
            input = new JRBlockingFilterInputStream(input);
            BitmapFactory.Options options = new BitmapFactory.Options();
            int j = connection.getContentLength();
            options.inSampleSize = 4;
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
            if (bitmap == null) throw new IOException("Image could not be decoded");
            return bitmap;
        } finally {
            input.close();
        }
    }
}

/* XXX This is copy/paste of a package scoped class from the Google ImageLoader library */
class JRBlockingFilterInputStream extends FilterInputStream {

    public JRBlockingFilterInputStream(InputStream input) {
        super(input);
    }

    @Override
    public int read(byte[] buffer, int offset, int count) throws IOException {
        int total = 0;
        while (total < count) {
            int read = super.read(buffer, offset + total, count - total);
            if (read == -1) {
                return (total != 0) ? total : -1;
            }
            total += read;
        }
        return total;
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        int total = 0;
        while (total < buffer.length) {
            int offset = total;
            int count = buffer.length - total;
            int read = super.read(buffer, offset, count);
            if (read == -1) {
                return (total != 0) ? total : -1;
            }
            total += read;
        }
        return total;
    }

    @Override
    public long skip(long count) throws IOException {
        long total = 0L;
        while (total < count) {
            long skipped = super.skip(count - total);
            if (skipped == 0L) {
                int b = super.read();
                if (b < 0) {
                    break;
                } else {
                    skipped += 1;
                }
            }
            total += skipped;
        }
        return total;
    }
}
