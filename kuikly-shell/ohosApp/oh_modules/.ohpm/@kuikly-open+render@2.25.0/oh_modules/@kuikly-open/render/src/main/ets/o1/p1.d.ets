// @keepTs
// @ts-nocheck
export declare class KRRect {
    /** 矩形左上角的 x 坐标 */
    x: number;
    /** 矩形左上角的 y 坐标 */
    y: number;
    /** 矩形的宽度 */
    width: number;
    /** 矩形的高度 */
    height: number;
    static readonly zero: Readonly<KRRect>;
    /**
     * 创建一个新的 KRRect 实例。
     * @param x 矩形左上角的 x 坐标
     * @param y 矩形左上角的 y 坐标
     * @param width 矩形的宽度
     * @param height 矩形的高度
     */
    constructor(p4: number, q4: number, r4: number, s4: number);
    equals(o4: KRRect): boolean;
}
