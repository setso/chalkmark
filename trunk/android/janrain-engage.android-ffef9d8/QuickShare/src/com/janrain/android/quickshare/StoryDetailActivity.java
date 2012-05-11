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

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import com.janrain.android.engage.utils.AndroidUtils;

public class StoryDetailActivity extends FragmentActivity {
    private static final String TAG = StoryDetailActivity.class.getSimpleName();
    private boolean mWaitingForChildActivityToFinish = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mWaitingForChildActivityToFinish = savedInstanceState.getBoolean("waiting", false);
        }

        if (savedInstanceState == null) {
            int index = getIntent().getExtras().getInt("index");
            StoryDetailFragment details = StoryDetailFragment.newInstance(index);
            setTitle(details.getShownStory().getTitle());
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, details).commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");
        if (getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE
                && AndroidUtils.SDK_INT >= 11
                && AndroidUtils.isXlarge()
                && !mWaitingForChildActivityToFinish) {
            // If the screen is now in landscape mode, we can show the
            // dialog in-line with the list so we don't need this activity.
            finish();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("waiting", mWaitingForChildActivityToFinish);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mWaitingForChildActivityToFinish = false;
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void startWaiting() {
        mWaitingForChildActivityToFinish = true;
    }
}
