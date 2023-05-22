package com.lundong.ascentialsync.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @author RawChen
 * @date 2023-03-06 15:16
 */
@Data
public class ExcelRecord {

	@ExcelProperty(index = 0)
	private String journalEntry;

	@ExcelProperty(index = 1)
	private String companyCode;

	@ExcelProperty(index = 2)
	private String glAccount;

	@ExcelProperty(index = 3)
	private String creditor;

	@ExcelProperty(index = 4)
	private String amountInTransactionCurrency;

	@ExcelProperty(index = 5)
	private String documentItemText;

	@ExcelProperty(index = 6)
	private String debitCreditCode;

	@ExcelProperty(index = 7)
	private String costCenter;

	@ExcelProperty(index = 8)
	private String wbsElement;

	@ExcelProperty(index = 9)
	private String assignmentReference;

	@ExcelProperty(index = 10)
	private String taxCode;

	@ExcelProperty(index = 11)
	private String taxAmount;

	@ExcelProperty(index = 12)
	private String deliverCenter;

}
