package com.checkmate.android.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;

public class MyWebView extends WebView {
    private static int mCurrentHistoryIndex = 0;
    private static ArrayList<String> mUrlHistory = new ArrayList<>();

    public static void AddHistory(String url) {
        if (!TextUtils.isEmpty(url)) {
            if (mUrlHistory.size() > 0) {
                String curUrl = mUrlHistory.get(mCurrentHistoryIndex);
                if (curUrl.equals(url))
                    return; // final url is same with new url, so don't need to add it again
            }

            for (int i = mCurrentHistoryIndex; i < mUrlHistory.size() - 1; i++) {
                mUrlHistory.remove(mUrlHistory.size() - 1);
            }

            int historyCount = mUrlHistory.size();
            for (int i = 0; i < historyCount; i++) {
                if (mUrlHistory.get(i).equals(url)) {
                    mUrlHistory.remove(i);
                    i--;
                    historyCount--;
                }
            }

            mUrlHistory.add(url);
            mCurrentHistoryIndex = mUrlHistory.size() - 1;
        }
    }

    public static String GetPrevHistory() {
        if (mCurrentHistoryIndex > 0) {
            mCurrentHistoryIndex--;
            return mUrlHistory.get(mCurrentHistoryIndex);
        }
        return "";
    }

    public static String GetNextHistory() {
        if (mCurrentHistoryIndex < mUrlHistory.size() - 1) {
            mCurrentHistoryIndex++;
            return mUrlHistory.get(mCurrentHistoryIndex);
        }
        return "";
    }

    public static String GetLastHistory() {
        if (mCurrentHistoryIndex < mUrlHistory.size()) {
            return mUrlHistory.get(mCurrentHistoryIndex);
        }
        return "";
    }

    public static void ClearHistory() {
        mCurrentHistoryIndex = 0;
        mUrlHistory.clear();
    }


    /*
     */
    public MyWebView(Context context) {
        super(context);
        // TODO Auto-generated constructor stub
        init(context);
    }

    public MyWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        init(context);
    }

    public MyWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // TODO Auto-generated constructor stub
        init(context);
    }

    public MyWebView(Context context, AttributeSet attrs, int defStyleAttr, boolean privateBrowsing) {
        super(context, attrs, defStyleAttr, privateBrowsing);
        // TODO Auto-generated constructor stub
        init(context);
    }

    private void init(Context context) {
        WebSettings ws = getSettings();
        ws.getPluginState();
        ws.setPluginState(WebSettings.PluginState.ON);
        ws.setJavaScriptEnabled(true);
        ws.setJavaScriptCanOpenWindowsAutomatically(true);
        ws.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        ws.setDisplayZoomControls(false);

        setBackgroundColor(Color.TRANSPARENT);

        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                // TODO Auto-generated method stub
                super.onPageStarted(view, url, favicon);
                if (mOnWebViewListner != null)
                    mOnWebViewListner.onPageLoadStarted(url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // TODO Auto-generated method stub
                super.onReceivedError(view, errorCode, description, failingUrl);
                final String mimeType = "text/html";
                final String encoding = "UTF-8";
                String html = "<html></html>";

                view.loadDataWithBaseURL("", html, mimeType, encoding, "");
            }
        });

        setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                // TODO Auto-generated method stub
                super.onProgressChanged(view, newProgress);
                if (mOnWebViewListner != null)
                    mOnWebViewListner.onPageLoadProgressChanged(newProgress);

                if (newProgress == 100) {
                    if (mOnWebViewListner != null) {
                        mOnWebViewListner.onPageLoadFinished();
                    }
                }
            }
        });
        setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
    }

    @Override
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        // TODO Auto-generated method stub
        if (mOnWebViewListner != null) {
            int contentHeight = (int) Math.floor(getContentHeight() * getScale());
            if (contentHeight - getScrollY() <= getHeight()) { // if diff is zero, then the bottom has been reached
                mOnWebViewListner.onScrollChanged(true);
            } else {
                mOnWebViewListner.onScrollChanged(false);
            }
        }

        super.onScrollChanged(l, t, oldl, oldt);
    }

    private OnWebViewListner mOnWebViewListner;

    public void setOnWebViewListner(OnWebViewListner listener) {
        mOnWebViewListner = listener;
    }

    public interface OnWebViewListner {
        public void onPageLoadStarted(String url);

        public void onPageLoadFinished();

        public void onPageLoadProgressChanged(int progress);

        public void onScrollChanged(boolean isHitBottom);
    }
}
