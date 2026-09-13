// @keepTs
// @ts-nocheck
import { KRAny, KRValue, KuiklyRenderCallback } from "../../e/f";
import { ComponentContent } from "@ohos.arkui.node";
import { NodeContent } from "@ohos.arkui.node";
import { UIContext } from "@ohos.arkui.UIContext";
import { KRNativeInstance } from "../../h1/j1";
export declare const kCssBackgroundColor = "backgroundColor";
export declare const kCssBackgroundImage = "backgroundImage";
export interface IKuiklyRenderViewExport {
    init(instanceId: string, viewTag: number, viewType: string): void;
    setProp(propKey: string, propValue: KRAny | KuiklyRenderCallback): boolean;
    /**
     * 调用当前View的实例方法
     * @param params 方法参数 (透传kotlin侧数据, 类型可为string, Array, Int8Array, number)
     * @param callback 回调
     */
    call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void;
}
export declare abstract class KuiklyRenderBaseView implements IKuiklyRenderViewExport {
    private instanceId;
    private viewTag;
    private viewType;
    private arkUINode;
    private viewWidth;
    private viewHeight;
    cssBackgroundColor: ResourceColor | null;
    cssLinearGradient: LinearGradient | null;
    nodeId: string;
    getViewWidth(): number;
    getViewHeight(): number;
    getInstanceId(): string;
    getViewTag(): number;
    getViewType(): string;
    getUIContext(): UIContext | null;
    getInstance(): KRNativeInstance | null;
    getNodeId(): string;
    /**
     * 初始化方法
     * @param instanceId 实例id
     * @param viewTag 视图Tag
     * @param viewType 视图类型
     */
    init(i1: string, j1: number, k1: string): void;
    /**
     * 设置（更新）属性时调用
     * @param propKey 视图实例属性名
     * @param propValue 视图实例属性值，类型一般为字符串等基础数据结构以及RenderCallback（用于事件绑定）
     * @return 是否处理了该key
     */
    setProp(g1: string, h1: KRAny | KuiklyRenderCallback): boolean;
    private setBackgroundImage;
    private parseLinearGradient;
    private static readonly gradientDirectionOrder;
    /**
     * 设置事件时调用
     * @param eventKey 和KR约定好的事件名
     * @param eventCallback 事件响应回调用KR层
     */
    /**
     * 调用当前View(Node)的实例方法
     * @param method 方法名字
     * @param params 方法参数 (透传kotlin侧数据, 类型可为string, Array, Int8Array, number)
     * @param callback 闭包回调
     */
    abstract call(v: string, w: KRAny, x: KuiklyRenderCallback | null): void;
    /**
     * 创建该节点对应的ArkUI
     * @returns 返回ComponentContent实例
     * 参考：
     * new ComponentContent<KRBaseViewExport>(this.getUIContext() as UIContext, wrapBuilder<[KRBaseViewExport]>(createArkUIView), this);
     * 其中createArkUIView定义为：
     * @Builder
      function createArkUIView(view: KRBaseViewExport) {
           // 构造你的UI,如Column
            Column(){
  
            }.width('100%').height('100%') // 务必设置width('100%').height('100%')，保证在ArkTS节点在CAPI侧的Wrapper Node容器中撑满
      }
     }
     */
    abstract createArkUIView(): ComponentContent<KuiklyRenderBaseView>;
    getContentSlot(): NodeContent | null;
    /**
     * View销毁时，此方法会被调用，用于销毁View中的资源
     */
    onDestroy(): void;
    /**
     * 已经添加到父节点中
     */
    didMoveToParentView(): void;
    /**
     * 设置ViewSize
     */
    sizeDidChanged(t: number, u: number): void;
    /**
     * 在View数据变化的情况下，可以调用该方法更新ArkUI View节点
     */
    updateArkUI(): void;
    /**
     * @final （禁止重写）
     */
    toSetProp(q: string, r: KRValue): boolean;
    /**
     * @final （禁止重写）
     */
    toSetEvent(o: string, p: KuiklyRenderCallback): void;
    /**
     * @final  （禁止重写）
     */
    toCreateArkUIView(m: string): ComponentContent<KuiklyRenderBaseView> | null;
    /**
     * @final  （禁止重写）
     */
    toDestroy(): void;
    /**
     * @final  （禁止重写）
     */
    toCall(j: string, k: KRAny, l: KuiklyRenderCallback | null): void;
    /**
     * @final  （禁止重写）
     */
    toDidMoveToParentView(): void;
    /**
     * @final  （禁止重写）
     */
    toSizeDidChanged(h: number, i: number): void;
    onConfigurationUpdate(): void;
}
export type KRRenderViewExportCreator = () => KuiklyRenderBaseView;
