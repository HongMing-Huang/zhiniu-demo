// @keepTs
// @ts-nocheck
import { KuiklyRenderBaseView } from "./d1/e1";
import { ComponentContent } from "@ohos.arkui.node";
import { KRAny, KRValue, KuiklyRenderCallback } from "../e/f";
import { IKRVideoViewController, IKRVideoViewListener } from "../g/b1";
export declare enum KRVideoPlayState {
    Unknown = 0,
    Playing = 1,
    Caching = 2,
    Paused = 3,
    PlayEnd = 4,
    Failed = 5
}
export declare enum KRVideoViewContentMode {
    Contain = 0,
    Cover = 1,
    Stretch = 2
}
declare enum g3 {
    None = 0,
    Preplay = 1,
    Play = 2,
    Pause = 3,
    Stop = 4
}
export declare class KRVideoView extends KuiklyRenderBaseView implements IKRVideoViewListener {
    init(x3: string, y3: number, z3: string): void;
    onPlayStateDidChanged: (state: KRVideoPlayState, viewController: IKRVideoViewController) => void;
    onPlayTimeDidChanged: (currentTime: number, totalTime: number, viewController: IKRVideoViewController) => void;
    onFirstFrameDidDisplay: (viewController: IKRVideoViewController) => void;
    onCustomEventWithInfo: (viewController: IKRVideoViewController, info: Map<string, string>) => void;
    playStateDidChangedEvent: KuiklyRenderCallback | null;
    playTimeDidChangedEvent: KuiklyRenderCallback | null;
    firstFrameDidDisplayEvent: KuiklyRenderCallback | null;
    customEventWithInfoEvent: KuiklyRenderCallback | null;
    src: string;
    controller: IKRVideoViewController | undefined;
    isPlaying: boolean;
    static readonly VIEW_NAME = "KRVideoView";
    private static readonly PROP_SRC;
    private static readonly PROP_PLAY_CONTROL;
    private static readonly PROP_MUTED;
    private static readonly PROP_RATE;
    private static readonly PROP_RESIZE_MODE;
    private static readonly EVENT_PLAY_STATE_CHANGE;
    private static readonly EVENT_PLAY_TIME_CHANGE;
    private static readonly EVENT_FIRST_FRAME;
    private static readonly EVENT_CUSTOM_EVENT;
    setProp(n3: string, o3: KRValue | KuiklyRenderCallback): boolean;
    setPlayControl(m3: g3): void;
    setResizeMode(l3: string): void;
    call(i3: string, j3: KRAny, k3: KuiklyRenderCallback | null): void;
    didMoveToParentView(): void;
    createArkUIView(): ComponentContent<KuiklyRenderBaseView>;
}
export {};
