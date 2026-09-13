// @keepTs
// @ts-nocheck
import { ErrorReason } from "./j";
/**
 * 此接口用于监听渲染时出现的异常
 */
export interface IKuiklyRenderExceptionListener {
    onRenderException(exception: Error, errorReason: ErrorReason): void;
}
