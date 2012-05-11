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
package com.janrain.android.quicksignin;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.janrain.android.engage.types.JRDictionary;

import java.util.ArrayList;

public class ProfileDetailActivity extends ListActivity {
    private static final String TAG = ProfileDetailActivity.class.getSimpleName();

    private JRDictionary mProfile;

    public ProfileDetailActivity() {
    }

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState
     *      If the activity is being re-initialized after previously being shut down then this
     *      Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     *      Note: Otherwise it is null.
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_detail_listview);

        ProfileAdapter mAdapter;
        ProfileData mProfileData;
        ArrayList<String> mProfileKeys;

        mProfileData = ProfileData.getInstance();
        mProfile = mProfileData.getCurrentProfile();

        mProfileKeys = new ArrayList<String>(mProfile.keySet());

//        if (mProfileKeys == null) {
//            mProfileKeys = new ArrayList<String>();
//        }

        mAdapter = new ProfileAdapter(this, R.layout.profile_detail_listview_row, mProfileKeys);
        setListAdapter(mAdapter);
    }

    public void onResume () {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d(TAG, "onStart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * This method will be called when an item in the list is selected.
     */
    @Override
    protected void onListItemClick(ListView l, View v, int pos, long id) {
    }

    /**
     * Initialize the contents of the Activity's standard options menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // use the shared menu
        return true;
    }

    /**
     * This hook is called whenever an item in your options menu is selected.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /**
     * Callback for creating dialogs that are managed.
     */
    protected Dialog onCreateDialog(int id) {
        return null;
    }

    /**
     * Array adapter used to render individual providers in list view.
     */
    private class ProfileAdapter extends ArrayAdapter<String> {
        private int mResourceId;

        public ProfileAdapter(Context context, int resId, ArrayList<String> items) {
            super(context, -1, items);

            mResourceId = resId;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater li = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = li.inflate(mResourceId, null);
                Log.i(TAG, "[getView] with null converView");
            } else Log.i(TAG, "[getView] with non null convertView");

            TextView keyLabel = (TextView)v.findViewById(R.id.row_profile_detail_key);
            TextView valueLabel = (TextView)v.findViewById(R.id.row_profile_detail_value);

            String key = getItem(position);

            keyLabel.setText(key);

            if (mProfile.get(key) instanceof String)
                valueLabel.setText(mProfile.getAsString(key));
            else
                valueLabel.setText("");

            return v;
        }
    }
}