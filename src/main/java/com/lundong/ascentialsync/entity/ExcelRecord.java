package com.lundong.ascentialsync.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Builder;
import lombok.Data;

/**
 * @author RawChen
 * @date 2023-03-06 15:16
 */
@Data
@Builder
public class ExcelRecord {

	/**
	 * 日记账分录
	 */
	@ExcelProperty(index = 0)
	private String journalEntry;

	/**
	 * 公司代码
	 */
	@ExcelProperty(index = 1)
	private String companyCode;

	/**
	 * GL账户跟费用类型对应(维护方式自定义纬度)
	 */
	@ExcelProperty(index = 2)
	private String glAccount;

	/**
	 * 债权人
	 */
	@ExcelProperty(index = 3)
	private String creditor;

	/**
	 * 交易金额
	 */
	@ExcelProperty(index = 4)
	private String amountInTransactionCurrency;

	/**
	 * 报销行备注
	 */
	@ExcelProperty(index = 5)
	private String documentItemText;

	/**
	 * 借记代码
	 */
	@ExcelProperty(index = 6)
	private String debitCreditCode;

	/**
	 * 成本中心，对应员工的成本中心
	 */
	@ExcelProperty(index = 7)
	private String costCenter;

	/**
	 * WBS元素-报销单项目代码
	 */
	@ExcelProperty(index = 8)
	private String wbsElement;

	/**
	 * 分配引用
	 */
	@ExcelProperty(index = 9)
	private String assignmentReference;

	/**
	 * 税码
	 */
	@ExcelProperty(index = 10)
	private String taxCode;

	/**
	 * 税金
	 */
	@ExcelProperty(index = 11)
	private String taxAmount;

	/**
	 * 交付中心
	 */
	@ExcelProperty(index = 12)
	private String deliveryCentre;

}
