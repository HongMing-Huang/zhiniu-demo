import { AbilityConstant, UIAbility, Want } from '@kit.AbilityKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { window } from '@kit.ArkUI';
import { KuiklyRenderAdapterManager } from '@kuikly-open/render';
import { AppKRRouterAdapter } from '../kuikly/adapters/AppKRRouterAdapter';
import Napi from 'libkuikly_entry.so';

export default class EntryAbility extends UIAbility {
  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    hilog.info(0x0000, 'kuikly', '%{public}s', 'Ability onCreate');
    // Kuikly 页面 openPage/closePage → ArkUI 页面栈（底部 Tab / 详情跳转依赖此桥）
    KuiklyRenderAdapterManager.krRouterAdapter = new AppKRRouterAdapter();
  }

  onDestroy(): void {
    hilog.info(0x0000, 'kuikly', '%{public}s', 'Ability onDestroy');
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    hilog.info(0x0000, 'kuikly', '%{public}s', 'Ability onWindowStageCreate');

    windowStage.loadContent('pages/Index', (err) => {
      if (err.code) {
        hilog.error(0x0000, 'kuikly', 'Failed to load content. Cause: %{public}s', JSON.stringify(err) ?? '');
        return;
      }
      hilog.info(0x0000, 'kuikly', 'Succeeded in loading content.');
      Napi.setResourceManager(getContext()?.resourceManager);
    });

    let mainWindow = windowStage.getMainWindowSync();
    mainWindow.setWindowLayoutFullScreen(true);
  }

  onWindowStageDestroy(): void {
    hilog.info(0x0000, 'kuikly', '%{public}s', 'Ability onWindowStageDestroy');
  }

  onForeground(): void {
    hilog.info(0x0000, 'kuikly', '%{public}s', 'Ability onForeground');
  }

  onBackground(): void {
    hilog.info(0x0000, 'kuikly', '%{public}s', 'Ability onBackground');
  }
}
