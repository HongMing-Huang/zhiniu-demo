// @keepTs
// @ts-nocheck
import { KRAny, KuiklyRenderCallback } from "../e/f";
import { KuiklyRenderBaseModule } from "./h";
export declare class KRNotifyModule extends KuiklyRenderBaseModule {
    static readonly MODULE_NAME = "KRNotifyModule";
    private static readonly METHOD_ADD_NOTIFY;
    private static readonly METHOD_REMOVE_NOTIFY;
    private static readonly METHOD_POST_NOTIFY;
    private subscribersMap;
    private globalCallback;
    syncMode(): boolean;
    call(e21: string, f21: KRAny, g21: KuiklyRenderCallback | null): KRAny;
    onDestroy(): void;
    addNotify(t20: string | null, u20: KuiklyRenderCallback | null): void;
    removeNotify(o20: string | null): void;
    postNotify(m20: string | null): void;
}
