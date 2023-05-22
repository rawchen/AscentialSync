package com.lundong.ascentialsync.util;

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
	 *
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
}
