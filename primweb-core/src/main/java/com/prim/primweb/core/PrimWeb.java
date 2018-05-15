package com.prim.primweb.core;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import com.prim.primweb.core.client.DefaultAgentWebViewClient;
import com.prim.primweb.core.client.IAgentWebViewClient;
import com.prim.primweb.core.jsinterface.IJsInterface;
import com.prim.primweb.core.jsinterface.SafeJsInterface;
import com.prim.primweb.core.jsloader.ICallJsLoader;
import com.prim.primweb.core.jsloader.SafeCallJsLoaderImpl;
import com.prim.primweb.core.setting.DefaultWebSetting;
import com.prim.primweb.core.setting.IAgentWebSetting;
import com.prim.primweb.core.urlloader.IUrlLoader;
import com.prim.primweb.core.urlloader.UrlLoader;
import com.prim.primweb.core.utils.PrimWebUtils;
import com.prim.primweb.core.webview.IAgentWebView;
import com.prim.primweb.core.webview.PrimAgentWebView;
import com.tencent.smtt.sdk.QbSdk;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * ================================================
 * 作    者：linksus
 * 版    本：1.0
 * 创建日期：5/11 0011
 * 描    述：代理webview的总入口
 * 修订历史：
 * ================================================
 */
public class PrimWeb {

    private ICallJsLoader callJsLoader;

    private IAgentWebView webView;
    private ViewGroup mViewGroup;
    private ViewGroup.LayoutParams mLayoutParams;
    private WeakReference<Context> context;
    private int mIndex = 0;
    private IAgentWebSetting setting;
    private Map<String, String> headers;
    private IUrlLoader urlLoader;
    private View mView;

    /**
     * 设置webview的模式 分为
     * Strict - 严格的模式：api小于17 禁止注入js,大于 17 注入js的对象所有方法必须都包含JavascriptInterface注解
     * Normal - 为正常模式
     */
    public enum ModeType {
        Strict, Normal
    }

    private ModeType modeType = ModeType.Normal;

    private static final String TAG = "PrimWeb";

    private IJsInterface mJsInterface;

    private IAgentWebViewClient agentWebViewClient;

    //初始化个数 30 * 0.75 多个左右,考虑到哈希表默认大小只有 4 * 0.75 个
    //而哈希表的缺点是:扩容性能会下降 初始化时提前计算好上限.
    private HashMap<String, Object> mJavaObject = new HashMap<>(30);

    public static void init(Application application) {
        // X5浏览器实列化
        QbSdk.initX5Environment(application, null);
    }

    PrimWeb(PrimBuilder builder) {
        this.webView = builder.webView;
        this.mView = builder.mView;
        this.mViewGroup = builder.mViewGroup;
        this.mLayoutParams = builder.mLayoutParams;
        this.context = builder.context;
        this.mIndex = builder.mIndex;
        this.setting = builder.setting;
        this.headers = builder.headers;
        this.urlLoader = builder.urlLoader;
        this.callJsLoader = builder.callJsLoader;
        this.modeType = builder.modeType;
        this.agentWebViewClient = builder.agentWebViewClient;
        if (builder.mJavaObject != null && !builder.mJavaObject.isEmpty()) {
            this.mJavaObject.putAll(builder.mJavaObject);
        }
        if (mViewGroup != null) {
            mViewGroup.addView(mView, mIndex, mLayoutParams);
        } else {//考虑到极端的情况
            PrimWebUtils.scanForActivity(context.get()).setContentView(mView);
        }
        doCheckSafe();
    }

    /** webview 安全检查 */
    private void doCheckSafe() {

    }

    /** 调用js方法 */
    public ICallJsLoader callJsLoader() {
        if (null == webView) {
            throw new NullPointerException("webView most not be null,please check your code!");
        }
        if (callJsLoader == null) {
            callJsLoader = SafeCallJsLoaderImpl.getInstance(webView);
        }
        return callJsLoader;
    }

    /** 注入js脚本 */
    public IJsInterface getJsInterface() {
        if (null == webView) {
            throw new NullPointerException("webView most not be null,please check your code!");
        }
        if (mJsInterface == null) {
            mJsInterface = SafeJsInterface.getInstance(webView, modeType);
        }
        return mJsInterface;
    }

    /** 准备阶段 */
    void ready() {
        // 加载设置
        if (null == setting) {
            setting = new DefaultWebSetting(context.get());
        }
        setting.setSetting(webView);

        // 加载url加载器
        if (null == urlLoader) {
            urlLoader = new UrlLoader(webView);
        }

        // 加载js脚本注入
        if (null == mJsInterface) {
            mJsInterface = SafeJsInterface.getInstance(webView, modeType);
        }

        if (mJavaObject != null && !mJavaObject.isEmpty()) {
            mJsInterface.addJavaObjects(mJavaObject);
        }

        // 加载webViewClient
        if (null == agentWebViewClient) {
            agentWebViewClient = new DefaultAgentWebViewClient(context.get());
        }
        webView.setAgentWebViewClient(agentWebViewClient);
    }

