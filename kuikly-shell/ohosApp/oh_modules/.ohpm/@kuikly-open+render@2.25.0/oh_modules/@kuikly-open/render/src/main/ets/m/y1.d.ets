// @keepTs
// @ts-nocheck
import { KRAny, KuiklyRenderCallback } from "../e/f";
import { KuiklyRenderBaseModule } from "./d1/u1";
export declare class KRExceptionModule extends KuiklyRenderBaseModule {
    syncMode(): boolean;
    static readonly MODULE_NAME = "KRExceptionModule";
    private static readonly ON_EXCEPTION;
    call(v17: string, w17: KRAny, x17: KuiklyRenderCallback | null): KRAny;
    onDestroy(): void;
    private fireExceptionCallback;
}
