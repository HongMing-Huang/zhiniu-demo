// @keepTs
// @ts-nocheck
import { KuiklyRenderBaseView } from "./d1/e1";
import { ComponentContent } from "@ohos.arkui.node";
import { KRAny, KRValue, KuiklyRenderCallback } from "../e/f";
import { IKRPAGViewController, IKRPAGViewListener } from "../g/w";
export declare class KRPAGView extends KuiklyRenderBaseView implements IKRPAGViewListener {
    init(z2: string, a3: number, b3: string): void;
    onAnimationStart: (viewController: IKRPAGViewController) => void;
    onAnimationEnd: (viewController: IKRPAGViewController) => void;
    onAnimationRepeat: (viewController: IKRPAGViewController) => void;
    onAnimationCancel: (viewController: IKRPAGViewController) => void;
    loadFailureEvent: KuiklyRenderCallback | null;
    animationStartEvent: KuiklyRenderCallback | null;
    animationEndEvent: KuiklyRenderCallback | null;
    animationCancelEvent: KuiklyRenderCallback | null;
    animationRepeatEvent: KuiklyRenderCallback | null;
    src: string;
    autoPlay: boolean;
    repeatCount: number;
    private hadSrcSet;
    private hadStop;
    controller: IKRPAGViewController | undefined;
    static readonly VIEW_NAME = "KRPAGView";
    private static readonly PROP_SRC;
    private static readonly PROP_REPEAT_COUNT;
    private static readonly PROP_AUTO_PLAY;
    private static readonly EVENT_LOAD_FAIL;
    private static readonly EVENT_ANIMATION_START;
    private static readonly EVENT_ANIMATION_END;
    private static readonly EVENT_ANIMATION_Cancel;
    private static readonly EVENT_ANIMATION_REPEAT;
    private static readonly PROP_METHOD_PLAY;
    private static readonly PROP_METHOD_STOP;
    private static readonly PROP_METHOD_SET_PROGRESS;
    private static readonly SCHEME_ASSETS;
    private static readonly SCHEME_FILE;
    setProp(t2: string, u2: KRValue | KuiklyRenderCallback): boolean;
    call(o2: string, p2: KRAny, q2: KuiklyRenderCallback | null): void;
    private tryAutoPlay;
    createArkUIView(): ComponentContent<KuiklyRenderBaseView>;
    convertAssetsPathIfNeed(i2: string): string;
}
