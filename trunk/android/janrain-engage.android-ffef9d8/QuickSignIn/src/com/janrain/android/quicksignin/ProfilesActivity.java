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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Config;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.janrain.android.engage.JREngage;
import com.janrain.android.engage.JREngageDelegate;
import com.janrain.android.engage.JREngageError;
import com.janrain.android.engage.net.async.HttpResponseHeaders;
import com.janrain.android.engage.session.JRSession;
import com.janrain.android.engage.types.JRActivityObject;
import com.janrain.android.engage.types.JRDictionary;

import java.util.ArrayList;
import java.util.List;

import static com.janrain.android.quicksignin.QuickSignInEnvironment.getAppId;
import static com.janrain.android.quicksignin.QuickSignInEnvironment.getTokenUrl;

public class ProfilesActivity extends ListActivity implements View.OnClickListener, JREngageDelegate {

    private static final String TAG = ProfilesActivity.class.getSimpleName();

    private static String ENGAGE_APP_ID = getAppId();
    private static String ENGAGE_TOKEN_URL = getTokenUrl();

    private static final int DIALOG_JRENGAGE_ERROR = 1;

    private List<LoginSnapshot> mProfilesList;
    private ProfileAdapter mAdapter;
    private ProfileData mProfileData;

    private JREngage mEngage;

    private Button mAddProfile;
    private boolean mEditing;
    private String mDialogErrorMessage;

    public ProfilesActivity() {
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
        if (Config.LOGD)
            Log.d(TAG, "[onCreate]");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.profiles_listview);

        mEngage = JREngage.initInstance(this, ENGAGE_APP_ID, ENGAGE_TOKEN_URL, this);
        //mEngage.setAlwaysForceReauthentication(true);

        mEditing = false;

        mAddProfile = (Button)findViewById(R.id.btn_add_profile);
        mAddProfile.setOnClickListener(this);

        mProfileData = ProfileData.getInstance();
        mProfilesList = mProfileData.getProfilesList();

        if (mProfilesList == null) {
            mProfilesList = new ArrayList<LoginSnapshot>();
        }

        mAdapter = new ProfileAdapter(this, R.layout.profiles_listview_row, mProfilesList);
        setListAdapter(mAdapter);

