package com.lundong.ascentialsync.entity;

import com.alibaba.fastjson.annotation.JSONField;
import com.lundong.ascentialsync.entity.spend.InvoiceApplyData;
import com.lundong.ascentialsync.entity.spend.PayApplyData;
import com.lundong.ascentialsync.entity.spend.ReimburseData;
import lombok.Data;

import java.util.List;

/**
 * 凭证列表
 *
 * @author RawChen
 * @date 2023-04-24 16:45
 */
@Data
public class FeishuSpendVoucher {

    /**
     * 核销数据
     */
    @JSONField(name = "invoice_apply_data")
    private List<InvoiceApplyData> invoiceApplyData;

    /**
     * 支付数据
     */
    @JSONField(name = "pay_apply_data")
    private List<PayApplyData> payApplyData;

    /**
     * 报账数据
     */
    @JSONField(name = "reimburse_data")
    private List<ReimburseData> reimburseData;
}
