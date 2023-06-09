package com.lundong.ascentialsync.entity.spend;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

/**
 * @author RawChen
 * @date 2023-06-01 16:05
 */
@Data
public class InvoiceDetail {

	@JSONField(name = "invoice_line_detail_list")
	private List<InvoiceLineDetail> invoiceLineDetailList;

	@JSONField(name = "currency_code")
	private String currencyCode;

	@JSONField(name = "deductible")
	private Boolean deductible;

	@JSONField(name = "deductible_exclude_tax_amount")
	private String deductibleExcludeTaxAmount;

	@JSONField(name = "deductible_tax_amount")
	private String deductibleTaxAmount;

	/**
	 * 不含税金额
	 */
	@JSONField(name = "exclude_tax_amount")
	private String excludeTaxAmount;

	@JSONField(name = "gross_amount")
	private String grossAmount;

	@JSONField(name = "invoice_code")
	private String invoiceCode;

	@JSONField(name = "invoice_no")
	private String invoiceNo;

	@JSONField(name = "invoice_tax_amount")
	private String invoiceTaxAmount;

	@JSONField(name = "invoice_tax_rate")
	private String invoiceTaxRate;

	@JSONField(name = "invoice_type_code")
	private String invoiceTypeCode;

	@JSONField(name = "invoice_type_description")
	private String invoiceTypeDescription;

	@JSONField(name = "invoice_union_id")
	private String invoiceUnionId;

	@JSONField(name = "non_deductible_reason_code")
	private String nonDeductibleReasonCode;

	@JSONField(name = "reimburse_line_id")
	private String reimburseLineId;

	@JSONField(name = "tax_amount")
	private String taxAmount;

	@JSONField(name = "tax_rate")
	private String taxRate;

	@JSONField(name = "under_reimbursement")
	private Boolean underReimbursement;

	@JSONField(name = "project_code")
	private String projectCode;

}
