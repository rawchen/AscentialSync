package com.lundong.ascentialsync.entity.spend;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

/**
 * @author RawChen
 * @date 2023-06-01 16:12
 */
@Data
public class InvoiceLineDetail {

	@JSONField(name = "invoice_line_deductible")
	private Boolean invoiceLineDeductible;

	@JSONField(name = "invoice_line_deductible_exclude_tax_amount")
	private String invoiceLineDeductibleExcludeTaxAmount;

	@JSONField(name = "invoice_line_deductible_tax_amount")
	private String invoiceLineDeductibleTaxAmount;

	@JSONField(name = "invoice_line_deductible_tax_rate")
	private String invoiceLineDeductibleTaxRate;

	@JSONField(name = "invoice_line_exclude_tax_amount")
	private String invoiceLineExcludeTaxAmount;

	@JSONField(name = "invoice_line_gross_amount")
	private String invoiceLineGrossAmount;

	@JSONField(name = "invoice_line_id")
	private String invoiceLineId;

	@JSONField(name = "invoice_line_non_deductible_reason_code")
	private String invoiceLineNonDeductibleReasonCode;

	@JSONField(name = "invoice_line_tax_amount")
	private String invoiceLineTaxAmount;

	@JSONField(name = "invoice_line_tax_rate")
	private String invoiceLineTaxRate;
}
