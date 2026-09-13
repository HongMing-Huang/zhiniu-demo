// @keepTs
// @ts-nocheck
export declare class KRColorParser {
    /**
     * Convert color from string
     * @param colorStr
     * @returns color object of type string and number should conform to ResourceColor [https://developer.huawei.com/consumer/cn/doc/harmonyos-references/ts-types-0000001774121374#ZH-CN_TOPIC_0000001774121374__resourcecolor]
     */
    static toColor(g22: string, h22?: boolean): ResourceColor;
    private static toColorInternal;
}
