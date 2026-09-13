import { KRNativeRenderController, KRRecord, Kuikly } from '@kuikly-open/render';
import globalNativeManager from '../kuikly/MyNativeManager';
import { KuiklyViewDelegate } from '../kuikly/KuiklyViewDelegate';
import router from '@ohos.router';
import fs from '@ohos.file.fs';

const COMMON_ASSETS: string[] = [
  'brand/logo-mark.png',
  'icons/ai.png',
  'icons/back.png',
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
  'icons/more.png',
  'icons/search.png',
  'icons/send.png',
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

  private prepareCommonAssets(): string {
    const context = getContext(this);
    const root = `${context.filesDir}/kuikly-assets`;
    fs.mkdirSync(`${root}/common/brand`, true);
    fs.mkdirSync(`${root}/common/icons`, true);
    COMMON_ASSETS.forEach((relativePath: string) => {
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
