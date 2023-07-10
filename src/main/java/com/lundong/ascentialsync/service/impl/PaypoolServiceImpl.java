package com.lundong.ascentialsync.service.impl;

import cn.hutool.core.date.LocalDateTimeUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.lundong.ascentialsync.config.Constants;
import com.lundong.ascentialsync.entity.ExcelPaypool;
import com.lundong.ascentialsync.entity.FeishuPaypool;
import com.lundong.ascentialsync.service.PaypoolService;
import com.lundong.ascentialsync.util.SftpUtil;
import com.lundong.ascentialsync.util.SignUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 同步待支付数据
 *
 * @author RawChen
 * @date 2023-06-13 12:01
 */
@Service
@Slf4j
public class PaypoolServiceImpl implements PaypoolService {

	@Autowired
	private Constants constants;

	/**
	 * 同步昨天一天支付池数据
	 */
	@Override
	@Scheduled(cron = "0 0 3 ? * *")
	public void syncPaypoolData() {
		SftpUtil sftpUtil = new SftpUtil(constants.SFTP_USER_ID, constants.SFTP_PASSWORD, constants.SFTP_HOST, 22);
		sftpUtil.login();
		String fileName = "PaymentRunReport_" + LocalDateTimeUtil.format(LocalDate.now().minusDays(1), "ddMMyyyy") + ".csv";
		InputStream inputStream = sftpUtil.downloadStream("pmtrepsap2feishu", fileName);
		if (inputStream == null) {
			log.info("无昨日支付同步数据：{}", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			return;
		}
		// Excel列表数据查询
		ArrayList<ExcelPaypool> excelPaypools = new ArrayList<>();
		try {
			ArrayList<ExcelPaypool> finalExcelPaypools = excelPaypools;
			EasyExcel.read(inputStream, ExcelPaypool.class, new ReadListener<ExcelPaypool>() {
				@Override
				public void invoke(ExcelPaypool paypool, AnalysisContext context) {
					finalExcelPaypools.add(paypool);
				}
				@Override
				public void doAfterAllAnalysed(AnalysisContext context) {
				}
			}).excelType(ExcelTypeEnum.CSV).sheet().headRowNumber(1).doRead();
		} catch (Exception e) {
			e.printStackTrace();
		}

		excelPaypools = excelPaypools.stream().collect(
				Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(
						Comparator.comparing(ExcelPaypool::getDocumentReferenceId))),
						ArrayList::new));
		log.info("需要同步的支付数: {}", excelPaypools.size());

		// 通过单据查看支付池id
		List<FeishuPaypool> payPoolList = new ArrayList<>();

		for (ExcelPaypool excelPaypool : excelPaypools) {
			List<FeishuPaypool> paypools = SignUtil.getPaypools(excelPaypool.getDocumentReferenceId());
			for (FeishuPaypool paypool : paypools) {
				// A1待支付 A3支付中 A5支付成功
				if ("A1".equals(paypool.getPaymentStatusCode()) || "A3".equals(paypool.getPaymentStatusCode())) {
					payPoolList.add(new FeishuPaypool().setId(paypool.getId()).setAccountant(paypool.getAccountant()));
				}
			}
		}
		payPoolList = payPoolList.stream().collect(
				Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(
				Comparator.comparing(FeishuPaypool::getId))),ArrayList::new));
		log.info("在支付中的支付池: {}", payPoolList.size());
		List<Boolean> resultList = new ArrayList<>();
		for (FeishuPaypool paypool : payPoolList) {
			resultList.add(SignUtil.updatePaypool(paypool.getId(), paypool.getAccountant()));
		}

		List<Boolean> resultFilterList = resultList.stream().filter(r -> r).collect(Collectors.toList());
		log.info("修改成功的单据数: {}", resultFilterList.size());
		if (resultFilterList.size() < payPoolList.size()) {
			SignUtil.sendMsg(constants.CHAT_ID_ARG, constants.USER_ID_ARG, "更新支付池支付状态部分失败，请查看日志，需要修改支付状态的单据数：" + payPoolList.size() +
					"，修改成功的单据数：" + resultFilterList.size());
		}

		SftpUtil sftpUtilNew = new SftpUtil(constants.SFTP_USER_ID, constants.SFTP_PASSWORD, constants.SFTP_HOST, 22);
		sftpUtilNew.login();
		sftpUtilNew.moveFile("pmtrepsap2feishu", fileName);

		log.info("支付池支付状态更新结束。");
	}
}
