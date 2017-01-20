package com.medlinker.hybridsdk.core;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.medlinker.hybridsdk.action.HybridAction;
import com.medlinker.hybridsdk.utils.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by vane on 16/6/2.
 */

public class HyBridWebViewClient extends WebViewClient {

    private WebView mWebView;

    public HyBridWebViewClient(WebView webView) {
        this.mWebView = webView;

    }

    private String mFilterHost;

    public void setHostFilter(String host) {
        mFilterHost = host;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        //TODO 需要重新讨论协议标准,建议url路径和本地的压缩包目录结构相同
        String tempUrl = url.replace("/webapp", "");
        //-----------------------------------------
        Uri uri = Uri.parse(tempUrl);
        File file = new File(FileUtil.getRootDir(view.getContext()).getAbsolutePath() + "/"
                + HybridConfig.FILE_HYBRID_DATA_PATH + "/" + uri.getPath());
        if (mFilterHost.equals(uri.getHost()) && file.exists()) {
            WebResourceResponse response = null;
            try {
                InputStream localCopy = new FileInputStream(file);
                String mimeType = getMimeType(tempUrl);
                response = new WebResourceResponse(mimeType, "UTF-8", localCopy);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }
        return super.shouldInterceptRequest(view, url);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(final WebView view, final WebResourceRequest request) {

        if (request != null && request.getUrl()!= null) {
            String scheme = request.getUrl().getScheme().trim();

            //TODO 需要重新讨论协议标准,建议url路径和本地的压缩包目录结构相同
            String tempUrl = request.getUrl().getPath().replace("/webapp", "");
            final Uri uri = Uri.parse(tempUrl);

            File file = new File(FileUtil.getRootDir(view.getContext()).getAbsolutePath() + "/"
                    + HybridConfig.FILE_HYBRID_DATA_PATH + "/" + uri.getPath());

            //如何存在本地缓存，则从缓存中获取
            if (mFilterHost.equals(uri.getHost()) && file.exists()) {
                WebResourceResponse response = null;
                try {
                    InputStream localCopy = new FileInputStream(file);
                    String mimeType = getMimeType(tempUrl);
                    response = new WebResourceResponse(mimeType, "UTF-8", localCopy);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }

            if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) {
                return super.shouldInterceptRequest(view, new WebResourceRequest() {
                    @Override
                    public Uri getUrl() {
                        return uri;
                    }

                    @Override
                    public boolean isForMainFrame() {
                        return request.isForMainFrame();
                    }

                    @Override
                    public boolean hasGesture() {
                        return request.hasGesture();
                    }

                    @Override
                    public String getMethod() {
                        return request.getMethod();
                    }

                    @Override
                    public Map<String, String> getRequestHeaders() {
                        return request.getRequestHeaders();
                    }
                });
            }
        }
        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        Uri parse = Uri.parse(url);
        String scheme = parse.getScheme();
        if (HybridConfig.SCHEME.equals(scheme)) {
            String host = parse.getHost();
            String param = parse.getQueryParameter(HybridConstant.GET_PARAM);
            String callback = parse.getQueryParameter(HybridConstant.GET_CALLBACK);
            if (null == HybridConfig.TagnameMapping.mapping(host)) {
                return super.shouldOverrideUrlLoading(view, url);
            }
            try {
                hybridDispatcher(host, param, callback);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
            return false;
        }
        view.loadUrl(url);
        return false;
    }

    private void hybridDispatcher(String method, String params, String jsmethod)
            throws IllegalAccessException, InstantiationException {
        Class type = HybridConfig.TagnameMapping.mapping(method);
        HybridAction action = (HybridAction) type.newInstance();
        action.onAction(mWebView, params, jsmethod);
    }

    private String getMimeType(String url) {
        if (url.contains(".")) {
            int index = url.lastIndexOf(".");
            if (index > -1) {
                int paramIndex = url.indexOf("?");
                String type = url.substring(index + 1, paramIndex == -1 ? url.length() : paramIndex);
                switch (type) {
                    case "js":
                        return "text/javascript";
                    case "css":
                        return "text/css";
                    case "html":
                        return "text/html";
                    case "png":
                        return "image/png";
                    case "jpg":
                        return "image/jpg";
                    case "gif":
                        return "image/gif";
                }
            }
        }
        return "text/plain";
    }
}
