// @keepTs
// @ts-nocheck
import { KRAny, KuiklyRenderCallback } from "../e/f";
import { KuiklyRenderBaseModule } from "./d1/u1";
export declare class KRRouterModule extends KuiklyRenderBaseModule {
    syncMode(): boolean;
    static readonly MODULE_NAME = "KRRouterModule";
    private static readonly OPEN_PAGE;
    private static readonly CLOSE_PAGE;
    call(v21: string, w21: KRAny, x21: KuiklyRenderCallback | null): KRAny;
    onDestroy(): void;
    private openPage;
    private closePage;
}
