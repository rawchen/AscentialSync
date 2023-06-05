package com.lundong.ascentialsync.entity;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author RawChen
 * @date 2023-04-24 16:45
 */
@Data
public class FeishuSpendForm {

    @JSONField(name = "applicant_id")
    private String applicantId;

    @JSONField(name = "applicant_name")
    private String applicantName;

    @JSONField(name = "approval_status")
    private String approvalStatus;

    @JSONField(name = "biz_cls_id")
    private String bizClsId;

    @JSONField(name = "biz_cls_name")
    private String bizClsName;

    @JSONField(name = "biz_unit_code")
    private String bizUnitCode;

    @JSONField(name = "biz_unit_name")
    private String bizUnitName;

    @JSONField(name = "company_code")
    private String companyCode;

    @JSONField(name = "company_name")
    private String companyName;

    @JSONField(name = "form_code")
    private String formCode;

    @JSONField(name = "form_id")
    private String formId;

    @JSONField(name = "submit_time")
    private String submitTime;

    @JSONField(name = "currency_code")
    private String currencyCode;

    @JSONField(name = "currency_desc")
    private String currencyDesc;




}
