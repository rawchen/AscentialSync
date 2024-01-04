package com.lundong.ascentialsync.entity.spend;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import java.util.List;

/**
 * @author RawChen
 * @date 2023-06-01 15:12
 */
@Data
public class ReimburseData {

	@JSONField(name = "allocations")
	private List<Allocation> allocations;

	@JSONField(name = "pay_pool_logs")
	private List<PayPoolLog> payPoolLogs;

	/**
	 * 发票报销行
	 */
	@JSONField(name = "invoice_detail_list")
	private List<InvoiceDetail> invoiceDetailList;

	@JSONField(name = "amount")
	private Double amount;


	@JSONField(name = "applicant_time")
	private String applicantTime;

	@JSONField(name = "applicant_uid")
	private String applicantUid;

	@JSONField(name = "apply_reason")
	private String applyReason;

	@JSONField(name = "approved_time")
	private String approvedTime;

	@JSONField(name = "biz_cls_code")
	private String bizClsCode;

	@JSONField(name = "biz_unit_code")
	private String bizUnitCode;

	@JSONField(name = "company_code")
	private String companyCode;

	@JSONField(name = "company_name")
	private String companyName;

	@JSONField(name = "currency_code")
	private String currencyCode;

	@JSONField(name = "department_id")
	private String departmentId;

	@JSONField(name = "department_name")
	private String departmentName;

	@JSONField(name = "doc_type")
	private String docType;

	@JSONField(name = "form_code")
	private String formCode;

	@JSONField(name = "form_header_id")
	private String formHeaderId;

	@JSONField(name = "index_time")
	private String indexTime;

	@JSONField(name = "invoice_des")
	private String invoiceDes;

	@JSONField(name = "invoice_number")
	private String invoiceNumber;

	@JSONField(name = "process_status")
	private String processStatus;

	@JSONField(name = "source_id")
	private String sourceId;

	@JSONField(name = "source_system")
	private String sourceSystem;

	@JSONField(name = "submitter_uid")
	private String submitterUid;

	@JSONField(name = "cost_center_code")
	private String costCenterCode;

	@JSONField(name = "cost_center_name")
	private String costCenterName;

	@JSONField(name = "version")
	private String version;

	@JSONField(name = "project_code")
	private String projectCode;

	@JSONField(name = "project_name")
	private String projectName;


}
