// @keepTs
// @ts-nocheck
import { KRAny, KuiklyRenderCallback } from "../e/f";
import { KuiklyRenderBaseModule } from "./h";
export declare class KRPerformanceModule extends KuiklyRenderBaseModule {
    static readonly MODULE_NAME = "KRPerformanceModule";
    private static readonly METHOD_NOTIFY_INIT_STATE;
    private static readonly METHOD_ON_GET_LAUNCH_DATA;
    private static readonly METHOD_ON_GET_PERFORMANCE_DATA;
    syncMode(): boolean;
    call(j21: string, k21: KRAny, l21: KuiklyRenderCallback | null): KRAny;
}