        if (mProfilesList.size() == 0) {

        }
    }

    public void onResume () {
        super.onResume();
    }

    @Override
    protected void onStart() {
        if (Config.LOGD)
            Log.d(TAG, "[onStart]");

        super.onStart();
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
        Log.d(TAG, "[onListItemClick] at position: " + ((Integer)pos).toString());

        LoginSnapshot snapshot = mAdapter.getItem(pos);
        mProfileData.setCurrentProfileByIdentifier(snapshot.getIdentifier());
        this.startActivity(new Intent(this, ProfileDetailActivity.class));
    }

    /*
     * Array adapter used to render individual providers in list view.
     */
    private class ProfileAdapter extends ArrayAdapter<LoginSnapshot> implements View.OnClickListener {
        private int mResourceId;

        public ProfileAdapter(Context context, int resId, List<LoginSnapshot> items) {
            super(context, -1, items);

            mResourceId = resId;
        }

        private Drawable getProviderIconDrawable(String providerName) {
            return JRSession.getInstance().getProviderByName(providerName)
                    .getProviderIcon(ProfilesActivity.this);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                LayoutInflater li = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = li.inflate(mResourceId, null);
                Log.i(TAG, "[getView] with null convertView");
            } else {
                Log.i(TAG, "[getView] with non null convertView");
            }

            ImageView icon = (ImageView)v.findViewById(R.id.row_profile_provider_icon);
            TextView name = (TextView)v.findViewById(R.id.row_profile_preferred_username_label);
            TextView timestamp = (TextView)v.findViewById(R.id.row_profile_timestamp_label);
            Button deleteRow = (Button)v.findViewById(R.id.row_delete_button);

            LoginSnapshot snapshot = getItem(position);

            Log.d(TAG, "[getView] for row " + ((Integer) position).toString() + ": " +
                    snapshot.getDisplayName());

            icon.setImageDrawable(getProviderIconDrawable(snapshot.getProvider()));
            name.setText(snapshot.getDisplayName());
            timestamp.setText(snapshot.getTimeStamp());

            deleteRow.setTag(position);
            deleteRow.setOnClickListener(this);

            if (mEditing)
                deleteRow.setVisibility(View.VISIBLE);
            else
                deleteRow.setVisibility(View.GONE);

            return v;
        }

        public void onClick(View view) {
            Integer position = ((Integer)view.getTag());
            mProfileData.deleteLoginSnapshotAtPosition(position);
            this.notifyDataSetChanged();
        }
    }

    /**
     * Initialize the contents of the Activity's standard options menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.profile_managing_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!mEditing)
            menu.findItem(R.id.edit_profiles).setTitle(R.string.edit_profiles);
        else
            menu.findItem(R.id.edit_profiles).setTitle(R.string.done_editing);

        return true;
    }

    /**
    * This hook is called whenever an item in your options menu is selected.
    */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_profiles:
                if (!mEditing) {
                    mEditing = true;
                    mAddProfile.setText(R.string.done_editing);
                    mAdapter.notifyDataSetChanged();
                    return true;
                }
                else {
                    mEditing = false;
                    mAddProfile.setText(R.string.add_another_profile);
                    mAdapter.notifyDataSetChanged();
                    return true;
                }
            case R.id.delete_all_profiles:
                mEditing = false;
                mAddProfile.setText(R.string.add_another_profile);
                mProfileData.deleteAllProfiles();
                mAdapter.notifyDataSetChanged();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DIALOG_JRENGAGE_ERROR:
                return new AlertDialog.Builder(this)
                    .setPositiveButton("Dismiss", null)
                    .setCancelable(false)
                    .setMessage(mDialogErrorMessage)
                    .create();
        }

        throw new RuntimeException("unknown dialogId");
    }

    public void jrEngageDialogDidFailToShowWithError(JREngageError error) {
        Log.d(TAG, "[jrEngageDialogDidFailToShowWithError]");

        mDialogErrorMessage = "Authentication Error: " +
                ((error == null) ? "unknown" : error.getMessage());

        showDialog(DIALOG_JRENGAGE_ERROR);
    }

    public void jrAuthenticationDidSucceedForUser(JRDictionary auth_info, String provider) {
        JRDictionary profile = (auth_info == null) ? null : auth_info.getAsDictionary("profile");
        String displayName = (profile == null) ? null : profile.getAsString("displayName");
        String message = "Authentication successful" + ((TextUtils.isEmpty(displayName))
                ? "" : (" for user: " + displayName));

        mProfileData.addProfile(auth_info, provider);
        mAdapter.notifyDataSetChanged();
                    
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void jrAuthenticationDidReachTokenUrl(String tokenUrl,
                                                 HttpResponseHeaders response,
                                                 String tokenUrlPayload,
                                                 String provider) {
        Toast.makeText(this, "Authentication did reach token url", Toast.LENGTH_SHORT).show();
    }

    public void jrAuthenticationDidNotComplete() {
        Toast.makeText(this, "Authentication did not complete", Toast.LENGTH_SHORT).show();
    }

    public void jrAuthenticationDidFailWithError(JREngageError error, String provider) {
        String message = "Authentication failed, error: " +
                ((error == null) ? "unknown" : error.getMessage());

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void jrAuthenticationCallToTokenUrlDidFail(String tokenUrl, JREngageError error, String provider) {
        Toast.makeText(this, "Authentication failed to reach token url", Toast.LENGTH_SHORT).show();
    }

    public void jrSocialDidNotCompletePublishing() { }

    public void jrSocialDidCompletePublishing() { }

    public void jrSocialDidPublishJRActivity(JRActivityObject activity, String provider) {
    }

    public void jrSocialPublishJRActivityDidFail(JRActivityObject activity,
                                                 JREngageError error,
                                                 String provider) {
    }

    public void onClick(View view) {
        if (mEditing) {
            mEditing = false;
            mAddProfile.setText(R.string.add_another_profile);
            mAdapter.notifyDataSetChanged();
        } else {
            /* To see an example of how you can force the user to always reauthenticate and skip the
             * returning user landing page, uncomment the following two lines, and comment-out the third */
            /* mEngage.setAlwaysForceReauthentication(true); */
            /* mEngage.showAuthenticationDialog(true); */
            mEngage.showAuthenticationDialog();
        }
    }
}