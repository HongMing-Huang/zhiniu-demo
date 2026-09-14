// 知牛 · 鸿蒙路由适配器：把 Kuikly 页面内 openPage/closePage 桥到 ArkUI 页面栈。
// 单 Index 页复用模式：所有 Kuikly 页面都路由到 pages/Index，通过 params 携带
// pageName/pageData，Index.aboutToAppear 读取后重建 Kuikly 视图。
import { IKRRouterAdapter, KRRecord } from '@kuikly-open/render';
import router from '@ohos.router';
import { common } from '@kit.AbilityKit';

export class AppKRRouterAdapter implements IKRRouterAdapter {
  openPage(context: common.UIAbilityContext, pageName: string, pageData: KRRecord): void {
    // 底部 Tab 级切换（__tab=1）：replaceUrl 不堆栈（返回键不倒退遍历 Tab）
    const isTab = pageData['__tab'] === '1';
    if (isTab) {
      const replaceOpt: router.RouterOptions = {
        url: 'pages/Index',
        params: {
          pageName,
          pageData
        }
      };
      router.replaceUrl(replaceOpt);
    } else {
      const pushOpt: router.RouterOptions = {
        url: 'pages/Index',
        params: {
          pageName,
          pageData
        }
      };
      router.pushUrl(pushOpt);
    }
  }

  closePage(context: common.UIAbilityContext): void {
    router.back();
  }
}
