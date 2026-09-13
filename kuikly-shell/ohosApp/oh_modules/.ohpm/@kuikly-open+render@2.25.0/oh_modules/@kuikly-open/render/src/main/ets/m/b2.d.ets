// @keepTs
// @ts-nocheck
import { KuiklyRenderBaseModule } from "./d1/u1";
import { KRAny, KuiklyRenderCallback } from "./../e/f";
import p18 from "@ohos.net.http";
import q18 from '@ohos.url';
interface s18 {
    url: string;
    method: string;
    param: KRAny | null;
    headers: KRAny | null;
    cookie: string | null;
    timeout: number;
}
export declare class KRNetworkModule extends KuiklyRenderBaseModule {
    syncMode(): boolean;
    static readonly MODULE_NAME = "KRNetworkModule";
    private static readonly METHOD_HTTP_REQUEST;
    private static readonly METHOD_HTTP_REQUEST_BINARY;
    call(c20: string, d20: KRAny, e20: KuiklyRenderCallback | null): KRAny;
    onDestroy(): void;
    private httpRequest;
    createURLAndOptions(w18: s18, x18: Int8Array | null): [
        q18.URL,
        p18.HttpRequestOptions
    ] | null;
    private isContentTypeJson;
}
export {};
