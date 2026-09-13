// @keepTs
// @ts-nocheck
import { IKuiklyRenderModuleExport } from "./d1/t1";
/**
 * ArkTS侧内置Module均在此注册生成实例闭包
 */
export declare class ModulesRegisterEntry {
    static registerExternalModulesCallback: () => void;
    static registerSDKModules(): void;
    static registerModuleCreator(y21: string, z21: () => IKuiklyRenderModuleExport): void;
}
