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
package com.janrain.android.engage.ui;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.janrain.android.engage.JREngage;
import com.janrain.android.engage.R;
import com.janrain.android.engage.session.JRProvider;

/**
 * @internal
 *
 * @class JRProvidersActivity
 * Displays list of [basic] providers.
 */
public class JRProviderListFragment extends JRUiFragment {
    public static final int RESULT_FAIL = Activity.RESULT_FIRST_USER;

    /**
     * @internal
     *
     * @class ProviderAdapter
     * Array adapter used to render individual providers in list view.
     **/
    private class ProviderAdapter extends ArrayAdapter<JRProvider> {
        LayoutInflater li = (LayoutInflater)
                JREngage.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        public ProviderAdapter() {
            // The super class only ends up using the last parameter passed into this super constructor,
            // the List.  The first two parameters are never used.

            super(JREngage.getActivity(), 0, mProviderList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                // This line is pretty much copied from the super implementation, which doesn't attach
                // the newly inflated View to it's parent. I don't know why.
                convertView = li.inflate(R.layout.jr_provider_listview_row, parent, false);
            }

            ImageView icon = (ImageView) convertView.findViewById(R.id.jr_row_provider_icon);
            TextView label = (TextView) convertView.findViewById(R.id.jr_row_provider_label);

            JRProvider provider = getItem(position);

            Drawable providerIcon = provider.getProviderIcon(getContext());
            icon.setImageDrawable(providerIcon);
            label.setText(provider.getFriendlyName());

            return convertView;
        }
    }

    private static final int TIMER_MAX_ITERATIONS = 40;

    private ArrayList<JRProvider> mProviderList;
    private ProviderAdapter mAdapter;
    private Timer mTimer;
    private int mTimerCount = 0;

    private ListView mListView;
    private TextView mEmptyTextLabel;
    private ProgressBar mLoadingProgress;

    private Runnable mNoProvidersFoundRunner = new Runnable() {
        public void run() {
            mEmptyTextLabel.setVisibility(View.VISIBLE);
            mLoadingProgress.setVisibility(View.GONE);

            Toast.makeText(getActivity(), "No providers found.", Toast.LENGTH_LONG).show();
        }
    };

    private Runnable mProvidersLoadedRunner = new Runnable() {
        public void run() {
            mListView.setVisibility(View.VISIBLE);
            mLoadingProgress.setVisibility(View.GONE);

            for (JRProvider p : mProviderList) mAdapter.add(p);
            mAdapter.notifyDataSetChanged();
            showHideTaglines();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mSession == null) return null;
        View listView = inflater.inflate(R.layout.jr_provider_listview, container, false);

        mListView = (ListView) listView.findViewById(android.R.id.list);
        mEmptyTextLabel = (TextView) listView.findViewById(R.id.jr_empty_label);
        mLoadingProgress = (ProgressBar) listView.findViewById(android.R.id.empty);

        getActivity().setTitle(R.string.jr_provider_list_title);

        mProviderList = mSession.getBasicProviders();
        if (mProviderList == null) mProviderList = new ArrayList<JRProvider>();

        mAdapter = new ProviderAdapter();
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(itemClickListener);

        if (mProviderList.size() == 0) {
            mListView.setVisibility(View.GONE);
            /* Show progress and poll for results */
            mLoadingProgress.setVisibility(View.VISIBLE);

            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    doSessionPoll();
                }
            }, 0, 500);
        }

        return listView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mTimer != null) mTimer.cancel();
    }

    /**
     * This method will be called when an item in the list is selected.
     */
    private ListView.OnItemClickListener itemClickListener = new ListView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            JRProvider provider = mAdapter.getItem(position);
            mSession.setCurrentlyAuthenticatingProvider(provider);

            if (provider.requiresInput() ||
                    (mSession.getAuthenticatedUserForProvider(provider) != null &&
                    !provider.getForceReauth()) && !mSession.getAlwaysForceReauth()) {
                showUserLanding();
            } else {
                showWebView();
            }
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        JREngage.logd(TAG, "requestCode: " + requestCode + " resultCode: " + resultCode);
        switch (requestCode) {
            case JRUiFragment.REQUEST_LANDING:
                switch (resultCode) {
                    case JRLandingFragment.RESULT_RESTART:
                    case JRLandingFragment.RESULT_SWITCH_ACCOUNTS:
                        break;
                    case Activity.RESULT_OK:
                        getActivity().setResult(Activity.RESULT_OK);
                        getActivity().finish();
                        break;
                    case JRLandingFragment.RESULT_FAIL:
                        getActivity().setResult(RESULT_FAIL);
                        getActivity().finish();
                    default:
                        throw new RuntimeException("unrecognized result code");
                }
                break;
            case JRUiFragment.REQUEST_WEBVIEW:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        getActivity().setResult(Activity.RESULT_OK);
                        getActivity().finish();
                        break;
                    case JRWebViewFragment.RESULT_FAIL:
                        getActivity().setResult(RESULT_FAIL);
                        getActivity().finish();
                        break;
                    case JRWebViewFragment.RESULT_RESTART:
                        break;
                    case JRWebViewFragment.RESULT_BAD_OPENID_URL:
                        // This result is not expected, but I can't test extensively enough to verify
                        // that it cannot be returned, therefore if it is returned we log it and display
                        // the provider selection as usual.
                        Log.e(TAG, "Unexpected RESULT_BAD_OPENID_URL from JRWebView");
                        break;
                    default:
                        throw new RuntimeException("unrecognized result code");
                }
                break;
            default:
                throw new RuntimeException("unexpected requestCode");
        }
    }

    /**
     * Called by timer.  Used when providers are not found in JRSession.
     * Continues polling until providers are found or the polling threshold is hit.
     */
    private void doSessionPoll() {
        ++mTimerCount;
        JREngage.logd(TAG, "[doSessionPoll] timer count: " + mTimerCount);

        if (mTimerCount > TIMER_MAX_ITERATIONS) {
            mTimer.cancel();
            getActivity().runOnUiThread(mNoProvidersFoundRunner);
            Log.w(TAG, "[doSessionPoll] providers not found, max iterations hit, timer cancelled...");
        } else {
            ArrayList<JRProvider> providers = mSession.getBasicProviders();
            if (providers.size() > 0) {
                mProviderList = providers;
                getActivity().runOnUiThread(mProvidersLoadedRunner);
                mTimer.cancel();
                JREngage.logd(TAG, "[doSessionPoll] providers found, timer cancelled...");
            } else {
                JREngage.logd(TAG, "[doSessionPoll] no providers yet, will retry soon...");
            }
        }
    }

    @Override
    protected void onBackPressed() {
        mSession.triggerAuthenticationDidCancel();
        getActivity().setResult(Activity.RESULT_CANCELED);
        getActivity().finish();
    }
}