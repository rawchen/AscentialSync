package com.lundong.ascentialsync.util;

import com.lundong.ascentialsync.entity.FeishuUser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author RawChen
 * @date 2023-03-03 17:44
 */
public class StringUtil {

	public static boolean isEmpty(String str) {
		return str == null || "".equals(str);
	}

	/**
	 * 部门转换
	 * 1正式 2实习 3外包 4劳务 5顾问 6离职 7试用
	 * A:正式 B:离职 C:试用期
	 * <p>
	 * 飞书管理后台->组织架构->成员字段管理
	 * 需要新增自定义人员类型字段离职和试用
	 *
	 * @param employeeType
	 * @return
	 */
	public static String employeeConvert(String employeeType) {
		if (employeeType == null) {
			return "A";
		} else if ("1".equals(employeeType)) {
			return "A";
		} else if ("2".equals(employeeType)) {
			return "C";
		} else if ("6".equals(employeeType)) {
			return "B";
		}
		return "A";
	}

	/**
	 * 去掉飞书返回的手机号国际字冠+86
	 *
	 * @param mobile
	 * @return
	 */
	public static String mobileDivAreaCode(String mobile) {
		if (mobile == null) {
			return "";
		} else if (mobile.startsWith("+86")) {
			return mobile.substring(3);
		} else {
			return mobile;
		}
	}

	public static String processChineseTitleOrder(String json) {
		json = json.replaceAll("\"iD\"", "\"ID\"")
				.replaceAll("\"customerCode\"", "\"客户代码\"")
				.replaceAll("\"customerName\"", "\"客户名称\"")
				.replaceAll("\"documentDate\"", "\"单据日期\"")
				.replaceAll("\"saleDeptCode\"", "\"销售部门代码\"")
				.replaceAll("\"sellerCode\"", "\"销售员代码\"")
				.replaceAll("\"documentMakerCode\"", "\"制单人代码\"")
				.replaceAll("\"modelName\"", "\"型号\"")
				.replaceAll("\"brandName\"", "\"品牌\"")
				.replaceAll("\"warehouseName\"", "\"仓库\"")
				.replaceAll("\"unitPrice\"", "\"含税单价\"")
				.replaceAll("\"unitPriceWithoutVAT\"", "\"未税单价\"")
				.replaceAll("\"exchangeRate\"", "\"汇率\"")
				.replaceAll("\"currency\"", "\"货币\"")
				.replaceAll("\"orderNumber\"", "\"订单号\"")
				.replaceAll("\"firstTransaction\"", "\"首次交易\"")
				.replaceAll("\"orderType\"", "\"订单类型\"")
				.replaceAll("\"quantity\"", "\"数量\"")
				.replaceAll("\"taxIncludedAmount\"", "\"含税金额(交易货币)（W）\"")
				.replaceAll("\"taxIncludedAmountRMB\"", "\"含税金额(RMB)（W）\"")
				.replaceAll("\"untaxedAmount\"", "\"未税金额(RMB)（W）\"")
				.replaceAll("\"actualUntaxedAmount\"", "\"实际未税金额(RMB)（W）\"")
				.replaceAll("\"charaterOfCustomer\"", "\"客户性质\"")
				.replaceAll("\"isKACustomer\"", "\"是否为KA客户\"");
		return json;
	}

	public static String processChineseTitleRefund(String json) {
		json = json.replaceAll("\"iD\"", "\"ID\"")
				.replaceAll("\"sellerNumber\"", "\"销售员编号\"")
				.replaceAll("\"saleDeptNumber\"", "\"销售部门编号\"")
				.replaceAll("\"date\"", "\"日期\"")
				.replaceAll("\"paymentAmount\"", "\"回款金额(W)\"")
				.replaceAll("\"paymentGrossMargin\"", "\"回款毛利(W)\"")
				.replaceAll("\"costAmount\"", "\"成本金额(W)\"")
				.replaceAll("\"customerPaymentAmount\"", "\"VIP客户回款金额(W)\"");
		return json;
	}

