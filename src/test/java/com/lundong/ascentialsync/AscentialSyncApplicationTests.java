package com.lundong.ascentialsync;

import com.lundong.ascentialsync.config.Constants;
import com.lundong.ascentialsync.entity.*;
import com.lundong.ascentialsync.util.ExcelUtil;
import com.lundong.ascentialsync.util.SftpUtil;
import com.lundong.ascentialsync.util.SignUtil;
import com.lundong.ascentialsync.util.TimeUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SpringBootTest
class AscentialSyncApplicationTests {

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
		SftpUtil sftpUtil = new SftpUtil(Constants.SFTP_USER_ID, Constants.SFTP_PASSWORD, Constants.SFTP_HOST, 22);
		sftpUtil.login();
		sftpUtil.moveFile("workday2feishu", "WorkdayFeishu_06062023.csv");
	}

}
