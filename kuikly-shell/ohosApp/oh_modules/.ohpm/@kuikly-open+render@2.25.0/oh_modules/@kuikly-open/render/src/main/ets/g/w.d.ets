// @keepTs
// @ts-nocheck
import { KRValue, KuiklyRenderCallback } from "../e/f";
import { KuiklyRenderBaseView } from "../p/d1/e1";
import { ComponentContent } from "@ohos.arkui.node";
export interface IKRPAGViewController {
    setProp(propKey: string, propValue: KRValue | KuiklyRenderCallback): boolean;
    addListener(listener: IKRPAGViewListener): void;
    removeListener(listener: IKRPAGViewListener): void;
    play(): void;
    stop(): void;
    setProgress(value: number): void;
}
export interface IKRPAGViewListener {
    onAnimationStart?: (viewController: IKRPAGViewController) => void;
    onAnimationEnd?: (viewController: IKRPAGViewController) => void;
    onAnimationRepeat?: (viewController: IKRPAGViewController) => void;
    onAnimationCancel?: (viewController: IKRPAGViewController) => void;
    onAnimationUpdate?: (viewController: IKRPAGViewController) => void;
}
export interface IKRPAGViewAdapter {
    createController(): IKRPAGViewController;
    createPAGView(ctx: UIContext, view: KuiklyRenderBaseView): ComponentContent<KuiklyRenderBaseView>;
}
