// @keepTs
// @ts-nocheck
import { KRAny, KuiklyRenderCallback } from "../e/f";
import { KuiklyRenderBaseModule } from "./h";
export declare class KRLogModuleArkTS extends KuiklyRenderBaseModule {
    static readonly MODULE_NAME = "KRLogModuleArkTS";
    private static readonly METHOD_LOG_INFO;
    private static readonly METHOD_LOG_DEBUG;
    private static readonly METHOD_LOG_ERROR;
    syncMode(): boolean;
    call(m18: string, n18: KRAny, o18: KuiklyRenderCallback | null): KRAny;
    private logInfo;
    private logDebug;
    private logError;
    private getTag;
}
