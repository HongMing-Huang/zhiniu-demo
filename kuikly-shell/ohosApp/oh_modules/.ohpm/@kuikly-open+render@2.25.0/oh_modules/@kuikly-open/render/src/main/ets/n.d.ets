// @keepTs
// @ts-nocheck
import { KRRecord } from "./e/f";
import { KRNativeRenderControllerReadyCallback } from "./h1/j1";
import { IKuiklyViewDelegate } from "./o";
import { KuiklyNativeManager } from "./k/l";
import { ErrorReason } from "./i/j";
import { Size } from "@ohos.arkui.node";
import { KRRenderExecuteModeBase } from "./u/x";
/**
 * @brief ArkUI C-Api Native渲染根节点
 */
@Component
export declare struct KRNativeRender {
    /**
     * 页面名字
     */
    pageName: string;
    /**
     * 透传给KR侧的页面数据
     */
    pageData: KRRecord;
    /**
     * 执行模式
     */
    executeMode: KRRenderExecuteModeBase;
    /**
     * 执行上下文code，驱动渲染对应的代码。KRRenderExecuteMode.Native模式时: 传递 ''
     */
    contextCode: string;
    /**
     * assets资源的实际路径，如未设置，默认在resfile中找assets资源
     */
    assetsDir: string;
    /**
     * 鸿蒙侧对KRSharedPreferencesModule实现方式的选择
     * 默认使用基于XML持久化的PreferencesModule
     * 如果为true，则调用KROhSharedPreferencesModule
     */
    useOhSharedPreferences: boolean;
    /**
     * 控制器ready 回调
     */
    onControllerReadyCallback: KRNativeRenderControllerReadyCallback | null;
    nativeManager: KuiklyNativeManager | null;
    delegate?: IKuiklyViewDelegate;
    /**
     * @deprecated use pageName instead
     */
    pagerName?: string;
    /**
     * @deprecated use pageData instead
     */
    pagerData?: KRRecord;
    /**
     * @deprecated use pageData instead
     */
    pageParams?: KRRecord;
    /**
     * @deprecated use controller.registerExceptionCallback from onControllerReadyCallback instead
     */
    onRenderException?: ((exception: Error, reason: ErrorReason) => void);
    /**
     * 容器是否在输入法中
     * 普通app默认为false，输入法使用到的场景请置为true
     */
    imeMode: boolean;
    initialSize?: Size;
    private controller?;
    private nodeContent;
    private loaded;
    loadNativeLibIfNeeded(): void;
    aboutToAppear(): void;
    aboutToDisappear(): void;
    build(): void;
}
