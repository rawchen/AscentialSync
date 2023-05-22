package com.lundong.ascentialsync.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.lundong.ascentialsync.entity.ExcelUser;
import com.lundong.ascentialsync.entity.FeishuUser;
import com.lundong.ascentialsync.service.SyncService;
import com.lundong.ascentialsync.util.SftpUtil;
import com.lundong.ascentialsync.util.SignUtil;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author RawChen
 * @date 2023-05-12 14:24
 */
public class SyncServiceImpl implements SyncService {
	@Override
	@Scheduled(cron = "0 0 1 ? * *")
	public void syncStaffData() {
		SftpUtil sftpUtil = new SftpUtil("", "", "", 22);
		InputStream inputStream = sftpUtil.downloadStream("directory", "fileName");

		// SFTP用户Excel列表数据查询
		List<ExcelUser> excelUsers = new ArrayList<>();
		try {
			EasyExcel.read(inputStream, ExcelUser.class, new AnalysisEventListener<ExcelUser>() {
				@Override
				public void invoke(ExcelUser user, AnalysisContext context) {
					excelUsers.add(user);
				}
				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
				}
			}).sheet().headRowNumber(3).doRead();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// 飞书用户列表查询（接口查）
		List<FeishuUser> feishuUsers = SignUtil.findByDepartment();

		// 字段确认
		for (ExcelUser excelUser : excelUsers) {
			for (FeishuUser feishuUser : feishuUsers) {
				if (excelUser.getEmployeeId().equals(feishuUser.getUserId())) {
					// TODO 从映射表获取相关数据
					feishuUser.setEmployeeNo(excelUser.getCompanyCode());
					feishuUser.setLeaderUserId(excelUser.getManagerId());
					feishuUser.setWorkStation(excelUser.getCostCenter());
					feishuUser.setDepartmentId(excelUser.getCompanyCode());
					break;
				}
			}
		}
	}
}
