// @keepTs
// @ts-nocheck
import v4 from "@ohos.window";
import { KRRect } from "./p1";
export declare class KRWindowInfo {
    private statusBarHeight;
    private windowRect;
    private window;
    private windowId;
    instanceCount: number;
    private keyboardHeightChangeCallback;
    private windowSizeChangeCallback;
    constructor(q24: v4.Window);
    getStatusBarHeightS(): number;
    getWindowRect(): KRRect;
    addInstanceCount(): void;
    subInstanceCount(): void;
    getInstanceCount(): number;
    offWindowCallback(): void;
}
