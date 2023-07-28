package com.lundong.ascentialsync.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @author RawChen
 * @date 2023-03-06 15:16
 */
@Data
public class ExcelPaypool {

	@ExcelProperty(index = 0)
	private String supplier;

	/**
	 * 单据编号
	 */
	@ExcelProperty(index = 2)
	private String documentReferenceId;

}
