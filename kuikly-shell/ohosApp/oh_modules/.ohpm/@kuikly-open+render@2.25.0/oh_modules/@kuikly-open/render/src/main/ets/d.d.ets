// @keepTs
// @ts-nocheck
import { KRRecord } from "./e/f";
import { KRNativeRenderControllerReadyCallback } from "./h1/j1";
import { KuiklyNativeManager } from "./k/l";
import { KRRenderExecuteModeBase } from "./u/x";
/**
 * @brief View粒度入口类（业务可以使用该Ark组件作为接入层入口类）
 */
@Component
export declare struct KRNative {
    /**
     * 页面名字
     */
    pageName: string;
    /**
     * 透传给KR侧的页面数据
     */
    pageParams: KRRecord;
    /**
     * 执行模式
     */
    executeMode: KRRenderExecuteModeBase;
    /**
     * 执行上下文code，驱动渲染对应的代码。KRRenderExecuteMode.Native模式时: 传递 ''
     */
    contextCode: string;
    /**
     * KRNative Controller回调, 可获取此Controller来调用 KRNative 暴露的方法
     */
    onControllerReadyCallback: KRNativeRenderControllerReadyCallback | null;
    nativeManager: KuiklyNativeManager | null;
    aboutToAppear(): void;
    build(): void;
}
