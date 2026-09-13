// @keepTs
// @ts-nocheck
/**
 * 异常原因
 */
export declare enum ErrorReason {
    UNKNOWN = 0,
    INITIALIZE = 1,
    CALL_KOTLIN = 2,
    CALL_NATIVE = 3,
    UPDATE_VIEW_TREE = 4
}
export declare class KuiklyRenderModuleExportError extends Error {
    constructor(n4: string);
}
export declare class KuiklyRenderViewExportError extends Error {
    constructor(m4: string);
}
export declare class KuiklyRenderShadowExportError extends Error {
    constructor(l4: string);
}
export declare class KRNativeBizError extends Error {
    constructor(k4: string);
}
