package com.lundong.ascentialsync.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @author RawChen
 * @date 2023-03-06 15:16
 */
@Data
public class ExcelUser implements Comparable<ExcelUser> {

	@ExcelProperty(index = 0)
	private String employeeId;

	@ExcelProperty(index = 1)
	private String companyCode;

	@ExcelProperty(index = 2)
	private String costCenter;

	@ExcelProperty(index = 4)
	private String ManagerId;




	@Override
	public int compareTo(ExcelUser o) {
		String str = o.getEmployeeId().replace("/", "");
		if (employeeId.length() > str.length()) {
			return 0;
		} else {
			return 1;
		}
	}
}
