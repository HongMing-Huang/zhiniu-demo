// @keepTs
// @ts-nocheck
import { ErrorReason } from "../i/j";
import f4 from '@ohos.app.ability.common';
import { KRAny } from "../e/f";
/**
 * 渲染流程执行环境接口
 */
export interface IKuiklyRenderContextHandler {
    /**
     * 初始化渲染执行环境
     * @param pagerId 页面实例ID
     * @param communicator - native和context执行产物通信器。
     * @param ext 扩展数据
     */
    init(pagerId: string, communicator: IKuiklyContextNativeCommunicator, ext: Record<string, string> | null): void;
    /**
     * 销毁渲染执行环境
     */
    destroy(): void;
    /**
     * 调用kotlin侧方法，与Kotlin通信
     * @param method KuiklyRenderContextMethod定义的方法
     * @param arg0 参数0
     * @param arg1 参数1
     * @param arg2 参数2
     * @param arg3 参数3
     * @param arg4 参数4
     * @param arg5 参数5
     */
    callKotlinMethod(method: KuiklyRenderContextMethod, arg0: KRAny, arg1: KRAny, arg2: KRAny, arg3: KRAny, arg4: KRAny, arg5: KRAny): void;
    /**
     * 注册Kuikly侧调用Native的回调
     * @param callback Kuikly -> Native 回调
     */
    registerCallNative(callback: KuiklyRenderNativeMethodCallback): void;
    /**
     * 注册异常处理回调
     */
    notifyException(exception: Error, errorReason: ErrorReason): void;
}
/**
 * Native和Kotlin侧交互通信者接口
 */
export interface IKuiklyContextNativeCommunicator {
    /**
     * 调用kotlin侧方法，与Kotlin通信
     * @param method KuiklyRenderContextMethod定义的方法
     * @param arg0 参数0
     * @param arg1 参数1
     * @param arg2 参数2
     * @param arg3 参数3
     * @param arg4 参数4
     * @param arg5 参数5
     */
    callKotlinMethod(method: number, arg0: KRAny, arg1: KRAny, arg2: KRAny, arg3: KRAny, arg4: KRAny, arg5: KRAny): void;
    /**
     * 注册Kuikly侧调用Native的回调
     * @param pagerId 页面实例ID
     * @param callback Kuikly -> Native 回调
     */
    registerCallNative(pagerId: string, callback: (methodId: number, arg0: KRAny, arg1: KRAny, arg2: KRAny, arg3: KRAny, arg4: KRAny, arg5: KRAny) => KRAny): void;
    /**
     * context销毁时调用
     */
    destroy(): void;
}
/**
 * Kuikly执行环境暴露给Native调用的方法列表枚举
 */
export declare enum KuiklyRenderContextMethod {
    Unknown = 0,
    CreateInstance = 1,
    UpdateInstance = 2,
    DestroyInstance = 3,
    FireCallback = 4,
    FireViewEvent = 5,
    LayoutView = 6
}
/**
 * Native暴露给Kuikly执行环境调用的方法列表枚举
 */
export declare enum KuiklyRenderNativeMethod {
    Unknown = 0,
    CreateRenderView = 1,
    RemoveRenderView = 2,
    InsertSubRenderView = 3,
    SetViewProp = 4,
    SetRenderViewFrame = 5,
    CalculateRenderViewSize = 6,
    CallViewMethod = 7,
    CallModuleMethod = 8,
    CreateShadow = 9,
    RemoveShadow = 10,
    SetShadowProp = 11,
    SetShadowForView = 12,
    SetTimeout = 13,
    CallShadowMethod = 14,
    FireFatalException = 15,
    SyncFlushUI = 16,
    CallTDFNativeMethod = 17,
    MaxCount = 18
}
/**
 * 将整数值转换为KuiklyRenderNativeMethod枚举值
 * @param value 整数值
 * @returns 对应的枚举值
 */
export declare function valueToNativeMethod(i4: number): KuiklyRenderNativeMethod;
export interface KuiklyRenderNativeMethodParams {
    method: KuiklyRenderNativeMethod;
    args: [
        KRAny,
        KRAny,
        KRAny,
        KRAny,
        KRAny,
        KRAny
    ];
}
export interface KuiklyRenderContextMethodParams {
    method: KuiklyRenderContextMethod;
    args: [
        string,
        KRAny,
        KRAny,
        KRAny,
        KRAny,
        KRAny
    ];
    context?: f4.UIAbilityContext;
}
/**
 * KuiklyRenderNativeMethodCallback 类型定义为一个函数，该函数接收一个 KuiklyRenderNativeMethod 枚举值和6个任意类型参数，
 * 并返回一个任意类型的结果。
 */
export type KuiklyRenderNativeMethodCallback = (methodId: KuiklyRenderNativeMethod, arg0: KRAny, arg1: KRAny, arg2: KRAny, arg3: KRAny, arg4: KRAny, arg5: KRAny) => KRAny;
export type KuiklyRenderContextMethodCallback = (method: KuiklyRenderContextMethod, pageId: string, arg1: KRAny, arg2: KRAny, arg3: KRAny, arg4: KRAny, arg5: KRAny) => void;
/**
 * 将任意 KRAny 类型的值转换为 Kotlin 侧能接收类型的值
 * @param value 要转换的 KRAny 类型的值。
 * @returns 转换后的 Kotlin能接收 类型的值。
 */
export declare function anyToKotlinObject(g4: KRAny): KRAny;
