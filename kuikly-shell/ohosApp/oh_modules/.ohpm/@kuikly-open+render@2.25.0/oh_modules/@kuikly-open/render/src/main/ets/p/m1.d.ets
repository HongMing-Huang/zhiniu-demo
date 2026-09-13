// @keepTs
// @ts-nocheck
import { KuiklyRenderBaseView } from "./d1/e1";
import { ComponentContent } from "@ohos.arkui.node";
import { KRAny, KRValue, KuiklyRenderCallback } from "../e/f";
export declare class KRBlurView extends KuiklyRenderBaseView {
    blurRadius: number;
    init(v1: string, w1: number, x1: string): void;
    setProp(t1: string, u1: KRValue | KuiklyRenderCallback): boolean;
    call(q1: string, r1: KRAny, s1: KuiklyRenderCallback | null): void;
    createArkUIView(): ComponentContent<KuiklyRenderBaseView>;
    didMoveToParentView(): void;
    sizeDidChanged(n1: number, o1: number): void;
    onDestroy(): void;
    private setBlurRadius;
}
