package com.lundong.ascentialsync;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.fastjson.JSONObject;
import com.lundong.ascentialsync.config.Constants;
import com.lundong.ascentialsync.entity.*;
import com.lundong.ascentialsync.entity.spend.SpendCustomField;
import com.lundong.ascentialsync.util.ExcelUtil;
import com.lundong.ascentialsync.util.SftpUtil;
import com.lundong.ascentialsync.util.SignUtil;
import com.lundong.ascentialsync.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest
class AscentialSyncApplicationTests {

	@Autowired
	private Constants constants;

	@Test
	void testGetCustomAttrs() {
		List<CustomAttr> customAttrs = SignUtil.getCustomAttrs();
		for (CustomAttr customAttr : customAttrs) {
			System.out.println(customAttr);
		}
	}

	@Test
	void testGetFeishuUser() {
		FeishuUser user = SignUtil.getFeishuUser("16gccb71");
		System.out.println(user);
	}

	@Test
	void testGetFeishuEmployees() {
		List<FeishuUser> users = SignUtil.getFeishuEmployees();
		for (FeishuUser user : users) {
			System.out.println(user);
		}
		System.out.println(users.size());
	}

	@Test
	void testGetFeishuBaseEmployees() {
		List<FeishuUser> users = SignUtil.getFeishuBaseEmployees();
//		for (FeishuUser user : users) {
//			System.out.println(user);
//		}
		System.out.println(users.size());
	}

	@Test
	void testSpecifiedCellWrite() throws IOException {
		ExcelHeader header = ExcelHeader.builder().build();
		header.setDocumentHeaderText("h1");
		header.setDocumentDate("h2");
		header.setCompanyCode("h3");
		header.setJournalEntry("h4");
		List<ExcelRecord> excelRecords = new ArrayList<>();
		ExcelRecord record01 = ExcelRecord.builder().build();
		record01.setCompanyCode("lkiuw");
		record01.setJournalEntry("vcdsf");
		record01.setCostCenter("vx");
		record01.setGlAccount("bm.lxkn");
		excelRecords.add(record01);
		ExcelRecord record02 = ExcelRecord.builder().build();
		record02.setCompanyCode("lkiuw2");
		record02.setJournalEntry("vcdsf2");
		record02.setCostCenter("vx2");
		record02.setGlAccount("bm.lxkn2");
		excelRecords.add(record02);
		ExcelUtil.generateCsv(header, excelRecords, Files.newOutputStream(Paths.get("c:\\out.csv")));
	}

	@Test
	void testSpendVouchers() {
		List<FeishuSpendVoucher> feishuSpendVouchers = SignUtil.spendVouchers("TR23060100002");
		for (FeishuSpendVoucher feishuSpendVoucher : feishuSpendVouchers) {
			System.out.println("feishuSpendVoucher: " + feishuSpendVoucher);
		}
	}

	@Test
	void testTimestampToDateFormat() {
		String s = TimeUtil.timestampToDateFormat("1685588983070");
		System.out.println(s);
	}

