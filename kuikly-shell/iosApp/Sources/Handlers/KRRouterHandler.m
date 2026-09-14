#import "KRRouterHandler.h"
#import "KuiklyRenderViewController.h"

@implementation KRRouterHandler

+ (void)load {
    [KRRouterModule registerRouterHandler:[self new]];
}

- (void)openPageWithName:(NSString *)pageName pageData:(NSDictionary *)pageData controller:(UIViewController *)controller {
    KuiklyRenderViewController *vc = [[KuiklyRenderViewController alloc] initWithPageName:pageName pageData:pageData];
    UINavigationController *nav = controller.navigationController;
    // 底部 Tab 级切换：无转场动画 + 折叠导航栈为 [根, 当前 Tab]（正常 App 的 Tab 行为）
    BOOL isTab = [pageData[@"__tab"] isKindOfClass:[NSString class]] &&
                 [pageData[@"__tab"] isEqualToString:@"1"];
    if (isTab && nav != nil && nav.viewControllers.count > 0) {
        NSMutableArray *vcs = [NSMutableArray arrayWithArray:nav.viewControllers];
        if (vcs.count > 1) {
            [vcs removeLastObject];
        }
        [vcs addObject:vc];
        [nav setViewControllers:vcs animated:NO];
        return;
    }
    [nav pushViewController:vc animated:YES];
}

- (void)closePage:(UIViewController *)controller {
    if (controller.navigationController.viewControllers.count == 1) {
        [controller.navigationController dismissViewControllerAnimated:NO completion:nil];
    } else {
        [controller.navigationController popViewControllerAnimated:YES];
    }
}

@end
