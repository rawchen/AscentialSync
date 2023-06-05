package com.lundong.ascentialsync.service.impl;

import com.lundong.ascentialsync.entity.*;
import com.lundong.ascentialsync.entity.spend.Allocation;
import com.lundong.ascentialsync.entity.spend.InvoiceDetail;
import com.lundong.ascentialsync.entity.spend.ReimburseData;
import com.lundong.ascentialsync.entity.spend.SpendCustomField;
import com.lundong.ascentialsync.service.SpendService;
import com.lundong.ascentialsync.util.ExcelUtil;
import com.lundong.ascentialsync.util.SignUtil;
import com.lundong.ascentialsync.util.StringUtil;
import com.lundong.ascentialsync.util.TimeUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author RawChen
 * @date 2023-05-12 14:24
 */
@Service
public class SpendServiceImpl implements SpendService {
	/**
	 * 每天定时同步昨天新增的差旅报销单和日常费用报销单，生成固定的csv文件到sftp的expfeishu2sap文件夹
	 */
	@Override
	@Scheduled(cron = "0 0 1 ? * *")
	public void syncFormData() {

		Date date = new Date();
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		calendar.add(Calendar.DATE, -3);
		date = calendar.getTime();

		List<SpendCustomField> glAccountList = SignUtil.getSpendCustomFields("0000001");
		List<SpendCustomField> expenseTypeList = SignUtil.getSpendCustomFields("0000002");
		List<SpendCustomField> taxCodeList = SignUtil.getSpendCustomFields("0000003");

		// 费控列表接口，先获取昨天添加到费控的表单列表
		List<FeishuSpendForm> feishuSpendForms = SignUtil.spendForms(date);
		System.out.println("表单数量: " + feishuSpendForms.size());
		// 过滤掉
		feishuSpendForms = feishuSpendForms.stream()
				.filter(f -> f.getBizClsName().contains("差旅报销") || f.getBizClsName().contains("日常费用报销"))
				.collect(Collectors.toList());
		System.out.println("过滤后表单数量: " + feishuSpendForms.size());
		for (FeishuSpendForm form : feishuSpendForms) {
			String employeeNo = "";
			// 通过applicant_id(申请人ID)获取员工ID
			FeishuUser feishuUser = SignUtil.getFeishuUser(form.getApplicantId());
			if (feishuUser != null && feishuUser.getEmployeeNo() != null) {
				employeeNo = feishuUser.getEmployeeNo();
			}
			// 遍历通过单据code查询详表
			List<FeishuSpendVoucher> feishuSpendVouchers = SignUtil.spendVouchers(form.getFormCode());
			// 根据单据code如果能查出来至少一条数据
			if (feishuSpendVouchers.size() > 0) {
				// 获取最新version的单据详表
				FeishuSpendVoucher feishuSpendVoucher = feishuSpendVouchers.get(feishuSpendVouchers.size() - 1);
				// 转换发起日期格式
				Date submitTime;
				Date approvedTime = new Date();

				// 获取最新version的reimburse_data
				ReimburseData reimburseData = null;
				List<ReimburseData> reimburseDataList = feishuSpendVoucher.getReimburseData();
				if (reimburseDataList != null && reimburseDataList.size() > 0) {
					reimburseData = reimburseDataList.get(reimburseDataList.size() - 1);
				}
				try {
					submitTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(form.getSubmitTime());
					if (reimburseData != null) {
						String approvedTimeString = reimburseData.getApprovedTime();
						if (approvedTimeString != null) {
							if (!approvedTimeString.contains(":") && approvedTimeString.length() == 13) {
								// 时间为时间戳
								approvedTimeString = TimeUtil.timestampToDate(approvedTimeString);
							}
							approvedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(approvedTimeString);
						}
					}
				} catch (ParseException e) {
					throw new RuntimeException(e);
				}
				// 通过获取的单据生成表头
				ExcelHeader header = ExcelHeader.builder()
						.journalEntry("1")
						.companyCode("")
						.postingDate(new SimpleDateFormat("dd.MM.yyyy").format(approvedTime))
						.documentDate(new SimpleDateFormat("dd.MM.yyyy").format(submitTime))
						.accountingDocumentType("EF")
						.documentHeaderText(employeeNo + "feishu")
						.documentReferenceID(form.getFormCode())
						.transactionCurrency(form.getCurrencyCode())
						.build();

				// 表头companyCode
				if (feishuUser != null && feishuUser.getCompanyCode() != null) {
					header.setCompanyCode(feishuUser.getCompanyCode());
				} else {
					header.setCompanyCode("");
				}

				// 通过获取的单据生成对应的明细
				List<ExcelRecord> excelRecords = new ArrayList<>();
				if (reimburseData != null) {
					// 发票报销行列表
					List<InvoiceDetail> invoiceDetailList = reimburseData.getInvoiceDetailList();
					for (InvoiceDetail invoiceDetail : invoiceDetailList) {
						String invoiceDetailBizTypeCode = "";
						ExcelRecord record = ExcelRecord.builder()
								.journalEntry("1")
								.companyCode(StringUtil.nullIsEmpty(reimburseData.getCompanyCode()))
								.glAccount("") //费用类型对应
								.creditor(employeeNo)
								.amountInTransactionCurrency(invoiceDetail.getGrossAmount())	// 区分含税和不含税
								.documentItemText("")
								.debitCreditCode("")	// 借记代码
								.wbsElement(StringUtil.nullIsEmpty(invoiceDetail.getProjectCode()))
								.assignmentReference("")
								.taxCode("") // 税码
								.deliveryCentre("")
								.build();
						if (feishuUser != null && feishuUser.getCostCenterCode() != null) {
							record.setCostCenter(feishuUser.getCostCenterCode());
						} else {
							record.setCostCenter("");
						}
						if (feishuUser != null && feishuUser.getCompanyCode() != null) {
							record.setCompanyCode(feishuUser.getCompanyCode());
						} else {
							record.setCompanyCode("");
						}

						// 借贷两条数据
						ExcelRecord recordNew = ExcelRecord.builder().build();
						BeanUtils.copyProperties(record, recordNew);
						record.setGlAccount("");
						recordNew.setCreditor("");
						record.setDocumentItemText("");
						record.setDebitCreditCode("H");
						recordNew.setDebitCreditCode("S");
						record.setCostCenter("");
						record.setWbsElement("");
						record.setAssignmentReference("");
						record.setTaxCode("");
						record.setTaxAmount("");
						record.setDeliveryCentre("");
						recordNew.setAmountInTransactionCurrency(invoiceDetail.getExcludeTaxAmount());
						recordNew.setTaxAmount(invoiceDetail.getInvoiceTaxAmount());

						// 获取invoice_detail_list同级的allocations，匹配reimburse_line_id
						// 从同级分摊列表找到第一个费用类型biz_type_code转换
						String reimburseLineId = invoiceDetail.getReimburseLineId();
						if (reimburseLineId != null) {
							List<Allocation> allocations = reimburseData.getAllocations();
							if (allocations != null) {
								for (Allocation allocation : allocations) {
									if (reimburseLineId.equals(allocation.getReimburseLineId())) {
										invoiceDetailBizTypeCode = allocation.getBizTypeCode();
										// 获取分摊行的行事由
										if (invoiceDetailBizTypeCode != null) {
											for (SpendCustomField customField : expenseTypeList) {
												if (customField.getCode().equals(invoiceDetailBizTypeCode)) {
													recordNew.setDocumentItemText(customField.getNameI18n());
												}
											}
										}

//										String lineDesc = allocation.getLineDesc();
//										if (lineDesc != null) {
											// 格式：1405258-廉紫-SAP F2020项目，GB0350WW59\n与同行人Jodie分摊
											// 截取第二个横杠开始到最后
//											lineDesc = lineDesc.substring(lineDesc.indexOf("-", lineDesc.indexOf("-") + 1) + 1);
//											lineDesc = lineDesc.replaceAll("\n", " ");
//											recordNew.setDocumentItemText(lineDesc);
//										}
										// 获取分摊行费用类型biz_type_code
										String bizTypeCode = allocation.getBizTypeCode();
										if (bizTypeCode != null) {
											for (SpendCustomField customField : glAccountList) {
												if (bizTypeCode.equals(customField.getCode())) {
													recordNew.setGlAccount(customField.getNameI18n());
													break;
												}
											}
										}
										break;
									}
								}
							}
						}

						// TODO recordNew.taxCode
						// 读取发票信息，判断是否可抵扣
						Boolean deductible = invoiceDetail.getDeductible();
						if (deductible != null && deductible) { // 可抵扣
							// 提取税率，提取对应SAP
							String invoiceTaxRate = invoiceDetail.getInvoiceTaxRate();
							if (invoiceTaxRate != null) {
								// 转换税率为百分之 6.0000000000 -> 6%
								invoiceTaxRate = StringUtil.taxRateFormat(invoiceTaxRate);

								for (SpendCustomField taxCode : taxCodeList) {
									if (taxCode.getNameI18n().startsWith(invoiceTaxRate + " input tax")) {
										// 匹配值假如以“6% input tax”开头
										recordNew.setTaxCode(taxCode.getCode());
										break;
									}
								}
							}
						} else {
							// 不可抵扣
							// 获取对应报销行的的费用类型
							if (invoiceDetailBizTypeCode != null) {
//								String expenseTypeString = "";
//								// 如果发票详情对应的分摊行费用类型不为空，匹配自定义维度
//								for (SpendCustomField customField : expenseTypeList) {
//									if (invoiceDetailBizTypeCode.equals(customField.getCode())) {
////										invoiceDetail.set
//										expenseTypeString = customField.getNameI18n();
//										break;
//									}
//								}
								// 判断是否为可抵扣的交通类型发票
								// 费用类型为
								if ("1002".equals(invoiceDetailBizTypeCode) || "1003".equals(invoiceDetailBizTypeCode)) {
									// 飞机， 火车/高铁
									for (SpendCustomField taxCode : taxCodeList) {
										if (taxCode.getNameI18n().startsWith("9% input deduction")) {
											recordNew.setTaxCode(taxCode.getCode());
											break;
										}
									}
								} else if ("1004".equals(invoiceDetailBizTypeCode) || "4001".equals(invoiceDetailBizTypeCode)) {
									// 差旅-地铁/公交/打车， 市内交通费
									for (SpendCustomField taxCode : taxCodeList) {
										if (taxCode.getNameI18n().startsWith("3% input deduction")) {
											recordNew.setTaxCode(taxCode.getCode());
											break;
										}
									}
								} else {
									// 其它费用类型
									// 提取税率，提取对应SAP
									String invoiceTaxRate = invoiceDetail.getInvoiceTaxRate();
									if (invoiceTaxRate != null) {
										invoiceTaxRate = StringUtil.taxRateFormat(invoiceTaxRate);
										for (SpendCustomField taxCode : taxCodeList) {
											if (taxCode.getNameI18n().startsWith(invoiceTaxRate + " input tax")) {
												// 匹配值假如以“6% input tax”开头
												recordNew.setTaxCode(taxCode.getCode());
												break;
											}
										}
									}
								}
							}
						}

						System.out.println(record);
						System.out.println(recordNew);
						excelRecords.add(record);
						excelRecords.add(recordNew);
					}
				}

				// 生成上传CSV到SFTP
				try {
					String fileName = employeeNo + "-" + TimeUtil.getTimestamp() + "-" +"TUTnew.csv";
//					SftpUtil sftpUtil = new SftpUtil(Constants.SFTP_USER_ID, Constants.SFTP_PASSWORD, Constants.SFTP_HOST, 22);
//					sftpUtil.login();
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
//					baos.write(0xef);
//					baos.write(0xbb);
//					baos.write(0xbf);
					ExcelUtil.generateCsv(header, excelRecords, baos);
					FileOutputStream fos = new FileOutputStream("C:\\" + fileName);
					//追加BOM标识
					fos.write(0xef);
					fos.write(0xbb);
					fos.write(0xbf);
					fos.write(baos.toByteArray());
					fos.close();

//					sftpUtil.upload("expfeishu2sap", fileName, baos.toByteArray());
					System.out.println("生成CSV文件：" + fileName);
					baos.close();
				} catch (Exception e) {

					throw new RuntimeException(e);
				}
			}
		}
		System.out.println("生成文件结束");
	}

	/**
	 * 每天定时同步当天新增的新数据
	 */
	@Override
	@Scheduled(cron = "0 0 1 ? * *")
	public void syncSpendVouchers() {

	}
}
