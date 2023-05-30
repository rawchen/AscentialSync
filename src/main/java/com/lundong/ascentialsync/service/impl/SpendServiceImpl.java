package com.lundong.ascentialsync.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.lundong.ascentialsync.entity.ExcelRecord;
import com.lundong.ascentialsync.entity.ExcelHeader;
import com.lundong.ascentialsync.service.SpendService;
import com.lundong.ascentialsync.util.SftpUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author RawChen
 * @date 2023-05-12 14:24
 */
@Service
public class SpendServiceImpl implements SpendService {
	@Override
	@Scheduled(cron = "0 0 1 ? * *")
	public void syncFormData() {
		SftpUtil sftpUtil = new SftpUtil("", "", "", 22);
		InputStream inputStream = sftpUtil.downloadStream("directory", "fileName");

		// SFTP单据Excel单据头数据查询
		List<ExcelHeader> excelHeaders = new ArrayList<>();
		try {
			EasyExcel.read(inputStream, ExcelHeader.class, new AnalysisEventListener<ExcelHeader>() {
				@Override
				public void invoke(ExcelHeader header, AnalysisContext context) {
					excelHeaders.add(header);
				}
				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
				}
			}).sheet().headRowNumber(1).doRead();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// TODO 飞书费控表头更新

		// SFTP单据Excel单据列表数据查询
		List<ExcelRecord> excelRecords = new ArrayList<>();
		try {
			EasyExcel.read(inputStream, ExcelRecord.class, new AnalysisEventListener<ExcelRecord>() {
				@Override
				public void invoke(ExcelRecord record, AnalysisContext context) {
					excelRecords.add(record);
				}
				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
				}
			}).sheet().headRowNumber(3).doRead();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// TODO 飞书费控单据新增
	}


	/**
	 * 每天定时同步当天新增的新数据
	 */
	@Override
	@Scheduled(cron = "0 0 1 ? * *")
	public void syncSpendVouchers() {

	}
}
