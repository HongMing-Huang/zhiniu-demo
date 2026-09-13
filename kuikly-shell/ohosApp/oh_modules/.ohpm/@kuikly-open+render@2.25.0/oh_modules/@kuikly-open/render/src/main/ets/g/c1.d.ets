// @keepTs
// @ts-nocheck
export interface IKRColorParserAdapter {
    /**
     * Convert color from string
     * @param colorStr
     * @returns color object of type string and number should conform to ResourceColor [https://developer.huawei.com/consumer/cn/doc/harmonyos-references/ts-types-0000001774121374#ZH-CN_TOPIC_0000001774121374__resourcecolor]
     */
    toColor(colorStr: string): ResourceColor;
}
