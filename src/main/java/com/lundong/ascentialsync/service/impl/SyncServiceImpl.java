package com.lundong.ascentialsync.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.lundong.ascentialsync.config.Constants;
import com.lundong.ascentialsync.entity.CustomAttr;
import com.lundong.ascentialsync.entity.ExcelUser;
import com.lundong.ascentialsync.entity.FeishuUser;
import com.lundong.ascentialsync.service.SyncService;
import com.lundong.ascentialsync.util.SftpUtil;
import com.lundong.ascentialsync.util.SignUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author RawChen
 * @date 2023-05-12 14:24
 */
@Service
public class SyncServiceImpl implements SyncService {
	@Override
	@Scheduled(cron = "0 0 1 ? * *")
	public void syncStaffData() {
		SftpUtil sftpUtil = new SftpUtil(Constants.SFTP_USER_ID, Constants.SFTP_PASSWORD, Constants.SFTP_HOST, 22);
		sftpUtil.login();
		InputStream inputStream = sftpUtil.downloadStream("workday2feishu", "staff-" + LocalDateTimeUtil.format(LocalDate.now(), "yyyyMMdd") + ".csv");

		// SFTP用户Excel列表数据查询
		List<ExcelUser> excelUsers = new ArrayList<>();
		try {
			EasyExcel.read(inputStream, ExcelUser.class, new ReadListener<ExcelUser>() {
				@Override
				public void invoke(ExcelUser user, AnalysisContext context) {
					excelUsers.add(user);
					System.out.println(user);
				}
				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
				}
			}).excelType(ExcelTypeEnum.CSV).sheet().headRowNumber(2).doRead();
		} catch (Exception e) {
			e.printStackTrace();
		}

		ArrayList<String> employeeNumbers = new ArrayList<>();
		for (ExcelUser excelUser : excelUsers) {
			employeeNumbers.add(excelUser.getEmployeeId());
		}

		// 飞书人事（标准版）花名册基础信息列表
		List<FeishuUser> feishuBaseEmployees = SignUtil.getFeishuBaseEmployees();

		List<FeishuUser> collect = feishuBaseEmployees.stream().filter(e -> employeeNumbers.contains(e.getEmployeeNo())).collect(Collectors.toList());
		for (FeishuUser feishuUser : collect) {
			for (ExcelUser excelUser : excelUsers) {
				if (feishuUser.getEmployeeNo().equals(excelUser.getEmployeeId())) {
					feishuUser.setCostCenterCode(excelUser.getCostCenter());
					feishuUser.setCompanyCode(excelUser.getCompanyCode());
					break;
				}
			}
		}
		System.out.println("collect size:" + collect.size());
		String companyCodeAttrId = "";
		String costCenterCodeAttrId = "";
		List<CustomAttr> customAttrList = SignUtil.getCustomAttrs();
		for (CustomAttr customAttr : customAttrList) {
			if ("Company Code".equals(customAttr.getValue())) {
				companyCodeAttrId = customAttr.getId();
				break;
			}
		}
		for (CustomAttr customAttr : customAttrList) {
			if ("Cost Center Code".equals(customAttr.getValue())) {
				costCenterCodeAttrId = customAttr.getId();
				break;
			}
		}
		// 飞书用户设置字段后做修改，如果该用户不存在就抛出错误
		for (FeishuUser feishuUser : collect) {
			SignUtil.updateFeishuUser(feishuUser, companyCodeAttrId, costCenterCodeAttrId);
		}
	}
}
