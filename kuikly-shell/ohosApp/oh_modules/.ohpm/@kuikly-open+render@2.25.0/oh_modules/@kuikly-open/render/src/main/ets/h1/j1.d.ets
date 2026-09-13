// @keepTs
// @ts-nocheck
import { KRAny, KRValue, KuiklyRenderCallback } from "../e/f";
import { KuiklyRenderBaseView } from "../p/d1/e1";
import { IKuiklyRenderModuleExport, KRRenderModuleExportCreator } from "../m/d1/t1";
import { KRNativeRenderController } from "../o";
import { KRRenderExecuteModeBase } from "../u/x";
export type KRViewCreator = () => KuiklyRenderBaseView;
export type KRExceptionCallback = (executeMode: KRRenderExecuteModeBase, stack: string) => void;
export type KRNativeRenderControllerReadyCallback = (controller: KRNativeRenderController) => void;
export declare class KRNativeInstance {
    private uiContext;
    private controller;
    private modules;
    private viewRegistry;
    private static moduleCreatorRegistry;
    private static viewCreatorRegistry;
    private exceptionCallback;
    private mInstanceId;
    constructor(d14: string, e14: UIContext, f14: KRNativeRenderController);
    registerExceptionCallback(c14: KRExceptionCallback | null): void;
    fireExceptionCallback(b14: string): void;
    getUIContext(): UIContext;
    getNativeRenderController(): KRNativeRenderController | null;
    getAssetsDir(): string;
    getControllerExecuteMode(): number;
    createView(x13: number, y13: string): void;
    viewWithTag(v13: number): KuiklyRenderBaseView | null;
    generateViewBuilder(r13: number, s13: string): KRAny;
    setViewProp(n13: number, o13: string, p13: KRValue): void;
    setViewEvent(j13: number, k13: string, l13: KuiklyRenderCallback): void;
    callViewMethod(e13: number, f13: string, g13: KRAny, h13: KuiklyRenderCallback | null): void;
    removeView(c13: number): void;
    didMoveToParentView(a13: number): void;
    setViewSize(w12: number, x12: number, y12: number): void;
    /**
     * 注册ArkTS侧Module创建器(一个页面最多有一个同module实例)
     * @param moduleName 和native侧该module同名
     * @param moduleCreator  module实例创建器
     */
    static registerModuleCreator(u12: string, v12: KRRenderModuleExportCreator): void;
    /**
     * 注册ArkTS侧View创建器
     * @param viewType 对应Kotlin侧的viewName返回字符串作为viewType注册，如：'KRImageView'
     */
    static registerViewCreator(s12: string, t12: KRViewCreator): void;
    /**
     * 获取Module实例（不存在就创建）
     * @param moduleName
     * @returns
     */
    getOrCreateModule(o12: string): IKuiklyRenderModuleExport | null;
    callModuleMethodFromNative(f12: KRAny, g12: KRAny, h12: KRAny, i12: KRAny, j12: KRAny, k12: KuiklyRenderCallback | null): KRAny;
    /**
     * 销毁时调用
     */
    onDestroy(): void;
    onConfigurationUpdate(): void;
}
