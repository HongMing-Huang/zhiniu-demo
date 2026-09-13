// @keepTs
// @ts-nocheck
import { KRSize } from "../o1/q1";
import { KRAny, KRRecord, KRValue } from "./f";
export declare class KRConvertUtil {
    /**
     * 将任意 KRAny 类型的值转换为 native 侧能接收类型的值
     * @param value 要转换的 KRAny 类型的值。
     * @returns 转换后的 native侧能接收 类型的值。
     */
    static anyToCObject(s22: KRAny): KRValue | null;
    /**
     * 获取屏幕size
     */
    static screenSize(): KRSize;
    static toKRRecord(n22: KRAny): KRRecord;
    private static hasByteArrayElement;
}
