package com.lundong.ascentialsync.service.impl;

import com.lundong.ascentialsync.entity.*;
import com.lundong.ascentialsync.entity.spend.Allocation;
import com.lundong.ascentialsync.entity.spend.InvoiceDetail;
import com.lundong.ascentialsync.entity.spend.ReimburseData;
import com.lundong.ascentialsync.entity.spend.SpendCustomField;
import com.lundong.ascentialsync.enums.InvoiceTypeEnum;
import com.lundong.ascentialsync.service.SpendService;
import com.lundong.ascentialsync.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 同步费控数据
 *
 * @author RawChen
 * @date 2023-05-12 14:24
 */
@Service
@Slf4j
public class SpendServiceImpl implements SpendService {
	/**
	 * 每天定时同步昨天新增的差旅报销单和日常费用报销单，生成固定的csv文件到sftp的expfeishu2sap文件夹
	 */
	@Override
	@Scheduled(cron = "0 0 1 ? * *")
	public void syncSpendData() {

		Date date = new Date();
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		calendar.add(Calendar.DATE, -1);
		date = calendar.getTime();

		List<SpendCustomField> glAccountList = SignUtil.getSpendCustomFields("0000001");
		List<SpendCustomField> expenseTypeList = SignUtil.getSpendCustomFields("0000002");
		List<SpendCustomField> taxCodeList = SignUtil.getSpendCustomFields("0000003");

		// 费控列表接口，先获取昨天添加到费控的表单列表
//		List<FeishuSpendForm> feishuSpendForms = SignUtil.spendForms(date);
		List<FeishuSpendVoucher> feishuSpendVoucherList = SignUtil.spendFormsWithTimestamp(date);
		log.info("昨天总单据数量（包含不同version）: {}", feishuSpendVoucherList.size());

		// 过滤出不是支付完成的
		feishuSpendVoucherList = feishuSpendVoucherList.stream()
				.filter(f -> f.getReimburseData().size() > 0).collect(Collectors.toList());

		List<ReimburseData> reimburseDataList = new ArrayList<>();

		System.out.println("feishuSpendVoucherList size:" + feishuSpendVoucherList.size());

		// 过滤出属于员工差旅报销和员工日常报销的
		for (FeishuSpendVoucher feishuSpendVoucher : feishuSpendVoucherList) {
			for (ReimburseData reimburse : feishuSpendVoucher.getReimburseData()) {
				if ("TRAVEL_REIMBURSEMENT".equals(reimburse.getBizUnitCode()) || "EXPENSE_REIMBURSEMENT".equals(reimburse.getBizUnitCode())) {
					reimburseDataList.add(reimburse);
				}
			}
		}

		System.out.println("reimburseDataList size: " + reimburseDataList.size());

		// 过滤出最新的，也就是根据version过滤掉重复formCode
		List<ReimburseData> reimburseDataListNew =  DataFilterUtil.filterByVersion(reimburseDataList);
		System.out.println("reimburseDataListNew size: " + reimburseDataListNew.size());

		// 过滤出单据状态为已完成的
		reimburseDataListNew = reimburseDataListNew.stream().filter(r -> "completed".equals(r.getProcessStatus())).collect(Collectors.toList());

		log.info("过滤后表单数量: {}", reimburseDataListNew.size());
//		feishuSpendForms = feishuSpendForms.stream().filter(f -> f.getFormCode().equals("TR23060600004")).collect(Collectors.toList());
//		log.info("过滤后表单数量: {}", feishuSpendForms.size());
		for (ReimburseData form : reimburseDataListNew) {
			String employeeNo = "";
			// 通过applicant_id(申请人ID)获取员工ID
			FeishuUser feishuUser = SignUtil.getFeishuUser(form.getApplicantUid());
			if (feishuUser != null && feishuUser.getEmployeeNo() != null) {
				employeeNo = feishuUser.getEmployeeNo();
			}
			// 遍历通过单据code查询详表
			List<FeishuSpendVoucher> feishuSpendVouchers = SignUtil.spendVouchers(form.getFormCode());
			// 根据单据code如果能查出来至少一条数据
			if (feishuSpendVouchers.size() > 0) {

				// 去除reimburseData列表size为0的
				feishuSpendVouchers = feishuSpendVouchers.stream().filter(v -> v.getReimburseData().size() > 0).collect(Collectors.toList());

				// 获取最新version的单据详表
				FeishuSpendVoucher feishuSpendVoucher = feishuSpendVouchers.get(feishuSpendVouchers.size() - 1);
				// 转换发起日期格式
				Date submitTime;
				Date approvedTime = new Date();
				// 获取最新version的reimburse_data
				ReimburseData reimburseData = null;
				List<ReimburseData> reimburseDatas = feishuSpendVoucher.getReimburseData();
				if (reimburseDatas != null && reimburseDatas.size() > 0) {
					reimburseData = reimburseDatas.get(reimburseDatas.size() - 1);
				}
				try {
					submitTime = TimeUtil.timestampToDate(form.getApplicantTime());
					if (reimburseData != null) {
						String approvedTimeString = reimburseData.getApprovedTime();
						if (approvedTimeString != null) {
							if (!approvedTimeString.contains(":") && approvedTimeString.length() == 13) {
								// 时间为时间戳
								approvedTimeString = TimeUtil.timestampToDateFormat(approvedTimeString);
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
								.companyCode("")
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

						// 税码
						// 先判断是否可抵扣
						Boolean deductible = invoiceDetail.getDeductible();
						if (deductible != null && deductible) { // 可抵扣
							// 提取税率，提取对应SAP
							String invoiceTaxRate = invoiceDetail.getInvoiceTaxRate();
							if (invoiceTaxRate != null) {
								// 转换税率为百分之 6.0000000000 -> 6%
								invoiceTaxRate = StringUtil.taxRateFormat(invoiceTaxRate);

								for (SpendCustomField taxCode : taxCodeList) {
									if (taxCode.getNameI18n().toLowerCase().startsWith(invoiceTaxRate + " input vat")) {
										// 匹配值假如以“6% input tax”开头
										recordNew.setTaxCode(taxCode.getCode());
										break;
									}
								}
								// 接着判断是差旅还是日常报销
								if (form.getBizUnitCode().contains("EXPENSE_REIMBURSEMENT")) {
									// 如果费用类型为2005员工福利则税码J0
									if ("2005".equals(invoiceDetailBizTypeCode)) {
										recordNew.setTaxCode("J0");
										recordNew.setTaxAmount("0");
										recordNew.setAmountInTransactionCurrency(invoiceDetail.getGrossAmount());
									}
								}
							}
						} else { // 不可抵扣
							InvoiceTypeEnum invoiceTypeEnum = InvoiceTypeEnum.getType(invoiceDetail.getInvoiceTypeCode());
							if (invoiceTypeEnum != null) {
								OUT:
								switch (invoiceTypeEnum) { // 根据发票类型判断
									case VAT_GENERAL_ELECTRIC:
										if (invoiceDetailBizTypeCode != null) {
											if ("1004".equals(invoiceDetailBizTypeCode) || "4001".equals(invoiceDetailBizTypeCode)) {
												// 1004差旅-地铁/公交/打车 4001市内交通费
												// 提取税率，税额
												String invoiceTaxRate = invoiceDetail.getInvoiceTaxRate();
												if (invoiceTaxRate != null) {
													invoiceTaxRate = StringUtil.taxRateFormat(invoiceTaxRate);
													for (SpendCustomField taxCode : taxCodeList) {
														if (taxCode.getNameI18n().toLowerCase().startsWith(invoiceTaxRate + " input vat")) {
															recordNew.setTaxCode(taxCode.getCode());
															break OUT;
														}
													}
												}
											} else {
												// 否
												recordNew.setTaxCode("J0");
												// 修改税金为0和修改不含税金额为总金额
												recordNew.setTaxAmount("0");
												recordNew.setAmountInTransactionCurrency(invoiceDetail.getGrossAmount());
											}
										}
										break;
									case VAT:
									case VAT_ELECTRIC:
									case VAT_GENERAL_TOLL_FEE:
										String invoiceTaxRate = invoiceDetail.getInvoiceTaxRate();
										if (invoiceTaxRate != null) {
											invoiceTaxRate = StringUtil.taxRateFormat(invoiceTaxRate);
											for (SpendCustomField taxCode : taxCodeList) {
												if (taxCode.getNameI18n().toLowerCase().startsWith(invoiceTaxRate + " input vat")) {
													recordNew.setTaxCode(taxCode.getCode());
													break OUT;
												}
											}
										}
										break;
									case PASSENGER_INVOICE:
									case HIGHWAY_TOLL_INVOICE:
										// 客运发票
										for (SpendCustomField taxCode : taxCodeList) {
											if (taxCode.getNameI18n().toLowerCase().startsWith("3% input vat")) {
												recordNew.setTaxCode(taxCode.getCode());
												// 重新计算税额
												// 不含税金额 = 全额 / (1+税率)
												double grossAmount = Double.parseDouble(invoiceDetail.getGrossAmount());
												BigDecimal calcResult = new BigDecimal(grossAmount);
												calcResult = calcResult.divide(new BigDecimal("1.03"), 2, RoundingMode.HALF_UP);
												recordNew.setAmountInTransactionCurrency(String.valueOf(calcResult));
												// 税额 = 全额-不含税金额
												recordNew.setTaxAmount(String.valueOf(new BigDecimal(grossAmount).subtract(calcResult)));
												break OUT;
											}
										}
										break;
									case AIR_TICKET_ITINERARY:
									case TRAIN_TICKET:
										// 航空机票行程单/火车票
										for (SpendCustomField taxCode : taxCodeList) {
											if (taxCode.getNameI18n().toLowerCase().startsWith("9% input vat")) {
												recordNew.setTaxCode(taxCode.getCode());
												// 重新计算税额
												// 不含税金额 = 全额 / (1+税率)
												double grossAmount = Double.parseDouble(invoiceDetail.getGrossAmount());
												BigDecimal calcResult = new BigDecimal(grossAmount);
												calcResult = calcResult.divide(new BigDecimal("1.09"), 2, RoundingMode.HALF_UP);
												recordNew.setAmountInTransactionCurrency(String.valueOf(calcResult));
												// 税额 = 全额-不含税金额
												recordNew.setTaxAmount(String.valueOf(new BigDecimal(grossAmount).subtract(calcResult)));
												break OUT;
											}
										}
										break;
									default:
										for (SpendCustomField taxCode : taxCodeList) {
											if (taxCode.getNameI18n().toLowerCase().startsWith("0% vat")) {
												recordNew.setTaxCode(taxCode.getCode());
												// 修改税金为0和修改不含税金额为总金额
												recordNew.setTaxAmount("0");
												recordNew.setAmountInTransactionCurrency(invoiceDetail.getGrossAmount());
												break;
											}
										}
								}

								// 继续根据税率大于0判断日常报销2005员工福利的税码
								if (Double.parseDouble(recordNew.getTaxAmount()) > 0) {
									if (form.getBizUnitCode().contains("EXPENSE_REIMBURSEMENT")) {
										if ("2005".equals(invoiceDetailBizTypeCode)) {
											recordNew.setTaxCode("J0");
											recordNew.setTaxAmount("0");
											recordNew.setAmountInTransactionCurrency(invoiceDetail.getGrossAmount());
										}
									}
								}
							}
						}

						// 员工日常报销
//						if (form.getBizUnitCode().contains("EXPENSE_REIMBURSEMENT")) {
//							recordNew.setTaxCode("J0");
//							// 修改税金为0和修改不含税金额为总金额
//							recordNew.setTaxAmount("0");
//							recordNew.setAmountInTransactionCurrency(invoiceDetail.getGrossAmount());
//						} else {
							// 差旅报销
							// 根据发票类型判断



//							Boolean deductible = invoiceDetail.getDeductible();
//							if (deductible != null && deductible) { // 可抵扣
//								// 提取税率，提取对应SAP
//								String invoiceTaxRate = invoiceDetail.getInvoiceTaxRate();
//								if (invoiceTaxRate != null) {
//									// 转换税率为百分之 6.0000000000 -> 6%
//									invoiceTaxRate = StringUtil.taxRateFormat(invoiceTaxRate);
//
//									for (SpendCustomField taxCode : taxCodeList) {
//										if (taxCode.getNameI18n().toLowerCase().startsWith(invoiceTaxRate + " input vat")) {
//											// 匹配值假如以“6% input tax”开头
//											recordNew.setTaxCode(taxCode.getCode());
//											break;
//										}
//									}
//								}
//							} else {}


								// 获取对应报销行的的费用类型
//							if (invoiceDetailBizTypeCode != null) {
//								String expenseTypeString = "";
//								// 如果发票详情对应的分摊行费用类型不为空，匹配自定义维度
//								for (SpendCustomField customField : expenseTypeList) {
//									if (invoiceDetailBizTypeCode.equals(customField.getCode())) {
////										invoiceDetail.set
//										expenseTypeString = customField.getNameI18n();
//										break;
//									}
//								}

//								if ("1002".equals(invoiceDetailBizTypeCode) || "1003".equals(invoiceDetailBizTypeCode)) {
//									// 飞机， 火车/高铁
//									for (SpendCustomField taxCode : taxCodeList) {
//										if (taxCode.getNameI18n().toLowerCase().startsWith("9% input vat")) {
//											recordNew.setTaxCode(taxCode.getCode());
//											break;
//										}
//									}
//								} else if ("1004".equals(invoiceDetailBizTypeCode) || "4001".equals(invoiceDetailBizTypeCode)) {
//									// 差旅-地铁/公交/打车， 市内交通费
//									for (SpendCustomField taxCode : taxCodeList) {
//										if (taxCode.getNameI18n().toLowerCase().startsWith("3% input vat")) {
//											recordNew.setTaxCode(taxCode.getCode());
//											break;
//										}
//									}
//								} else {
//									// 其它费用类型
//									// 提取税率，提取对应SAP
//									String invoiceTaxRate = invoiceDetail.getInvoiceTaxRate();
//									if (invoiceTaxRate != null) {
//										invoiceTaxRate = StringUtil.taxRateFormat(invoiceTaxRate);
//										for (SpendCustomField taxCode : taxCodeList) {
//											if (taxCode.getNameI18n().toLowerCase().startsWith(invoiceTaxRate + " input vat")) {
//												// 匹配值假如以“6% input tax”开头
//												recordNew.setTaxCode(taxCode.getCode());
//												break;
//											}
//										}
//									}
//								}
//							}

						excelRecords.add(record);
						excelRecords.add(recordNew);
					}

					// 分摊行找不到发票的也要添加
					List<Allocation> allocations = reimburseData.getAllocations();
					List<Allocation> allocationList = new ArrayList<>();
					for (Allocation allocation : allocations) {
						boolean temp = false;
						for (InvoiceDetail invoiceDetail : invoiceDetailList) {
							if (invoiceDetail.getReimburseLineId().equals(allocation.getReimburseLineId())) {
								// 说明报账行内至少存在一张发票
								temp = true;
								break;
							}
						}
						if (!temp) {
							allocationList.add(allocation);
						}
					}

					// 遍历没有发票的分摊行列表
					for (Allocation allocation : allocationList) {
						ExcelRecord record = ExcelRecord.builder()
								.journalEntry("1")
								.companyCode("")
								.glAccount("") //费用类型对应
								.creditor(employeeNo)
								.amountInTransactionCurrency(String.valueOf(allocation.getReimbursementAmount()))
								.documentItemText("")
								.debitCreditCode("")	// 借记代码
								.wbsElement("")
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
						recordNew.setTaxCode("J0");
						recordNew.setAmountInTransactionCurrency(String.valueOf(allocation.getReimbursementAmount()));
						recordNew.setTaxAmount("0");

						String invoiceDetailBizTypeCode = allocation.getBizTypeCode();
						// 自定义维度表取报销行备注（费用类型），GLaccount费用类型代码
						if (invoiceDetailBizTypeCode != null) {
							for (SpendCustomField customField : expenseTypeList) {
								if (customField.getCode().equals(invoiceDetailBizTypeCode)) {
									recordNew.setDocumentItemText(customField.getNameI18n());
									break;
								}
							}
							for (SpendCustomField customField : glAccountList) {
								if (customField.getCode().equals(invoiceDetailBizTypeCode)) {
									recordNew.setGlAccount(customField.getNameI18n());
									break;
								}
							}
						}

						excelRecords.add(record);
						excelRecords.add(recordNew);
					}

				}

				// 生成上传CSV到SFTP
				try {
					String fileName = employeeNo + "_" + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date()) +".csv";
//					SftpUtil sftpUtil = new SftpUtil(Constants.SFTP_USER_ID, Constants.SFTP_PASSWORD, Constants.SFTP_HOST, 22);
//					sftpUtil.login();
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
//					baos.write(0xef);
//					baos.write(0xbb);
//					baos.write(0xbf);
					ExcelUtil.generateCsv(header, excelRecords, baos);
					FileOutputStream fos = new FileOutputStream("C:\\" + fileName);
					//追加BOM标识
//					fos.write(0xef);
//					fos.write(0xbb);
//					fos.write(0xbf);
					fos.write(baos.toByteArray());
					fos.close();

//					sftpUtil.upload("expfeishu2sap", fileName, baos.toByteArray());
					log.info("生成CSV文件：{}", fileName);
					baos.close();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
		log.info("生成文件结束");
	}
}
