package com.lundong.ascentialsync.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author RawChen
 * @date 2023-03-06 15:16
 */
@Data
@Accessors(chain = true)
@Builder
public class ExcelHeader {

	/**
	 * 日记账分录
	 */
	@ExcelProperty(index = 0)
	private String journalEntry;

	@ExcelProperty(index = 1)
	private String companyCode;

	/**
	 * 审批结束日期
	 */
	@ExcelProperty(index = 2)
	private String postingDate;

	/**
	 * 发起日期
	 */
	@ExcelProperty(index = 3)
	private String documentDate;

	/**
	 * 会计凭证类型 EF
	 */
	@ExcelProperty(index = 4)
	private String accountingDocumentType;

	/**
	 * 员工ID+feishu
	 */
	@ExcelProperty(index = 5)
	private String documentHeaderText;

	/**
	 * 文档参考ID-报销单单号
	 */
	@ExcelProperty(index = 6)
	private String documentReferenceID;

	/**
	 * 交易货币
	 */
	@ExcelProperty(index = 7)
	private String transactionCurrency;

}
