package com.lundong.ascentialsync.entity.spend;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author shuangquan.chen
 * @date 2024-01-04 11:46
 */
@Data
public class PayPoolLog {

    @JSONField(name = "pay_pool_id")
    private String payPoolId;

    @JSONField(name = "form_pay_detail_id")
    private String formPayDetailId;

    @JSONField(name = "field_key")
    private String fieldKey;

    @JSONField(name = "field_name")
    private String fieldName;

    @JSONField(name = "before_value")
    private String beforeValue;

    @JSONField(name = "after_value")
    private String afterValue;

    @JSONField(name = "reason")
    private String reason;

    @JSONField(name = "creator")
    private String creator;

    @JSONField(name = "create_time")
    private String createTime;
}
