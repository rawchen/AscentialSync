package com.lundong.ascentialsync.entity.spend;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

/**
 * @author RawChen
 * @date 2023-06-01 15:25
 */
@Data
public class Allocation {

	@JSONField(name = "alloc_line_num")
	private Integer allocLineNum;

	@JSONField(name = "applicant_uid")
	private String applicantUid;

	@JSONField(name = "biz_type_code")
	private String bizTypeCode;

	@JSONField(name = "company_code")
	private String companyCode;

	@JSONField(name = "department_id")
	private String departmentId;

	@JSONField(name = "ex_tax_amount")
	private Integer exTaxAmount;

	@JSONField(name = "exchange_rate")
	private String exchangeRate;

	@JSONField(name = "form_code")
	private String formCode;

	@JSONField(name = "invoice_date")
	private String invoiceDate;

	@JSONField(name = "line_date")
	private String lineDate;

	@JSONField(name = "line_desc")
	private String lineDesc;

	@JSONField(name = "rate_date")
	private String rateDate;

	@JSONField(name = "reimburse_line_id")
	private String reimburseLineId;

	@JSONField(name = "reimbursement_amount")
	private Double reimbursementAmount;

	@JSONField(name = "source_id")
	private String sourceId;

	@JSONField(name = "tax_amount")
	private Double taxAmount;

	@JSONField(name = "tax_code")
	private String taxCode;

	@JSONField(name = "tax_rate")
	private String taxRate;

	@JSONField(name = "custom_field_list")
	private List<CustomField> customFieldList;

}
