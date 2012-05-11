package chalkmark.project;

// http://www.ibiblio.org/wm/paint/auth/gogh/gogh.starry-night-rhone.jpg

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;


public class ChalkmarkInfo extends Activity 
{
	String TAG = "CHALKMARK";

    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chalkmark_info);
        
        WebView webView = (WebView) findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("http://www.chalkmarkapp.com/p/about-chalkmark.html");

   }
}