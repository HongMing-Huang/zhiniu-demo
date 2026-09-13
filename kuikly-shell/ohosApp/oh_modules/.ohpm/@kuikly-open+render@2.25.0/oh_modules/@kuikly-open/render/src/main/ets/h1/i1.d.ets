// @keepTs
// @ts-nocheck
import { KRRenderModuleExportCreator } from "../m/d1/t1";
import { KRNativeInstance, KRViewCreator } from "./j1";
import h14 from "@ohos.window";
import { KRWindowInfo } from "../o1/r1";
import { KRNativeRenderController } from "../o";
export declare enum KRCallNativeMethod {
    General = 0,
    Register = 1,
    FireCallback = 2,
    KeyboardHeightChange = 3,
    FireViewEvent = 4
}
export declare enum KRNativeCallArkTSMethod {
    Unknown = 0,
    CallModuleMethod = 1,
    CreateView = 2,
    CreateArkUINode = 3,
    SetViewProp = 4,
    SetViewEvent = 5,
    CallViewMethod = 6,
    RemoveView = 7,
    SetViewSize = 8,
    DidMoveToParentView = 9
}
export declare class KRNativeManager {
    private static instance;
    private instancesMap;
    private didInit;
    private windowInfoMap;
    private constructor();
    static getInstance(): KRNativeManager;
    onLastWindow(o16: h14.Window): void;
    getWindowInfo(n16: string): KRWindowInfo | undefined;
    /**
     * 移除窗口信息如果必要
     * @param instanceId
     * @param windowId
     */
    removeWindowInfoIfNeed(k16: string): void;
    /**
     * 创建Native实例
     * @param instanceId 实例ID
     */
    createNativeInstance(h16: string, i16: UIContext, j16: KRNativeRenderController): void;
    /**
     * 删除Native实例（延时1s）
     * @param instanceId
     */
    removeNativeInstance(f16: string): void;
    /**
     * 获取实例
     * @param instanceId
     * @returns
     */
    getNativeInstance(d16: string): KRNativeInstance | null;
    initIfNeed(): void;
    private arkTSCallNative;
    /**
     * 注册ArkTS侧View创建器
     * @param viewType 对应Kotlin侧的viewName返回字符串作为viewType注册，如：'KRImageView'
     */
    registerViewCreator(y14: string, z14: KRViewCreator): void;
    registerViewCreatorV2(w14: string, x14: KRViewCreator): void;
    /**
     * 注册ArkTS侧View创建器
     * @param moduleName 和native侧该module同名
     * @param moduleCreator  module实例创建器
     */
    registerModuleCreator(u14: string, v14: KRRenderModuleExportCreator): void;
    /**
     * 通知键盘高度变化
     * @param height 键盘高度（单位vp）
     * @param duration 键盘弹出时间（单位毫秒）
     */
    keyboardHeightChange(q14: number, r14: number, s14: string): void;
    private fireCallback;
    private fireViewEvent;
}
