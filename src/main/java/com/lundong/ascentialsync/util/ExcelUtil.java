package com.lundong.ascentialsync.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.lundong.ascentialsync.entity.ExcelHeader;
import com.lundong.ascentialsync.entity.ExcelRecord;

import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author RawChen
 * @date 2023-05-30 17:51
 */
public class ExcelUtil {
	public static void generateCsv(ExcelHeader header, List<ExcelRecord> excelRecords, OutputStream outputStream) {
		try {
			// 使用EasyExcel创建CSV文件
			ExcelWriter excelWriter = EasyExcel.write(outputStream).excelType(ExcelTypeEnum.CSV).charset(Charset.defaultCharset()).build();
			WriteSheet writeSheet = EasyExcel.writerSheet().build();
			List<ExcelHeader> headers = new ArrayList<>();
			headers.add(ExcelHeader.builder()
					.journalEntry("JournalEntry")
					.companyCode("CompanyCode")
					.postingDate("PostingDate")
					.documentDate("DocumentDate")
					.accountingDocumentType("AccountingDocumentType")
					.documentHeaderText("DocumentHeaderText")
					.documentReferenceID("DocumentReferenceID")
					.transactionCurrency("TransactionCurrency")
					.build());
			headers.add(header);
			excelWriter.write(headers, writeSheet);
			// 填充明细表头和数据
			excelRecords.add(0, ExcelRecord.builder()
					.journalEntry("JournalEntry")
					.companyCode("CompanyCode")
					.glAccount("GLAccount")
					.creditor("Creditor")
					.amountInTransactionCurrency("AmountInTransactionCurrency")
					.documentItemText("DocumentItemText")
					.debitCreditCode("DebitCreditCode")
					.costCenter("CostCenter")
					.wbsElement("WBSElement")
					.assignmentReference("AssignmentReference")
					.taxCode("TaxCode")
					.taxAmount("TaxAmount")
					.deliveryCentre("DeliveryCentre")
					.build());
			excelWriter.write(excelRecords, writeSheet);
			// 关闭文件流和ExcelWriter
			excelWriter.finish();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