	@Test
	void testTimestampToDate() {
		Date date = TimeUtil.timestampToDate("1685516215000");
		System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date));
	}

	@Test
	void moveFile() {
		SftpUtil sftpUtil = new SftpUtil(constants.SFTP_USER_ID, constants.SFTP_PASSWORD, constants.SFTP_HOST, 22);
		sftpUtil.login();
//		sftpUtil.moveFile("workday2feishu", "WorkdayFeishu_10072023.csv");
	}

	@Test
	void getPaypools() {
		List<FeishuPaypool> paypools = SignUtil.getPaypools("ER23100100001");
		System.out.println("size: " + paypools.size());
		for (FeishuPaypool paypool : paypools) {
			System.out.println(paypool);
		}
	}

	@Test
	void testUpdatePaypool() {
		SignUtil.updatePaypool("7231075633424744765", "26c7gf5g");
	}

	@Test
	void getAppId() {
		System.out.println();
	}

	@Test
	void getSpendFormsWithFormCodeList() {
		List<FeishuPaypool> paypools = new ArrayList<>();
		paypools.add(new FeishuPaypool().setVendorFormHeaderCode("ER23062700001"));
		paypools.add(new FeishuPaypool().setVendorFormHeaderCode("ER23062700001"));
		List<FeishuSpendVoucher> feishuSpendVouchers = SignUtil.spendFormsWithFormCodeList(paypools);
		for (FeishuSpendVoucher feishuSpendVoucher : feishuSpendVouchers) {
			System.out.println(feishuSpendVoucher);
		}
	}

	@Test
	void testReTry() {
		// 重试
		String resultStr = "";
		for (int i = 0; i < 3; i++) {
			resultStr = HttpRequest.get("https://baidu.com/")
					.form("")
					.body("")
					.execute()
					.body();
			if (resultStr.contains("502 Bad Gateway")) {
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				log.info("resultStr: {}", "504 Gateway Time-out, 重试" + (i + 1) + "次");
			} else {
				break;
			}
		}
		System.out.println(resultStr);
		// 重试完检测
		if (resultStr.contains("502 Bad Gateway")) {
			log.info("重试3次后失败: {}", "userId: " + "123");
		}
	}

	@Test
	void testReTry02() {
		// 重试
		String resultStr = "";
		JSONObject jsonObject = null;
		for (int i = 0; i < 3; i++) {
			resultStr = HttpRequest.get("http://127.0.0.1:8088/feishu/webhook/event1")
					.execute()
					.body();
			try {
				jsonObject = JSONObject.parseObject(resultStr);
			} catch (Exception e) {
				log.error("json解析失败, 重试 {} 次, message: {}, body: {}", i + 1,e.getMessage() , resultStr);
			}
			if (jsonObject != null) {
				break;
			}
		}
		// 重试完检测
		if (jsonObject == null) {
			log.info("重试3次后失败");

		}
		if ("null".equals(jsonObject.getString("msg"))) {
			System.out.println("success");
		} else {
			log.info("resultStr: {}", resultStr);
		}
	}

	@Test
	void testGetAccessToken() {
		// 重试
		String accessToken = SignUtil.getAccessToken("cli_a46e3ddc147b900e", "324kibvjVE7W8zKwSVRfcfSlbeI5W8qa");
		System.out.println("accessToken: " + accessToken);
	}

	@Test
	void testPayPoolList() throws IOException {
//		SftpUtil sftpUtil = new SftpUtil(constants.SFTP_USER_ID, constants.SFTP_PASSWORD, constants.SFTP_HOST, 22);
//		sftpUtil.login();
		String fileName = "PaymentRunReport_" + LocalDateTimeUtil.format(LocalDate.now().minusDays(1), "ddMMyyyy") + ".csv";
		InputStream inputStream = Files.newInputStream(new File("/Users/rawchen/Downloads/PaymentRunReport_14102023.csv").toPath());
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
//		System.out.println(excelPaypools.size());
		excelPaypools = excelPaypools.stream().collect(
				Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(
								Comparator.comparing(ExcelPaypool::getDocumentReferenceId))),
						ArrayList::new));
		log.info("提取单据的支付数: {}", excelPaypools.size());

		// 通过单据查看支付池id
		List<FeishuPaypool> payPoolList = new ArrayList<>();

		for (ExcelPaypool excelPaypool : excelPaypools) {
			List<FeishuPaypool> paypools = SignUtil.getPaypools(excelPaypool.getDocumentReferenceId());
			for (FeishuPaypool paypool : paypools) {
				// A1待支付 A3支付中 A5支付成功
				if ("A1".equals(paypool.getPaymentStatusCode())) {
					payPoolList.add(new FeishuPaypool().setId(paypool.getId()).setAccountant(paypool.getAccountant()));
				}
			}
		}
		payPoolList = payPoolList.stream().collect(
				Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(
						Comparator.comparing(FeishuPaypool::getId))),ArrayList::new));
		log.info("在待支付的支付池: {}", payPoolList.size());
	}

	@Test
	 void testGetSpendCustomFields() {
		List<SpendCustomField> spendCustomFields = SignUtil.getSpendCustomFields(SignUtil.getAccessToken(constants.APP_ID_FEISHU, constants.APP_SECRET_FEISHU), "0000002");
		for (SpendCustomField spendCustomField : spendCustomFields) {
			System.out.println(spendCustomField);
		}
	}

}
