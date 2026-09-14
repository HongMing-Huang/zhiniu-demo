#import "KRBridgeModule.h"
#import "KuiklyRenderViewController.h"
#import "KuiklyContextParam.h"
#import "KuiklyRenderView.h"
#import "KuiklyRenderThreadManager.h"
#import <SDWebImage/SDWebImageManager.h>
#import <SDWebImage/SDWebImageDownloader.h>
#import <SDWebImage/SDImageCache.h>

@implementation KRBridgeModule

@synthesize hr_rootView;

#pragma mark - Page Navigation

- (void)closePage:(NSDictionary *)args {
    UIViewController *vc = [self viewController];
    [vc.navigationController popViewControllerAnimated:YES];
}

- (void)openPage:(NSDictionary *)args {
    NSDictionary *params = [self parseParams:args];
    NSString *pageName = params[@"pageName"] ?: params[@"url"];
    NSMutableDictionary *pageData = [params[@"pageData"] mutableCopy] ?: [NSMutableDictionary new];
    KuiklyRenderViewController *vc = [[KuiklyRenderViewController alloc] initWithPageName:pageName pageData:pageData];
    [[self viewController].navigationController pushViewController:vc animated:YES];
}

#pragma mark - Clipboard

- (void)copyToPasteboard:(NSDictionary *)args {
    NSDictionary *params = [self parseParams:args];
    NSString *content = params[@"content"];
    [UIPasteboard generalPasteboard].string = content;
}

#pragma mark - Logging

- (void)log:(NSDictionary *)args {
    NSDictionary *params = [self parseParams:args];
    NSString *content = params[@"content"];
    NSLog(@"KuiklyRender: %@", content);
}

#pragma mark - Image Cache

- (void)getLocalImagePath:(NSDictionary *)args {
    NSDictionary *params = [self parseParams:args];
    NSString *urlStr = params[@"imageUrl"];
    NSURL *url = [NSURL URLWithString:urlStr];

    [[SDWebImageDownloader sharedDownloader] downloadImageWithURL:url
                                                          options:0
                                                         progress:nil
                                                        completed:^(UIImage * _Nullable image, NSData * _Nullable data, NSError * _Nullable error, BOOL finished) {
        if (image) {
            NSString *key = [[SDWebImageManager sharedManager] cacheKeyForURL:url];
            [[SDImageCache sharedImageCache] storeImage:image imageData:data forKey:key toDisk:YES completion:^{
                NSString *path = [[SDImageCache sharedImageCache] cachePathForKey:key];
                KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
                if (callback) {
                    callback(@{@"localPath": path ?: @""});
                }
            }];
        }
    }];
}

#pragma mark - Asset File

- (void)readAssetFile:(NSDictionary *)args {
    NSDictionary *params = [self parseParams:args];
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    NSString *path = params[@"assetPath"];
    KuiklyContextParam *contextParam = ((KuiklyRenderView *)self.hr_rootView).contextParam;
    NSURL *pathUrl = [contextParam urlForFileName:[path stringByDeletingPathExtension] extension:[path pathExtension]];
    dispatch_async(dispatch_get_global_queue(0, 0), ^{
        NSError *error;
        NSString *jsonStr = [NSString stringWithContentsOfURL:pathUrl encoding:NSUTF8StringEncoding error:&error];
        NSDictionary *result = @{
            @"result": jsonStr ?: @"",
            @"error": error.description ?: @""
        };
        if (callback) {
            callback(result);
        }
    });
}

#pragma mark - HTTP Gateway（知牛网络桥）

/* 知牛 shared 层的网络传输桥：NSURLSession 标准 API（completion 队列与 Kotlin 侧无耦合）。
 * args: url / method(GET|POST) / body(POST JSON 文本，可空)
 * callback: { result: 响应文本, error: 错误描述, statusCode: 状态码 }
 * KRBridgeModule（Kuikly module callback）会把回调编组回 Context 线程，Kotlin 侧安全。 */
