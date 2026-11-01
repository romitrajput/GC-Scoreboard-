package com.gully.cricket;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.json.JSONArray;

/** Hosts the offline scorer (assets/index.html) in a WebView. Matches are kept in WebView localStorage. */
public class MainActivity extends Activity {
    private static final int PICK_FILE = 42;
    private static final int VOICE = 43;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try {
                    startActivityForResult(i, PICK_FILE);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");
        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
        } else if (requestCode == VOICE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> heard = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (heard != null && !heard.isEmpty()) {
                String json = new JSONArray(heard).toString();
                web.evaluateJavascript("window.onVoiceResult&&window.onVoiceResult(" + json + ")", null);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    private File writeShared(String name, byte[] bytes) throws Exception {
        File dir = new File(getCacheDir(), "shared");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("no cache dir");
        File f = new File(dir, name);
        try (FileOutputStream o = new FileOutputStream(f)) { o.write(bytes); }
        return f;
    }

    private void shareFile(File f, String mime, String title) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", f);
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType(mime);
        i.putExtra(Intent.EXTRA_STREAM, uri);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i, title));
    }

    /** Called from JavaScript as window.AndroidBridge.* */
    class Bridge {
        @JavascriptInterface
        public void startVoice() {
            runOnUiThread(() -> {
                Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN");
                i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
                i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say: four, six, dot, wide, no ball, out, undo");
                try {
                    startActivityForResult(i, VOICE);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Voice input not available on this phone", Toast.LENGTH_LONG).show();
                }
            });
        }

        @JavascriptInterface
        public void shareImage(String base64Png) {
            runOnUiThread(() -> {
                try {
                    File f = writeShared("gully-matchday.png", Base64.decode(base64Png, Base64.DEFAULT));
                    shareFile(f, "image/png", "Share scoreboard");
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Could not share image", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void shareText(String text, String fileName) {
            runOnUiThread(() -> {
                try {
                    if (fileName == null || fileName.isEmpty()) {
                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("text/plain");
                        i.putExtra(Intent.EXTRA_TEXT, text);
                        startActivity(Intent.createChooser(i, "Share"));
                    } else {
                        File f = writeShared(fileName, text.getBytes(StandardCharsets.UTF_8));
                        shareFile(f, "application/json", "Save backup");
                    }
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Could not share", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
