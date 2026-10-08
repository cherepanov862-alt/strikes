package ru.mansionstrike;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** Full-screen WebView that runs the offline game from assets/index.html. */
public class MainActivity extends Activity {
    private WebView web;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        useFastestRefreshRate();
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        // keep the page's GPU process at foreground priority so the game is not throttled
        web.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        setContentView(web);
        hideBars();
        web.loadUrl("file:///android_asset/index.html");

        // A physical mouse: capture it so the game gets relative movement (the page's window.M bridge).
        web.setOnCapturedPointerListener((v, e) -> {
            int a = e.getActionMasked();
            float dx = e.getX(), dy = e.getY();
            if (a == MotionEvent.ACTION_SCROLL) {
                float w = e.getAxisValue(MotionEvent.AXIS_VSCROLL);
                if (w != 0) js("window.dispatchEvent(new WheelEvent('wheel',{deltaY:" + (-w * 100) + "}))");
                return true;
            }
            js("window.M&&M(" + a + "," + e.getButtonState() + "," + dx + "," + dy + ")");
            return true;
        });
        web.setOnGenericMotionListener((v, e) -> {
            if (e.isFromSource(InputDevice.SOURCE_MOUSE) && !web.hasPointerCapture()) {
                web.requestPointerCapture();
            }
            return false;
        });
    }

    /** Ask for the display's highest refresh rate (90/120/144 Hz panels) at the current resolution. */
    private void useFastestRefreshRate() {
        Display d = getWindowManager().getDefaultDisplay();
        Display.Mode cur = d.getMode(), best = cur;
        for (Display.Mode m : d.getSupportedModes()) {
            if (m.getPhysicalWidth() == cur.getPhysicalWidth() && m.getPhysicalHeight() == cur.getPhysicalHeight()
                    && m.getRefreshRate() > best.getRefreshRate()) best = m;
        }
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.preferredDisplayModeId = best.getModeId();
        lp.preferredRefreshRate = best.getRefreshRate();
        getWindow().setAttributes(lp);
    }

    private void js(String code) {
        web.evaluateJavascript(code, null);
    }

    private void hideBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideBars();
    }

    @Override
    public void onBackPressed() {
        // Back opens the in-game pause menu instead of closing the app.
        js("window.press&&press('KeyP')");
    }

    @Override
    protected void onPause() {
        super.onPause();
        web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        hideBars();
    }
}
