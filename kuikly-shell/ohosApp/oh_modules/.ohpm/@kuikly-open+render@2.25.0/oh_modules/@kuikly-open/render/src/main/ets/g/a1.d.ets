// @keepTs
// @ts-nocheck
import { KRRecord } from "../e/f";
import a from "@ohos.app.ability.common";
export interface IKRRouterAdapter {
    /**
     * 打开一个 Kuikly 页面
     * @param context 上下文
     * @param pageName 页面名字
     * @param pageData 页面参数
     */
    openPage(context: a.UIAbilityContext, pageName: string, pageData: KRRecord): void;
    /**
     * 关闭当前页面
     * @param 上下文
     */
    closePage(context: a.UIAbilityContext): void;
}
