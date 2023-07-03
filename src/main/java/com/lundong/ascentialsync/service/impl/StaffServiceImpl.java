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
import com.lundong.ascentialsync.service.StaffService;
import com.lundong.ascentialsync.util.SftpUtil;
import com.lundong.ascentialsync.util.SignUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 同步员工数据
 *
 * @author RawChen
 * @date 2023-05-12 14:24
 */
@Service
@Slf4j
public class StaffServiceImpl implements StaffService {

	@Autowired
	private Constants constants;

	@Override
	@Scheduled(cron = "0 0 1 ? * *")
	public void syncStaffData() {
		SftpUtil sftpUtil = new SftpUtil(constants.SFTP_USER_ID, constants.SFTP_PASSWORD, constants.SFTP_HOST, 22);
		sftpUtil.login();
		String fileName = "WorkdayFeishu_" + LocalDateTimeUtil.format(LocalDate.now().minusDays(1), "ddMMyyyy") + ".csv";
		InputStream inputStream = sftpUtil.downloadStream("workday2feishu", fileName);
		if (inputStream == null) {
			log.info("无该日期员工同步数据：{}", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			return;
		}
		// SFTP用户Excel列表数据查询
		List<ExcelUser> excelUsers = new ArrayList<>();
		try {
			EasyExcel.read(inputStream, ExcelUser.class, new ReadListener<ExcelUser>() {
				@Override
				public void invoke(ExcelUser user, AnalysisContext context) {
					excelUsers.add(user);
				}
				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
				}
			}).excelType(ExcelTypeEnum.CSV).sheet().headRowNumber(1).doRead();
		} catch (Exception e) {
			e.printStackTrace();
		}

		log.info("需要同步的员工数: {}", excelUsers.size());

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
		List<Boolean> resultList = new ArrayList<>();
		for (FeishuUser feishuUser : collect) {
			boolean r = SignUtil.updateFeishuUser(feishuUser, companyCodeAttrId, costCenterCodeAttrId);
			resultList.add(r);
		}
		List<Boolean> resultFilterList = resultList.stream().filter(r -> r).collect(Collectors.toList());
		log.info("修改成功的员工数: {}", resultFilterList.size());
		if (resultFilterList.size() > 0) {
			// 至少成功修改一个用户的数据
			sftpUtil.moveFile("workday2feishu", fileName);
		}
	}
}
