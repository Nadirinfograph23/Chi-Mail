package com.chilyacatering.chimail;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String HOME = "https://webmail.chilya-catering.com/";
    private static final String ALLOWED_HOST = "webmail.chilya-catering.com";
    private static final int FILE_CHOOSER_REQUEST = 7101;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;

    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(9,9,9));
        getWindow().setNavigationBarColor(Color.rgb(9,9,9));
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(9,9,9));

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(14), 0, dp(12), 0);
        bar.setBackgroundColor(Color.rgb(9,9,9));
        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60)));

        ImageView logo = new ImageView(this);
        logo.setImageResource(com.chilyacatering.chimail.R.mipmap.chilya_icon_foreground);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        bar.addView(logo, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout brandText = new LinearLayout(this);
        brandText.setOrientation(LinearLayout.VERTICAL);
        brandText.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams brandParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        brandParams.setMargins(dp(11), 0, dp(8), 0);
        bar.addView(brandText, brandParams);

        TextView title = new TextView(this);
        title.setText("CHI MAIL");
        title.setTextColor(Color.rgb(255,210,26));
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        brandText.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("CHILYA CATERING");
        subtitle.setTextColor(Color.rgb(180,180,180));
        subtitle.setTextSize(9);
        brandText.addView(subtitle);

        Button refresh = new Button(this);
        refresh.setText("↻");
        refresh.setTextSize(22);
        refresh.setTextColor(Color.rgb(255,210,26));
        refresh.setAllCaps(false);
        refresh.setMinWidth(0);
        refresh.setMinimumWidth(dp(44));
        refresh.setPadding(0, 0, 0, 0);
        GradientDrawable buttonBg = new GradientDrawable();
        buttonBg.setColor(Color.rgb(35,35,35));
        buttonBg.setCornerRadius(dp(10));
        refresh.setBackground(buttonBg);
        bar.addView(refresh, new LinearLayout.LayoutParams(dp(44), dp(42)));

        View accent = new View(this);
        accent.setBackgroundColor(Color.rgb(255,210,26));
        root.addView(accent, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        root.addView(webView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        refresh.setOnClickListener(v -> webView.reload());

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
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return route(request.getUrl()); }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) { return route(Uri.parse(url)); }
            private boolean route(Uri uri) {
                String scheme = uri.getScheme() == null ? "" : uri.getScheme();
                String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
                if ("https".equalsIgnoreCase(scheme) && (ALLOWED_HOST.equals(host) || host.endsWith("." + ALLOWED_HOST))) return false;
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    new AlertDialog.Builder(MainActivity.this).setTitle("Ouvrir le lien externe ?")
                        .setMessage(uri.toString()).setNegativeButton("Annuler", (d,w) -> d.dismiss())
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
                catch (ActivityNotFoundException e) {
                    fileCallback = null; callback.onReceiveValue(null);
                    Toast.makeText(MainActivity.this, "Sélecteur de fichiers indisponible", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });

        webView.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) -> {
            Uri uri = Uri.parse(url);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !(ALLOWED_HOST.equals(uri.getHost()) || (uri.getHost() != null && uri.getHost().endsWith("." + ALLOWED_HOST)))) {
                Toast.makeText(this, "Téléchargement externe bloqué", Toast.LENGTH_LONG).show(); return;
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
            } catch (Exception e) { Toast.makeText(this, "Échec du téléchargement", Toast.LENGTH_LONG).show(); }
        });

        setContentView(root);
        if (state == null) webView.loadUrl(HOME); else webView.restoreState(state);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER_REQUEST && fileCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount(); results = new Uri[count];
                    for (int i=0; i<count; i++) results[i] = data.getClipData().getItemAt(i).getUri();
                } else if (data.getData() != null) results = new Uri[]{data.getData()};
            }
            fileCallback.onReceiveValue(results); fileCallback = null;
        }
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); if (webView != null) webView.saveState(out);
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (fileCallback != null) { fileCallback.onReceiveValue(null); fileCallback = null; }
        if (webView != null) { webView.stopLoading(); webView.setWebChromeClient(null); webView.setWebViewClient(null); webView.destroy(); }
        super.onDestroy();
    }
}