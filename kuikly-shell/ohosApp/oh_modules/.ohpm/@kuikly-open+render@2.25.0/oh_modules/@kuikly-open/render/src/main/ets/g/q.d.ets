// @keepTs
// @ts-nocheck
import { IKRLogAdapter } from "./z";
import { IKRRouterAdapter } from "./a1";
import { IKRColorParserAdapter } from "./c1";
import { IKRPAGViewAdapter } from "./w";
import { IKRVideoViewAdapter } from "./b1";
export declare class KuiklyRenderAdapterManager {
    /**
     * 页面跳转适配器
     */
    static krRouterAdapter?: IKRRouterAdapter | null;
    /**
     * Log适配器
     */
    static krLogAdapter: IKRLogAdapter | null;
    /**
     * 颜色转换器
     */
    static krColorParserAdapter: IKRColorParserAdapter | null;
    /**
     * 视频控件适配器
     */
    static krVideoViewAdapter: IKRVideoViewAdapter | null;
    /**
     * Pag适配器
     */
    static krPagAdapter: IKRPAGViewAdapter | null;
}
