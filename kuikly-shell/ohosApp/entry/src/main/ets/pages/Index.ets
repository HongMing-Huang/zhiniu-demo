import { KRNativeRenderController, KRRecord, Kuikly } from '@kuikly-open/render';
import globalNativeManager from '../kuikly/MyNativeManager';
import { KuiklyViewDelegate } from '../kuikly/KuiklyViewDelegate';
import router from '@ohos.router';
import fs from '@ohos.file.fs';
import common from '@ohos.app.ability.common';
import util from '@ohos.util';

const COMMON_ASSETS: string[] = [
  'brand/apple-touch-icon.png',
  'brand/favicon-32.png',
  'brand/logo-mark.png',
  'fonts/JetBrainsMono-Regular.ttf',
  'icons/ai.png',
  'icons/back.png',
  'icons/bell.png',
  'icons/calendar.png',
  'icons/chart.png',
  'icons/chat-message.png',
  'icons/check.png',
  'icons/chevron_down.png',
  'icons/chevron_right.png',
  'icons/chevron_up.png',
  'icons/close.png',
  'icons/data-display.png',
  'icons/error-triangle.png',
  'icons/filter-sort.png',
  'icons/filter.png',
  'icons/info.png',
  'icons/more.png',
  'icons/refresh.png',
  'icons/search.png',
  'icons/send.png',
  'icons/setting.png',
  'icons/star_filled.png',
  'icons/star.png',
  'icons/theme.png',
  'icons/user-circle.png'
];

@Entry
@Component
struct Index {
  private kuiklyViewDelegate = new KuiklyViewDelegate();
  private kuiklyController: KRNativeRenderController | null = null;
  private pageName: string | null = null;
  private pageData?: KRRecord;
  private assetsDir: string = '';
  @State showKuikly: boolean = true;

  // 资产清单来源：优先读 rawfile/common/manifest.txt（syncCommonAssets.sh 生成，随源目录自动更新），
  // 读不到时回退内置列表。单文件缺失只跳过该项，不再让 aboutToAppear 抛异常导致
  // assetsDir 为空、全端图标消失（曾出现“顶部和底栏只剩文字”的根因之一）。
  private commonAssetList(context: common.Context): string[] {
    try {
      const raw = context.resourceManager.getRawFileContentSync('common/manifest.txt');
      const text = util.TextDecoder.create('utf-8').decodeToString(new Uint8Array(raw.buffer));
      const lines = text.split('\n').map((l: string) => l.trim()).filter((l: string) => l.length > 0);
      if (lines.length > 0) {
        return lines;
      }
    } catch (e) {
      console.error('[zhiniu] manifest.txt missing, fallback to builtin list');
    }
    return COMMON_ASSETS;
  }

  private prepareCommonAssets(): string {
    const context = getContext(this);
    const root = `${context.filesDir}/kuikly-assets`;
    // mkdirSync 在目录已存在时可能抛 File exists（新 API 版本行为）——二次进入必须容错
    ['brand', 'icons', 'fonts'].forEach((dir: string) => {
      try {
        fs.mkdirSync(`${root}/common/${dir}`, true);
      } catch (e) {
        // 目录已存在：继续
      }
    });
    this.commonAssetList(context).forEach((relativePath: string) => {
      try {
        const content = context.resourceManager.getRawFileContentSync(`common/${relativePath}`);
        const file = fs.openSync(
          `${root}/common/${relativePath}`,
          fs.OpenMode.CREATE | fs.OpenMode.TRUNC | fs.OpenMode.READ_WRITE
        );
        try {
          fs.writeSync(file.fd, content.buffer, { length: content.length });
        } finally {
          fs.closeSync(file);
        }
      } catch (e) {
        console.error(`[zhiniu] rawfile missing, skipped: common/${relativePath}`);
      }
    });
    return root;
  }

  onBackPress(): boolean | void {
    if (this.kuiklyController) {
      return this.kuiklyController.onBackPress();
    }
  }

  aboutToAppear(): void {
    this.assetsDir = this.prepareCommonAssets();
    const params = router.getParams() as Record<string, Object>;
    if (params) {
      this.pageName = params.pageName as string;
      this.pageData = (params.pageData as KRRecord | null) ?? {};
    }
  }

  onPageShow(): void {
    this.kuiklyViewDelegate.pageDidAppear();
  }

  onPageHide(): void {
    this.kuiklyViewDelegate.pageDidDisappear();
  }

  build() {
    Stack() {
      if (this.showKuikly) {
        Kuikly({
          pageName: this.pageName ?? 'MarketList',
          pageData: this.pageData ?? {},
          assetsDir: this.assetsDir,
          delegate: this.kuiklyViewDelegate,
          onControllerReadyCallback: (controller) => {
            this.kuiklyController = controller;
          },
          nativeManager: globalNativeManager,
        });
      }
    }
    .width('100%')
    .height('100%')
    .expandSafeArea([SafeAreaType.KEYBOARD]);
  }
}
