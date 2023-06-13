package com.lundong.ascentialsync.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FeishuPaypool {

    /**
     * 支付池ID
     */
    @JSONField(name = "id")
    private String id;

    /**
     * payment_status_code
     *     A1：待支付
     *     A2：提交资金
     *     A3：支付中
     *     A4：支付异常
     *     A5：支付成功
     */
    @JSONField(name = "payment_status_code")
    private String paymentStatusCode;

    /**
     * 申请人(union_id)
     */
    @JSONField(name = "applicant")
    private String applicant;

    /**
     * 付款人(union_id)
     */
    @JSONField(name = "payment_person")
    private String paymentPerson;

    /**
     * 会计，出纳(union_id)
     */
    @JSONField(name = "accountant")
    private String accountant;
}
