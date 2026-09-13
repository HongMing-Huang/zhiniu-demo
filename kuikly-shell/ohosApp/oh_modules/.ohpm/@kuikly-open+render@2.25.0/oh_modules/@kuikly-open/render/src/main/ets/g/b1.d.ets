// @keepTs
// @ts-nocheck
import { KRValue, KuiklyRenderCallback } from "../e/f";
import { KuiklyRenderBaseView } from "../p/d1/e1";
import { ComponentContent } from "@ohos.arkui.node";
import { KRVideoPlayState, KRVideoViewContentMode } from "../p/f1";
export interface IKRVideoViewController {
    /**
     * kuikly侧设置的属性，一般用于业务扩展使用
     */
    setProp(propKey: string, propValue: KRValue | KuiklyRenderCallback): boolean;
    /**
     * 添加事件监听
     */
    addListener(listener: IKRVideoViewListener): void;
    /**
     * 移除事件监听
     */
    removeListener(listener: IKRVideoViewListener): void;
    /**
     * 设置视频源
     */
    setSrc(src: string): void;
    /**
     * 预播放到第一帧（停在第一帧，用于预加载优化）
     */
    preplay(): void;
    /**
     * 播放视频
     */
    play(): void;
    /**
     * 暂停视频
     */
    pause(): void;
    /**
     * 停止并销毁视频
     */
    stop(): void;
    /**
     * 设置画面拉伸模式
     */
    setVideoContentMode(videoViewContentMode: KRVideoViewContentMode): void;
    /**
     * 设置静音属性
     */
    setMuted(muted: boolean): void;
    /**
     * 设置倍速（1.0, 1.5, 2.0）
     */
    setRate(rate: number): void;
    /**
     * seek视频
     * @param seekToTimeMs 时间，单位毫秒
     */
    seekToTime(seekToTimeMs: number): void;
}
export interface IKRVideoViewListener {
    onPlayStateDidChanged?: (state: KRVideoPlayState, viewController: IKRVideoViewController) => void;
    onPlayTimeDidChanged?: (currentTime: number, totalTime: number, viewController: IKRVideoViewController) => void;
    onFirstFrameDidDisplay?: (viewController: IKRVideoViewController) => void;
    onCustomEventWithInfo?: (viewController: IKRVideoViewController, info: Map<string, string>) => void;
}
export interface IKRVideoViewAdapter {
    createController(): IKRVideoViewController;
    createVideoView(ctx: UIContext, view: KuiklyRenderBaseView): ComponentContent<KuiklyRenderBaseView>;
}
