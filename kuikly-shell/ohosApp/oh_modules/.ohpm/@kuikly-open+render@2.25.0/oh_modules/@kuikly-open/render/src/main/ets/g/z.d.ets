// @keepTs
// @ts-nocheck
export interface IKRLogAdapter {
    /**
     * 打印 debug 级别日志
     * @param tag 日志 tag
     * @param msg 日志信息
     */
    d(tag: string, msg: string): void;
    /**
     * 打印 info 级别日志
     * @param tag 日志 tag
     * @param msg 日志信息
     */
    i(tag: string, msg: string): void;
    /**
     * 打印 error 级别日志
     * @param tag 日志 tag
     * @param msg 日志信息
     */
    e(tag: string, msg: string): void;
}
