// @keepTs
// @ts-nocheck
import { KRAny, KuiklyRenderCallback } from "../../e/f";
import { KRNativeRenderController } from "../../o";
import { IKuiklyRenderModuleExport } from "./t1";
import { KuiklyRenderBaseView } from "../../p/d1/e1";
export declare abstract class KuiklyRenderBaseModule implements IKuiklyRenderModuleExport {
    protected controller: KRNativeRenderController | null;
    protected instanceId: string;
    abstract syncMode(): boolean;
    init(w16: string): void;
    call(t16: string, u16: KRAny, v16: KuiklyRenderCallback | null): KRAny;
    onDestroy(): void;
    viewWithTag(r16: number): KuiklyRenderBaseView | null;
}
