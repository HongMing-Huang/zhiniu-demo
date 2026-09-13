// @keepTs
// @ts-nocheck
import { KuiklyRenderCallback } from "../e/f";
import u22 from "@ohos.app.ability.common";
export declare class KRNetworkCacheUtil {
    private static downloadTasks;
    /**
     * 通用下载或者本地缓存来返回url对应的本地磁盘文件路径
     * @param url
     * @param callback
     */
    static fetchFileByDownloadOrCache(u23: u22.UIAbilityContext, v23: string, w23: KuiklyRenderCallback): void;
    static downloadFile(l23: u22.UIAbilityContext, m23: string, n23: string, o23: KuiklyRenderCallback): void;
    private static handleLoadFailure;
    static urlToFilename(f23: string): string;
    /**
     * 根据网络url生成本地的文件磁盘文件路径
     * @param url
     */
    static generateCacheFilePathByUrl(c23: u22.UIAbilityContext, d23: string): string;
    static getPathExtension(x22: string): string | null;
}