    /** 发起阶段 */
    PrimWeb launch(String url) {
        if (null == headers || headers.isEmpty()) {
            urlLoader.loadUrl(url);
        } else {
            urlLoader.loadUrl(url, headers);
        }
        return this;
    }

    public static PrimBuilder with(Context context) {
        if (context == null) {
            throw new NullPointerException("context can not be null");
        }
        return new PrimBuilder(context);
    }

    public static class PrimBuilder {
        private IAgentWebView webView;
        private View mView;
        private WeakReference<Context> context;
        private ViewGroup mViewGroup;
        private ViewGroup.LayoutParams mLayoutParams;
        private int mIndex;
        private IAgentWebSetting setting;
        private Map<String, String> headers;
        private IUrlLoader urlLoader;
        private ICallJsLoader callJsLoader;
        private ModeType modeType = ModeType.Normal;
        private HashMap<String, Object> mJavaObject;
        private IAgentWebViewClient agentWebViewClient;

        PrimBuilder(Context context) {
            this.context = new WeakReference<>(context);
            this.webView = new PrimAgentWebView(this.context.get());
            this.mView = webView.getAgentWebView();
        }

        /** 设置webview的父类 */
        public CommonBuilder setWebParent(@NonNull ViewGroup v, @NonNull ViewGroup.LayoutParams lp) {
            this.mViewGroup = v;
            this.mLayoutParams = lp;
            return new CommonBuilder(this);
        }

        public CommonBuilder setWebParent(@NonNull ViewGroup v, @NonNull ViewGroup.LayoutParams lp, int index) {
            this.mViewGroup = v;
            this.mLayoutParams = lp;
            this.mIndex = index;
            return new CommonBuilder(this);
        }

        /** 所有设置完成 */
        public PerBuilder build() {
            if (mViewGroup == null) {
                throw new NullPointerException("ViewGroup not null,please check your code!");
            }
            return new PerBuilder(new PrimWeb(this));
        }

        private void addJavaObject(String key, Object o) {
            if (mJavaObject == null) {
                mJavaObject = new HashMap<>(30);//初始化个数 30 * 0.75 多个左右,考虑到哈希表 默认大小只有 4 * 0.75 个
                // 而哈希表的缺点是:扩容性能会下降 初始化时提前计算好上限.
            }
            mJavaObject.put(key, o);
        }
    }

    public static class CommonBuilder {
        private PrimBuilder primBuilder;

        public CommonBuilder(PrimBuilder primBuilder) {
            this.primBuilder = primBuilder;
        }

        /** 设置代理的webview 若不设置使用默认的 */
        public CommonBuilder setAgentWebView(IAgentWebView webView) {
            primBuilder.webView = webView;
            primBuilder.mView = webView.getAgentWebView();
            return this;
        }

        /** web的代理设置 */
        public CommonBuilder setAgentWebSetting(IAgentWebSetting agentWebSetting) {
            primBuilder.setting = agentWebSetting;
            return this;
        }

        /** 设置url加载器 */
        public CommonBuilder setUrlLoader(IUrlLoader urlLoader) {
            primBuilder.urlLoader = urlLoader;
            return this;
        }

        /** 设置js 方法加载器 */
        public CommonBuilder setCallJsLoader(ICallJsLoader callJsLoader) {
            primBuilder.callJsLoader = callJsLoader;
            return this;
        }

        /** 设置模式 */
        public CommonBuilder setModeType(ModeType modeType) {
            primBuilder.modeType = modeType;
            return this;
        }

        /** 注入js */
        public CommonBuilder addJavascriptInterface(@NonNull String name, @NonNull Object o) {
            primBuilder.addJavaObject(name, o);
            return this;
        }

        /** 设置WebViewClient */
        public CommonBuilder setWebViewClient(IAgentWebViewClient agentWebViewClient) {
            primBuilder.agentWebViewClient = agentWebViewClient;
            return this;
        }

        /** 设置完成开始建造 */
        public PerBuilder build() {
            return primBuilder.build();
        }
    }

    /** 设置完成准备发射 */
    public static class PerBuilder {
        private PrimWeb primWeb;
        private boolean isReady = false;

        public PerBuilder(PrimWeb primWeb) {
            this.primWeb = primWeb;
        }

        public PerBuilder ready() {
            if (!isReady) {
                primWeb.ready();
                isReady = true;
            }
            return this;
        }

        public PrimWeb launch(@NonNull String url) {
            if (!isReady) {
                ready();
            }
            return primWeb.launch(url);
        }
    }
}