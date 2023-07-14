package com.lundong.ascentialsync;

import cn.hutool.http.HttpRequest;
import com.lundong.ascentialsync.config.Constants;
import com.lundong.ascentialsync.entity.*;
import com.lundong.ascentialsync.util.ExcelUtil;
import com.lundong.ascentialsync.util.SftpUtil;
import com.lundong.ascentialsync.util.SignUtil;
import com.lundong.ascentialsync.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
		sftpUtil.moveFile("workday2feishu", "WorkdayFeishu_10072023.csv");
	}

	@Test
	void getPaypools() {
		List<FeishuPaypool> paypools = SignUtil.getPaypools(null);
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
			resultStr = HttpRequest.get("https://rawchen.com/")

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

}
