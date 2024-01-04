package com.lundong.ascentialsync.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.lundong.ascentialsync.entity.spend.PayPoolLog;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

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

    /**
     * 业务单据表头编号(form_code)
     */
    @JSONField(name = "vendor_form_header_code")
    private String vendorFormHeaderCode;

    @JSONField(name = "pay_pool_logs")
    private List<PayPoolLog> payPoolLogs;
}