- (void)httpRequest:(NSDictionary *)args {
    NSDictionary *params = [self parseParams:args];
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    NSString *urlStr = params[@"url"];
    NSString *method = params[@"method"] ?: @"GET";
    NSString *body = params[@"body"];

    NSURL *url = [NSURL URLWithString:urlStr];
    if (!url || !callback) {
        if (callback) {
            callback(@{@"result": @"", @"error": @"invalid url or callback", @"statusCode": @(-1)});
        }
        return;
    }
    NSMutableURLRequest *req = [NSMutableURLRequest requestWithURL:url];
    req.HTTPMethod = method;
    req.timeoutInterval = 60.0;
    if (body.length > 0 && [method caseInsensitiveCompare:@"POST"] == NSOrderedSame) {
        [req setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
        req.HTTPBody = [body dataUsingEncoding:NSUTF8StringEncoding];
    }
    NSURLSessionDataTask *task = [[NSURLSession sharedSession] dataTaskWithRequest:req
                                                                completionHandler:^(NSData *data, NSURLResponse *resp, NSError *error) {
        NSString *text = data ? [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] : @"";
        NSInteger status = 0;
        if ([resp isKindOfClass:[NSHTTPURLResponse class]]) {
            status = ((NSHTTPURLResponse *)resp).statusCode;
        }
        NSDictionary *result = @{
            @"result": text ?: @"",
            @"error": error.localizedDescription ?: @"",
            @"statusCode": @(status)
        };
        // 回调必须编组回 Context 线程（NSURLSession completion 在自有队列触发，
        // Kuikly 的 Kotlin 回调恢复依赖 Context 线程时序）
        [KuiklyRenderThreadManager performOnContextQueueWithBlock:^{
            if (callback) { callback(result); }
        }];
    }];
    [task resume];
}

/* 知牛同步版 HTTP：syncCallNative 直返结果 JSON（在 Context 线程阻塞执行）。
 * 用于短请求（localhost 行情/搜索，百毫秒级）；LLM 长请求仍走异步 httpRequest。 */
- (NSString *)httpRequestSync:(NSDictionary *)args {
    NSDictionary *params = [self parseParams:args];
    NSString *urlStr = params[@"url"];
    NSString *method = params[@"method"] ?: @"GET";
    NSString *body = params[@"body"];
    NSURL *url = [NSURL URLWithString:urlStr];
    if (!url) {
        return @"{\"error\":\"invalid url\",\"statusCode\":-1}";
    }
    NSMutableURLRequest *req = [NSMutableURLRequest requestWithURL:url];
    req.HTTPMethod = method;
    req.timeoutInterval = 20.0;
    if (body.length > 0 && [method caseInsensitiveCompare:@"POST"] == NSOrderedSame) {
        [req setValue:@"application/json" forHTTPHeaderField:@"Content-Type"];
        req.HTTPBody = [body dataUsingEncoding:NSUTF8StringEncoding];
    }
    __block NSData *data = nil;
    __block NSString *errText = @"";
    __block NSInteger status = 0;
    dispatch_semaphore_t sem = dispatch_semaphore_create(0);
    NSURLSessionDataTask *syncTask = [[NSURLSession sharedSession] dataTaskWithRequest:req
                                                                    completionHandler:^(NSData *d, NSURLResponse *resp, NSError *error) {
        data = d;
        status = ([resp isKindOfClass:[NSHTTPURLResponse class]]) ? ((NSHTTPURLResponse *)resp).statusCode : 0;
        errText = error.localizedDescription ?: @"";
        dispatch_semaphore_signal(sem);
    }];
    [syncTask resume];
    dispatch_semaphore_wait(sem, dispatch_time(DISPATCH_TIME_NOW, 25LL * NSEC_PER_SEC));
    NSString *text = data ? [[NSString alloc] initWithData:data encoding:NSUTF8StringEncoding] : @"";
    NSDictionary *result = @{
        @"result": text ?: @"",
        @"error": errText ?: (text ? @"" : @"timeout or empty response"),
        @"statusCode": @(status)
    };
    NSData *json = [NSJSONSerialization dataWithJSONObject:result options:0 error:nil];
    return json ? [[NSString alloc] initWithData:json encoding:NSUTF8StringEncoding] : @"{\"error\":\"serialize fail\",\"statusCode\":-1}";
}

#pragma mark - Helpers

- (NSDictionary *)parseParams:(NSDictionary *)args {
    id param = args[KR_PARAM_KEY];
    if ([param isKindOfClass:[NSDictionary class]]) {
        return param;
    }
    if ([param isKindOfClass:[NSString class]]) {
        NSData *data = [(NSString *)param dataUsingEncoding:NSUTF8StringEncoding];
        if (data) {
            id json = [NSJSONSerialization JSONObjectWithData:data options:0 error:nil];
            if ([json isKindOfClass:[NSDictionary class]]) {
                return json;
            }
        }
    }
    return @{};
}

- (UIViewController *)viewController {
    UIView *view = self.hr_rootView;
    UIResponder *responder = view;
    while (responder) {
        if ([responder isKindOfClass:[UIViewController class]]) {
            return (UIViewController *)responder;
        }
        responder = [responder nextResponder];
    }
    return nil;
}

@end
