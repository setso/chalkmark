package com.janrain.android.simpledemo.test;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.Button;
import android.widget.EditText;

import com.janrain.android.simpledemo.MainActivity;
import com.janrain.android.simpledemo.R;

public class SimpleDemoTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private MainActivity mActivity;
    private Button mAuthButton;
    private Button mPubButton;
    private EditText mUrlEdit;
    
    public SimpleDemoTest() {
        super("com.janrain.android.simpledemo", MainActivity.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mAuthButton = (Button) mActivity.findViewById(R.id.btn_test_auth);
        mPubButton = (Button) mActivity.findViewById(R.id.btn_test_pub);
        mUrlEdit = (EditText) mActivity.findViewById(R.id.share_url);        
    }
    
    public void testPreconditions() {
        assertNotNull(mActivity);
        assertNotNull(mAuthButton);
        assertNotNull(mPubButton);
        assertNotNull(mUrlEdit);        
    }
    
    public void testTest() {
        
    }
}
