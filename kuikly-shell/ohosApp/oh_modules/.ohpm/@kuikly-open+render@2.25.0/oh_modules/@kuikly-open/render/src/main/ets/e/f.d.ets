// @keepTs
// @ts-nocheck
import t22 from '@ohos.window';
export type KRValue = number | string | ArrayBuffer | Array<KRAny> | boolean | object;
export type KRArray = Array<KRValue | Record<string, KRValue>>;
export type KRRecord = Record<string, KRValue | KRArray | Record<string, KRValue | KRArray | Record<string, KRValue>>>;
export type KRAny = KRValue | KRArray | KRRecord | null;
export type KuiklyRenderCallback = (result: KRAny) => void;
export interface Insets {
    left: number;
    top: number;
    right: number;
    bottom: number;
}
export interface AvoidAreaChangeParams {
    type: t22.AvoidAreaType;
    area: t22.AvoidArea;
}
