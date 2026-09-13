// @keepTs
// @ts-nocheck
import { KRViewCreator } from "../h1/j1";
/**
 * ArkTS侧内置Module均在此注册生成实例闭包
 */
export declare class ViewsRegisterEntry {
    static registerExternalViewCallback: () => void;
    static registerSDKViews(): void;
    /**
     * 注册ArkTS侧View创建器
     * @param viewType 对应Kotlin侧的viewName返回字符串作为viewType注册，如：'KRImageView'
     */
    static registerViewCreator(d4: string, e4: KRViewCreator): void;
}
