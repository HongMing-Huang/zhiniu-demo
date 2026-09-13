// @keepTs
// @ts-nocheck
import { KRAny, KuiklyRenderCallback } from "../../e/f";
import { KuiklyRenderBaseModule } from "../h";
export declare class KRSnapshotModule extends KuiklyRenderBaseModule {
    static readonly MODULE_NAME = "KRSnapshotModule";
    syncMode(): boolean;
    call(b17: string, c17: KRAny, d17: KuiklyRenderCallback | null): KRAny;
}
