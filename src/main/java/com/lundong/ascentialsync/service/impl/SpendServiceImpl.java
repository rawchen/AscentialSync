package com.lundong.ascentialsync.service.impl;

import com.lundong.ascentialsync.config.Constants;
import com.lundong.ascentialsync.entity.*;
import com.lundong.ascentialsync.entity.spend.*;
import com.lundong.ascentialsync.enums.InvoiceTypeEnum;
import com.lundong.ascentialsync.service.SpendService;
import com.lundong.ascentialsync.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
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

	@Autowired
	private Constants constants;

	/**
	 * 每天定时同步昨天新增的差旅报销单和日常费用报销单，生成固定的csv文件到sftp的expfeishu2sap文件夹
	 */
	@Override
	@Scheduled(cron = "0 0 2 ? * *")
	public void syncSpendData() {

		Date date = new Date();
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		calendar.add(Calendar.DATE, -1);
		date = calendar.getTime();

		List<SpendCustomField> glAccountList = SignUtil.getSpendCustomFields("0000001");
		List<SpendCustomField> expenseTypeList = SignUtil.getSpendCustomFields("0000002");
		List<SpendCustomField> taxCodeList = SignUtil.getSpendCustomFields("0000003");
		List<SpendCustomField> deliveryCentreList = SignUtil.getSpendCustomFields("0000004");

		// 费控列表接口，先获取昨天添加到费控的表单列表
//		List<FeishuSpendForm> feishuSpendForms = SignUtil.spendForms(date);
//		List<FeishuSpendVoucher> feishuSpendVoucherList = SignUtil.spendFormsWithTimestamp(date);

		// 遍历支付池获取支付状态为A1待支付的单子
		List<FeishuPaypool> paypoolList = SignUtil.paypoolScrollWithTimestamp(date);
		List<FeishuSpendVoucher> feishuSpendVoucherList = SignUtil.spendFormsWithFormCodeList(paypoolList);
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

//		for (ReimburseData reimburseData : reimburseDataListNew) {
//			System.out.println(reimburseData);
//		}

		// 过滤出单据状态为已完成的
		reimburseDataListNew = reimburseDataListNew.stream().filter(r -> "completed".equals(r.getProcessStatus())).collect(Collectors.toList());

		log.info("过滤后表单数量: {}", reimburseDataListNew.size());
//		reimburseDataListNew = reimburseDataListNew.stream().filter(f -> f.getFormCode().equals("TR23060900005")).collect(Collectors.toList());
//		log.info("过滤后表单数量: {}", feishuSpendForms.size());

		List<GenerateEntity> generateEntityList = new ArrayList<>();
		for (ReimburseData form : reimburseDataListNew) {
			// 新建一个报销行ID列表
			// 存储报销总额和发票总额不相等的报销（排除）
			List<String> reimburseLineIdList = new ArrayList<>();
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
				// 某个单据的所有报销数据，在这先过滤价格不对应的单据
				for (ReimburseData data : reimburseDatas) {
					if (data.getAllocations() != null && data.getAllocations().size() > 0) {
						for (Allocation allocation : data.getAllocations()) {
							BigDecimal invoiceGrossAmount = new BigDecimal("0.0");
							// 根据每行报账数据的lineId获取
							for (InvoiceDetail invoiceDetail : data.getInvoiceDetailList()) {
								if (allocation.getReimburseLineId().equals(invoiceDetail.getReimburseLineId())) {
									// 累加发票额
									invoiceGrossAmount = invoiceGrossAmount.add(new BigDecimal(invoiceDetail.getGrossAmount()));
								}
							}
//							System.out.println(invoiceGrossAmount + "  " + allocation.getReimbursementAmount());
							// 判断同一报销行id的报销总额和发票总额是否相同，（不包含有报销没发票的，后面处理）
//							if (invoiceGrossAmount != allocation.getReimbursementAmount() && invoiceGrossAmount != 0) {
							if(invoiceGrossAmount.compareTo(BigDecimal.valueOf(allocation.getReimbursementAmount())) != 0) {
								// 如果金额不相同
								// 记录当前报销行id,供后面修改
								String lineId = allocation.getReimburseLineId();
								reimburseLineIdList.add(lineId);
								for (InvoiceDetail invoiceDetail : data.getInvoiceDetailList()) {
									if (invoiceDetail.getReimburseLineId().equals(lineId)) {
										invoiceDetail.setGrossAmount(String.valueOf(allocation.getReimbursementAmount()));
										invoiceDetail.setExcludeTaxAmount(String.valueOf(allocation.getReimbursementAmount()));
									}
								}
							}
						}
					}
				}

				// 单据审批结束时间approvedTime如果早于2023-07-11，则更改为它
				try {
					Date newDate = new SimpleDateFormat("yyyy-MM-dd").parse("2023-07-11");
					if (approvedTime.before(newDate)) {
						approvedTime = newDate;
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
								.reimburseLineId(invoiceDetail.getReimburseLineId())
								.journalEntry("1")
								.companyCode("")
								.glAccount("") //费用类型对应
								.creditor(employeeNo)
								.amountInTransactionCurrency(invoiceDetail.getGrossAmount())	// 区分含税和不含税
								.documentItemText("")
								.debitCreditCode("")	// 借记代码
//								.wbsElement(StringUtil.nullIsEmpty(StringUtil.getZhCustomFie(reimburseData.getProjectName())))
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
										// 2023.09.06 需求变更：更改行事由为 用户名+报销类别+报销事由
//										if (invoiceDetailBizTypeCode != null) {
//											for (SpendCustomField customField : expenseTypeList) {
//												if (customField.getCode().equals(invoiceDetailBizTypeCode)) {
//													recordNew.setDocumentItemText(customField.getNameI18n());
//												}
//											}
//										}
//										String lineDesc = allocation.getLineDesc();
										String applyReason = reimburseData.getApplyReason();
										String docItemTextResult = StringUtil.generateDocItemText(
												SignUtil.getFeishuUser(form.getApplicantUid()), form.getFormCode() , applyReason);
										recordNew.setDocumentItemText(docItemTextResult);
										record.setDocumentItemText(docItemTextResult);

										// DeliveryCentre
										if (!StringUtil.isEmpty(recordNew.getCostCenter())) {
											if (deliveryCentreList != null && deliveryCentreList.size() > 0) {
												for (SpendCustomField deliveryCentre : deliveryCentreList) {
													if (deliveryCentre.getCode().equals(recordNew.getCostCenter())) {
														recordNew.setDeliveryCentre(deliveryCentre.getNameI18n());
														break;
													}
												}
											}
											// 如果纬度表根据CostCenter找不到就取报销行自己的自定义DeliveryCentre
											if (StringUtil.isEmpty(recordNew.getDeliveryCentre())) {
												List<CustomField> customFieldList = allocation.getCustomFieldList();
												if (customFieldList != null) {
													for (CustomField customField : customFieldList) {
														// 字段所在自定义维度编码
														if ("0001".equals(customField.getFieldCode())) {
															recordNew.setDeliveryCentre(customField.getFieldValueCode());
															break;
														}
													}
												}
											}
										}

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
									// 如果费用类型为“2001体检费、2002团建费、2003零食/下午茶、2005员工福利其他、4004餐饮招待”则税码J0
									if ("2005".equals(invoiceDetailBizTypeCode) || "2001".equals(invoiceDetailBizTypeCode)
											|| "2002".equals(invoiceDetailBizTypeCode) || "2003".equals(invoiceDetailBizTypeCode)
											|| "4004".equals(invoiceDetailBizTypeCode)) {
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
													// 免税
													if (invoiceTaxRate.startsWith("-999")) {
														recordNew.setTaxCode("J0");
													} else {
														invoiceTaxRate = StringUtil.taxRateFormat(invoiceTaxRate);
														for (SpendCustomField taxCode : taxCodeList) {
															if (taxCode.getNameI18n().toLowerCase().startsWith(invoiceTaxRate + " transportation input vat")) {
																recordNew.setTaxCode(taxCode.getCode());
																break OUT;
															}
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
											if (taxCode.getNameI18n().toLowerCase().startsWith("3% transportation input vat")) {
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
										// 航空机票行程单
										// 通过发票报销行ID获取报销总额
										// 设置税码和税额为空（后续修改为税码J0税额0）
										// 标记发票报销行ID
										List<Allocation> allocations = reimburseData.getAllocations();
										if (allocations != null) {
											for (Allocation allocation : allocations) {
												if (invoiceDetail.getReimburseLineId().equals(allocation.getReimburseLineId())) {
													record.setAmountInTransactionCurrency(String.valueOf(allocation.getReimbursementAmount()));
													recordNew.setAmountInTransactionCurrency(String.valueOf(allocation.getReimbursementAmount()));
													break;
												}
											}
										}
										record.setTaxCode("");
										recordNew.setTaxCode("J0");
										record.setTaxAmount("");
										recordNew.setTaxAmount("0");
										record.setReimburseLineId(invoiceDetail.getReimburseLineId());
										recordNew.setReimburseLineId(invoiceDetail.getReimburseLineId());
										reimburseLineIdList.add(invoiceDetail.getReimburseLineId());
										break;
									case TRAIN_TICKET:
										// 火车票
										for (SpendCustomField taxCode : taxCodeList) {
											if (taxCode.getNameI18n().toLowerCase().startsWith("9% transportation input vat")) {
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

								// 继续根据税率大于0判断如果费用类型为“2001体检费、2002团建费、2003零食/下午茶、2005员工福利其他、4004餐饮招待”则税码J0
								if (Double.parseDouble(recordNew.getTaxAmount()) > 0) {
									if (form.getBizUnitCode().contains("EXPENSE_REIMBURSEMENT")) {
										if ("2005".equals(invoiceDetailBizTypeCode) || "2001".equals(invoiceDetailBizTypeCode)
												|| "2002".equals(invoiceDetailBizTypeCode) || "2003".equals(invoiceDetailBizTypeCode)
												|| "4004".equals(invoiceDetailBizTypeCode)) {
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
//							for (SpendCustomField customField : expenseTypeList) {
//								if (customField.getCode().equals(invoiceDetailBizTypeCode)) {
//									recordNew.setDocumentItemText(customField.getNameI18n());
//									break;
//								}
//							}
							for (SpendCustomField customField : glAccountList) {
								if (customField.getCode().equals(invoiceDetailBizTypeCode)) {
									recordNew.setGlAccount(customField.getNameI18n());
									break;
								}
							}
						}

						String applyReason = reimburseData.getApplyReason();
						String docItemTextResult = StringUtil.generateDocItemText(
								SignUtil.getFeishuUser(form.getApplicantUid()), form.getFormCode() , applyReason);
						recordNew.setDocumentItemText(docItemTextResult);
						record.setDocumentItemText(docItemTextResult);

						// DeliveryCentre
						if (!StringUtil.isEmpty(recordNew.getCostCenter())) {
							if (deliveryCentreList != null && deliveryCentreList.size() > 0) {
								for (SpendCustomField deliveryCentre : deliveryCentreList) {
									if (deliveryCentre.getCode().equals(recordNew.getCostCenter())) {
										recordNew.setDeliveryCentre(deliveryCentre.getNameI18n());
										break;
									}
								}
							}
							// 如果纬度表根据CostCenter找不到就取报销行自己的自定义DeliveryCentre
							if (StringUtil.isEmpty(recordNew.getDeliveryCentre())) {
								List<CustomField> customFieldList = allocation.getCustomFieldList();
								if (customFieldList != null) {
									for (CustomField customField : customFieldList) {
										// 字段所在自定义维度编码
										if ("0001".equals(customField.getFieldCode())) {
											recordNew.setDeliveryCentre(customField.getFieldValueCode());
											break;
										}
									}
								}
							}
						}

						excelRecords.add(record);
						excelRecords.add(recordNew);
					}
//					System.out.println(reimburseLineIdList);

					// 去重
					reimburseLineIdList = reimburseLineIdList.stream().distinct().collect(Collectors.toList());

					// excelRecords根据reimburseLineIdList去重
					excelRecords = DataFilterUtil.distinctByReimburseLineId(excelRecords, reimburseLineIdList);

					// excelRecords中reimburseLineId不为空的设置税码，税额为空（后续修改为税码J0税额0），且更改贷方交易金额为借方交易金额
					for (int i = 0; i < excelRecords.size(); i++) {
						if (reimburseLineIdList.contains(excelRecords.get(i).getReimburseLineId())) {
							if ("H".equals(excelRecords.get(i).getDebitCreditCode())) {
								excelRecords.get(i).setTaxCode("");
								excelRecords.get(i).setTaxAmount("");
							} else if ("S".equals(excelRecords.get(i).getDebitCreditCode())) {
								if (excelRecords.get(i - 1) != null) {
									excelRecords.get(i).setAmountInTransactionCurrency(excelRecords.get(i - 1).getAmountInTransactionCurrency());
								}
								excelRecords.get(i).setTaxCode("J0");
								excelRecords.get(i).setTaxAmount("0");
							}
						}
					}
				}


				// 生成上传CSV到SFTP
				try {
					String fileName = employeeNo + "_" + new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date()) +".csv";
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
//					baos.write(0xef);
//					baos.write(0xbb);
//					baos.write(0xbf);
					ExcelUtil.generateCsv(header, excelRecords, baos);
//					FileOutputStream fos = new FileOutputStream("/Users/rawchen/" + fileName);
					//追加BOM标识
//					fos.write(0xef);
//					fos.write(0xbb);
//					fos.write(0xbf);
//					fos.write(baos.toByteArray());
//					fos.close();
					generateEntityList.add(new GenerateEntity()
							.setByteArrayOutputStream(baos)
							.setFileName(fileName)
							.setFormCode(form.getFormCode()));
					baos.close();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}

		log.info("需要上传的报销单文件数: {}", reimburseDataListNew.size());
		SftpUtil sftpUtil = new SftpUtil(constants.SFTP_USER_ID, constants.SFTP_PASSWORD, constants.SFTP_HOST, 22);
		sftpUtil.login();
		if (sftpUtil.getSftp() == null) {
			// 登录失败，同步失败
			SignUtil.sendMsg(constants.CHAT_ID_ARG, constants.USER_ID_ARG, "SFTP登录错误导致报销单更新失败，请查看日志后调用接口手动同步。");
		} else {
			List<GenerateEntity> resultSuccessList = new ArrayList<>();
			List<GenerateEntity> resultFailList = new ArrayList<>();
			for (GenerateEntity generateEntity : generateEntityList) {
				boolean result = sftpUtil.upload("expfeishu2sap", generateEntity.getFileName(), generateEntity.getByteArrayOutputStream().toByteArray());
				if (result) {
					resultSuccessList.add(generateEntity);
					log.info("上传CSV文件成功：{}", generateEntity.getFileName());
				} else {
					resultFailList.add(generateEntity);
					log.info("上传CSV文件失败：{}", generateEntity.getFileName());
				}
			}
			log.info("上传成功的报销单文件数: {}", resultSuccessList.size());
			if (resultSuccessList.size() < reimburseDataListNew.size()) {
				StringBuilder successFormCodeListStr = new StringBuilder();
				for (GenerateEntity generateEntitySuccess : resultFailList) {
					successFormCodeListStr.append(generateEntitySuccess.getFormCode()).append("\n");
				}
				StringBuilder failFormCodeListStr = new StringBuilder();
				for (GenerateEntity generateEntity : resultFailList) {
					failFormCodeListStr.append(generateEntity.getFormCode()).append("\n");
				}
				log.info("生成报销单部分失败。\n成功上传单据号如下：\n" + successFormCodeListStr + "\n失败上传单据号如下：\n" + failFormCodeListStr);
				SignUtil.sendMsg(constants.CHAT_ID_ARG, constants.USER_ID_ARG,
						"生成报销单部分失败，请查看日志。\n成功上传单据号如下：\n" + successFormCodeListStr + "\n失败上传单据号如下：\n" + failFormCodeListStr);
			}
			log.info("生成文件结束");
		}

	}
}
