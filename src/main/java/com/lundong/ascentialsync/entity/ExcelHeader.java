package com.lundong.ascentialsync.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @author RawChen
 * @date 2023-03-06 15:16
 */
@Data
public class ExcelHeader {

	@ExcelProperty(index = 0)
	private String journalEntry;

	@ExcelProperty(index = 1)
	private String companyCode;

	@ExcelProperty(index = 2)
	private String postingDate;

	@ExcelProperty(index = 3)
	private String documentDate;

	@ExcelProperty(index = 4)
	private String accountingDocumentType;

	@ExcelProperty(index = 5)
	private String documentHeaderText;

	@ExcelProperty(index = 6)
	private String documentReferenceID;

	@ExcelProperty(index = 7)
	private String transactionCurrency;

}
