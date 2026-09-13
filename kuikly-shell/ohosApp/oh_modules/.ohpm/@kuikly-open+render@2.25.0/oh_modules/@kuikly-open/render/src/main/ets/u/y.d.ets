// @keepTs
// @ts-nocheck
import { KRRenderExecuteModeBase } from "./x";
export declare class KRRenderNativeMode extends KRRenderExecuteModeBase {
    constructor();
    getMode(): number;
    static readonly Native: KRRenderExecuteModeBase;
}
