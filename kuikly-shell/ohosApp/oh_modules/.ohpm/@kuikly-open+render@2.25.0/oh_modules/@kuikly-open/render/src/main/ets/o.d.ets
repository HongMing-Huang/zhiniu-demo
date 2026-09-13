// @keepTs
// @ts-nocheck
import { KRExceptionCallback } from "./h1/j1";
import { KRAny, KRRecord, KRValue } from "./e/f";
import x6 from "@ohos.app.ability.common";
import { KRRect } from "./o1/p1";
import { KRRenderViewExportCreator } from "./p/d1/e1";
import { KRRenderModuleExportCreator } from "./m/d1/t1";
import { Configuration } from '@ohos.app.ability.Configuration';
import { KRRenderExecuteModeBase } from "./u/x";
/** window尺寸变化事件名 */
export declare const KRWindowSizeDidChangedEventKey = "windowSizeDidChanged";
/** RootView尺寸变化事件名. */
export declare const KRRootViewSizeDidChangedEventKey = "rootViewSizeDidChanged";
/** 页面可见事件 */
export declare const KRPageDidAppearEventKey = "viewDidAppear";
/** 页面不可见事件 */
export declare const KRPageDidDisappearEventKey = "viewDidDisappear";
export declare const KRConfigurationUpdateEventKey = "configurationDidChanged";
/** 字典key常量 */
export declare const KRRootViewWidthKey = "rootViewWidth";
export declare const KRRootViewHeightKey = "rootViewHeight";
export declare const KRUrlKey = "url";
export declare const KRStatusBarHeightKey = "statusBarHeight";
export declare const KRPlatformKey = "platform";
export declare const KRDeviceWidthKey = "deviceWidth";
export declare const KRDeviceHeightKey = "deviceHeight";
export declare const KRActivityWidthKey = "activityWidth";
export declare const KRActivityHeightKey = "activityHeight";
export declare const KROsVersionKey = "osVersion";
export declare const KRAppVersionKey = "appVersion";
export declare const KRParamKey = "param";
export declare const KRWidthKey = "width";
export declare const KRHeightKey = "height";
export declare const KRNativeBuild = "nativeBuild";
export declare const KRSafeAreaInsets = "safeAreaInsets";
export declare const KRDensityInfo = "densityInfo";
export declare const KRExecuteMode = "executeMode";
export declare const KRContextCode = "contextCode";
export declare const KRDensity = "density";
export declare const KRSystemLanguageKey = "sysLang";
export declare const KRFeature = "feature";
export declare class KRNativeRenderController {
    /**
     * 页面实例 id
     */
    instanceId: string;
    /**
     * 执行上下文code，驱动渲染对应的代码。KRRenderExecuteMode.Native模式时: 传递 ''
     */
    contextCode: string;
    /**
     * assets资源的实际路径，如未设置，默认在resfile中找assets资源
     */
    assetsDir: string;
    /**
     * 执行模式
     */
    executeMode: KRRenderExecuteModeBase;
    /**
     * 页面名字
     */
    pageName: string;
    /**
     * 透传给KR侧的页面数据
     */
    pageParams: KRRecord;
    /**
     * 鸿蒙侧，向用户提供新旧两种KRSharedPreferencesMoudle使用的控制开关（变量）
     */
    useOhSharedPreferences: boolean;
    /**
     * 所在的窗口Id
     */
    windowId: string;
    /**
     * SizeChanged 执行时机是否需提前至 onSizeChanged 而非 onAreaChanged，以避免页面 Size 变化时出现白屏
     */
    private sizeChangeHandledByOnSizeChange;
    private lifecycleCallbacks;
    private fontSizeScale;
    private fontWeightScale;
    /**
     * 输入法模式
     * 在输入法中，获取不到window和safearea，所以只有有view rect后就可以初始化kuikly
     */
    imeMode: boolean;
    private environmentCallbackId;
    private densityPixels;
    private uiContext;
    private uiAbilityContext;
    private didInit;
    private lazyTasks;
    /**
     * 外部自定义存放在Controller中的数据, 在Module中，可通过Controller来获取数据
     */
    private externalParams;
    /**
     * 外部自定义的Controller, 可设置此值，然后在Module中获取
     */
    private externalController;
    private safeAreaRect;
    private viewRect;
    private windowRect;
    private updateSafeInsetsHandler;
    private windowClass;
    private destroyed;
    private onWindowSizeChanged;
    private avoidAreaListener;
    private doUpdateSafeArea;
    init(y10: UIContext, z10: x6.UIAbilityContext, a11: KRRenderExecuteModeBase, b11: string, c11: KRRecord, d11: string, e11: boolean, f11: string, g11: boolean, h11?: KRRect): void;
    /**
     * 未捕获异常回调
     * @param exceptionCallback
     */
    registerExceptionCallback(x10: KRExceptionCallback): void;
    onViewSizeChanged(t10: KRRect, u10?: number, v10?: number): void;
    notifyWindowSizeChanged(q10: number, r10: number): void;
    paramsReady(): boolean;
    onConfigurationUpdate(m10: Configuration): void;
    onSafeAreaInsetsChanged(l10: KRRect): void;
    private encodeSafeAreaInsetsString;
    private notifySizeChange;
    onAttach(): void;
    private isViewSizeChanged;
    onAreaChange(z9: Area): void;
    onSizeChange(w9: SizeOptions): void;
    onAboutToDisappear(): void;
    onPageShow(): void;
    private doOnDensityChanged;
    onPageHide(): void;
    sendEvent(n9: string, o9: KRRecord): void;
    sendEventSync(k9: string, l9: KRRecord, m9: boolean): void;
    onBackPress(): boolean;
    putParam(g9: string, h9: KRValue): void;
    getParam(f9: string): KRAny | null;
    setExternalController(e9: object): void;
    getExternalController<d9>(): d9;
    getUIAbilityContext(): x6.UIAbilityContext | null;
    private doOnPageShow;
    private doOnPageHide;
    syncSendEvent(a9: string): boolean;
    private doSendEvent;
    private doOnAttach;
    private doDidInit;
    addKuiklyRenderViewLifecycleCallback(t8: IKuiklyRenderViewLifecycleCallback): void;
    dispatchLifecycleStateChanged(j8: number, k8?: Object): void;
    private doAboutToDisappear;
    private doOnAreaChange;
    private createPageParams;
    private createConfigJson;
    private runKRRenderTask;
    private runLazyTasksOnce;
    private getSafeAreaRect;
    /**
     * 获取窗口活跃的density
     * customDensity的值默认为systemDensity
     * 如果windowStage主动设置了DefaultDensityEnabled/CustomDensity,则分别是defaultDensity/customDensity
     * 因此默认取customDensity作为活跃的density
     */
    private getWindowActiveDensity;
    /**
     * 字体缩放是否跟随系统
     * @return boolean true:跟随系统(默认) false:不跟随系统(缩放比例为1)
     */
    fontSizeScaleFollowSystem(): boolean;
    /**
     * 回调启动数据
     */
    onGetLaunchData(m7: KRRecord): void;
    /**
     * 回调性能数据
     */
    onGetPerformanceData(l7: KRRecord): void;
    /**
     * Kuikly框架设置性能监控选项，默认只开启动监控
     * @return Array<KRMonitorType>: 需要设置的性能监控选项列表
     */
    performanceMonitorTypes(): Array<KRMonitorType>;
    private getMonitorTypeMask;
}
export declare abstract class IKuiklyViewDelegate extends KRNativeRenderController {
    /**
     * 获取自定义扩展渲染视图创建注册Map
     */
    abstract getCustomRenderViewCreatorRegisterMap(): Map<string, KRRenderViewExportCreator>;
    /**
     * 获取自定义扩展渲染视图创建注册Map。
     * 通过这个方式注册的creator，创建的自定义view将不会有影子节点处理基础事件，需要用户在arkts侧响应所有属性的设置。
     * 当这个方式的好处是，由于不存在影子节点，其view曾经和DSL中定义的是保持一致的。
     * 建议仅在有强一致层级需求的时候才采用。
     */
    getCustomRenderViewCreatorRegisterMapV2(): Map<string, KRRenderViewExportCreator>;
    /**
     * 获取自定义扩展module创建注册Map
     */
    abstract getCustomRenderModuleCreatorRegisterMap(): Map<string, KRRenderModuleExportCreator>;
    pageDidAppear(): void;
    pageDidDisappear(): void;
    /**
     * 获取自定义[KuiklyRenderView]生命周期回调
     */
    getKuiklyRenderViewLifecycleCallback(): IKuiklyRenderViewLifecycleCallback | null;
}
/**
 * RenderView 生命周期回调
 */
export interface IKuiklyRenderViewLifecycleCallback {
    /**
     * 启动
     */
    onInit(): void;
    /**
     * renderCore 初始化开始
     */
    onInitCoreStart(): void;
    /**
     * renderCore 初始化完成
     */
    onInitCoreFinish(): void;
    /**
     * 初始化渲染环境开始
     */
    onInitContextStart(): void;
    /**
     * 初始化渲染环境完成
     */
    onInitContextFinish(): void;
    /**
     * 创建页面开始
     */
    onCreateInstanceStart(): void;
    /**
     * 创建页面结束
     */
    onCreateInstanceFinish(): void;
    /**
     * 页面首帧渲染完成
     */
    onFirstFramePaint(): void;
}
/**
 * 性能监控类型
 */
export declare enum KRMonitorType {
    LAUNCH = 1,
    FRAME = 2,
    MEMORY = 4
}