	public static String processChineseTitleReturnable(String json) {
		json = json.replaceAll("\"iD\"", "\"ID\"")
				.replaceAll("\"returnTime\"", "\"退货时间\"")
				.replaceAll("\"returnReason\"", "\"退货原因\"")
				.replaceAll("\"qualityProblem\"", "\"质量问题\"")
				.replaceAll("\"salesNumber\"", "\"所属销售员编号\"")
				.replaceAll("\"returnedQuantity\"", "\"退货数量\"")
				.replaceAll("\"unitPrice\"", "\"含税单价\"")
				.replaceAll("\"untaxedPrice\"", "\"未税单价\"")
				.replaceAll("\"unitAmount\"", "\"含税金额(交易货币)\"")
				.replaceAll("\"untaxedAmount\"", "\"未税金额(交易货币)\"")
				.replaceAll("\"unitAmountRmb\"", "\"含税金额(RMB)\"")
				.replaceAll("\"untaxedAmountRmb\"", "\"未税金额(RMB)\"")
				.replaceAll("\"unitAmountMyriad\"", "\"含税金额(交易货币)/W\"")
				.replaceAll("\"untaxedAmountMyriad\"", "\"未税金额(交易货币)/W\"")
				.replaceAll("\"unitAmountRmbMyriad\"", "\"含税金额(RMB)/W\"")
				.replaceAll("\"untaxedAmountRmbMyriad\"", "\"未税金额(RMB)/W\"")
				.replaceAll("\"model\"", "\"型号\"")
				.replaceAll("\"brand\"", "\"品牌\"")
				.replaceAll("\"deptNumber\"", "\"部门编号\"");
		return json;
	}

	public static byte[] readInputStream(InputStream inputStream) throws IOException {
		byte[] buffer = new byte[1024];
		int len = 0;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		while ((len = inputStream.read(buffer)) != -1) {
			bos.write(buffer, 0, len);
		}
		bos.close();
		return bos.toByteArray();
	}

	/**
	 * 如果String为null则返回空串
	 *
	 * @param str
	 * @return
	 */
	public static String nullIsEmpty(String str) {
		if (str == null) {
			return "";
		} else {
			return str;
		}
	}

	public static String getZhCustomFie(String nameI18n) {
		if (nameI18n == null) {
			return "";
		} else {
			nameI18n = nameI18n.substring(nameI18n.indexOf("\"zh\":\"") + 6);
			nameI18n = nameI18n.substring(0, nameI18n.indexOf("\""));
			return nameI18n;
		}
	}

	public static String taxRateFormat(String invoiceTaxRate) {
		if (invoiceTaxRate == null) {
			return "";
		} else {
			if (invoiceTaxRate.contains(".")) {
				invoiceTaxRate = invoiceTaxRate.substring(0, invoiceTaxRate.indexOf("."));
				invoiceTaxRate = invoiceTaxRate + "%";
				return invoiceTaxRate;
			} else {
				return "";
			}
		}
	}

	/**
	 * 行事由生成规则：[用户姓名]的[日常报销/差旅报销]-[单据报销事由]
	 * ER日常，TR差旅
	 * 如果超过超过20Char用...省略代替
	 *
	 * @param feishuUser
	 * @param formCode
	 * @param lineDesc
	 * @return
	 */
	public static String generateDocItemText(FeishuUser feishuUser, String formCode, String lineDesc) {
		String name = "";
		if (feishuUser != null && feishuUser.getName() != null) {
			name = feishuUser.getName();
		}

		if (formCode != null) {
			if (formCode.startsWith("ER")) {
				formCode = "日常报销";
			} else if (formCode.startsWith("TR")) {
				formCode = "差旅报销";
			}
		}

		// 格式：1405258-廉紫-SAP F2020项目，GB0350WW59\n与同行人Jodie分摊
		// 截取第二个横杠开始到最后
		if (lineDesc != null) {
			if ("".equals(lineDesc)) {
				lineDesc = "";
			} else {
				lineDesc = lineDesc.substring(lineDesc.indexOf("-", lineDesc.indexOf("-") + 1) + 1);
				lineDesc = lineDesc.replaceAll("\n", " ");
			}
		}

		String strResult = name + "的" + formCode + "-" + lineDesc;

		if ("".equals(lineDesc)) {
			strResult = strResult.substring(0, strResult.length() - 1);
		}

		if (strResult.length() <= 50) {
			return strResult;
		} else {
			return strResult.substring(0, 47) + "...";
		}
	}
}
