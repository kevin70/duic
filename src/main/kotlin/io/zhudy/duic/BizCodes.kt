package io.zhudy.duic

import com.memeyule.cryolite.core.BizCode

/**
 * @author Kevin Zou (kevinz@weghst.com)
 */
enum class BizCodes(override val code: Int, override val msg: String, override val status: Int = 400) : BizCode {

    /**
     *
     */
    C_1000(1000, "未找到应用"),
    C_1001(1001, "未找到应用 profile"),
    C_1002(1001, "未找到配置项"),
    C_1003(1003, "修改 app content 失败"),
    C_1004(1004, "修改 app content 失败, 数据版本不一致"),
    C_1005(1005, "数据类型不一致无法合并"),
    C_1006(1006, "app content 不是有效的 yaml 格式"),

    //
    C_2000(2000, "用户不存在"),
    C_2001(2001, "密码不匹配")
    ;
}