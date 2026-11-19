package com.chilyacatering.chimail;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String HOME = "https://webmail.chilya-catering.com/";
    private static final String ALLOWED_HOST = "webmail.chilya-catering.com";
    private static final int FILE_CHOOSER_REQUEST = 7101;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(18,18,18));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(18,18,18));
        webView = new WebView(this);
        webView.setBackgroundColor(android.graphics.Color.WHITE);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(webView, false);
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return route(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return route(Uri.parse(url));
            }
            private boolean route(Uri uri) {
                String scheme = uri.getScheme() == null ? "" : uri.getScheme();
                String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
                if ("https".equalsIgnoreCase(scheme) && (ALLOWED_HOST.equals(host) || host.endsWith("." + ALLOWED_HOST))) return false;
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Ouvrir le lien externe ?")
                        .setMessage(uri.toString())
                        .setNegativeButton("Annuler", (d,w) -> d.dismiss())
                        .setPositiveButton("Ouvrir", (d,w) -> {
                            try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                            catch (ActivityNotFoundException e) { Toast.makeText(MainActivity.this, "Aucune application compatible", Toast.LENGTH_SHORT).show(); }
                        }).show();
                    return true;
                }
                return true;
            }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                Toast.makeText(MainActivity.this, "Connexion HTTPS non sécurisée : accès bloqué.", Toast.LENGTH_LONG).show();
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = params.createIntent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try { startActivityForResult(intent, FILE_CHOOSER_REQUEST); }
                catch (ActivityNotFoundException e) { fileCallback = null; callback.onReceiveValue(null); Toast.makeText(MainActivity.this, "Sélecteur de fichiers indisponible", Toast.LENGTH_SHORT).show(); return false; }
                return true;
            }
        });
        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            Uri uri = Uri.parse(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !(ALLOWED_HOST.equals(uri.getHost()) || (uri.getHost() != null && uri.getHost().endsWith("." + ALLOWED_HOST)))) {
                Toast.makeText(this, "Téléchargement externe bloqué", Toast.LENGTH_LONG).show();
                return;
            }
            try {
                DownloadManager.Request request = new DownloadManager.Request(uri);
                String cookie = CookieManager.getInstance().getCookie(url);
                if (cookie != null) request.addRequestHeader("Cookie", cookie);
                if (userAgent != null) request.addRequestHeader("User-Agent", userAgent);
                request.setMimeType(mimeType);
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType));
                request.setDescription("Téléchargement CHI MAIL");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimeType));
                ((DownloadManager)getSystemService(DOWNLOAD_SERVICE)).enqueue(request);
                Toast.makeText(this, "Téléchargement lancé", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Échec du téléchargement", Toast.LENGTH_LONG).show();
            }
        });
        setContentView(webView);
        if (state == null) webView.loadUrl(HOME); else webView.restoreState(state);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && fileCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i=0; i<count; i++) results[i] = data.getClipData().getItemAt(i).getUri();
                } else if (data.getData() != null) results = new Uri[]{data.getData()};
            }
            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (webView != null) webView.saveState(out);
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
        if (webView != null) { webView.stopLoading(); webView.setWebChromeClient(null); webView.setWebViewClient(null); webView.destroy(); }
        super.onDestroy();
    }
}
