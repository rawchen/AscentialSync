package com.lundong.ascentialsync.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lundong.ascentialsync.config.Constants;
import com.lundong.ascentialsync.entity.*;
import com.lundong.ascentialsync.entity.spend.SpendCustomField;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.HttpCookie;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author RawChen
 * @date 2023-03-08 18:37
 */
@Slf4j
public class SignUtil {

	/**
	 * SAP系统自定义签名规则
	 *
	 * @param objects
	 * @param secretKey
	 * @param requestJson
	 * @return
	 */
	public static String makeMd5Token(Map<String, String> objects, String secretKey, String requestJson) {
		StringBuilder content = new StringBuilder();
		content.append(secretKey);
		// 对 resultmap 中的参数进行排序
		List<String> keyList = new ArrayList<>();
		Iterator<Map.Entry<String, String>> ite = objects.entrySet().iterator();
		while (ite.hasNext()) {
			keyList.add(ite.next().getKey());
		}
		Collections.sort(keyList);
		// 拼接 secretKey
		for (String key : keyList) {
			content.append(key).append(objects.get(key));
		}
		content.append(requestJson).append(secretKey);
		// 生成 md5 签名
		return DigestUtils.md5Hex(content.toString());
	}

	/**
	 * 飞书自建应用获取tenant_access_token
	 */
	public static String getAccessToken(String appId, String appSecret) {
		JSONObject object = new JSONObject();
		object.put("app_id", appId);
		object.put("app_secret", appSecret);
		String resultStr = HttpRequest.post("https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal")
				.form(object)
				.execute().body();
		if (StringUtils.isNotEmpty(resultStr)) {
			JSONObject resultObject = (JSONObject) JSON.parse(resultStr);
			if (!"0".equals(resultObject.getString("code"))) {
				return "";
			} else {
				String tenantAccessToken = resultObject.getString("tenant_access_token");
				if (tenantAccessToken != null) {
					return tenantAccessToken;
				}
			}
		}
		return "";
	}

	/**
	 * 根据OPEN ID获取部门ID和部门名
	 *
	 * @param accessToken
	 * @param openDepartmentId
	 * @return
	 */
	public static String getDepartmentIdAndName(String accessToken, String openDepartmentId) {
		String resultStr = HttpRequest.get(
						"https://open.feishu.cn/open-apis/contact/v3/departments/"
								+ openDepartmentId
								+ "?department_id_type=open_department_id&user_id_type=user_id")
				.header("Authorization", "Bearer " + accessToken)
				.execute().body();
		if (StringUtils.isNotEmpty(resultStr)) {
			JSONObject resultObject = (JSONObject) JSON.parse(resultStr);
			if (!"0".equals(resultObject.getString("code"))) {
				return "";
			} else {
				JSONObject data = (JSONObject) resultObject.get("data");
				JSONObject department = (JSONObject) data.get("department");
				String department_id = department.getString("department_id");
				String name = department.getString("name");
				return department_id + "," + name;
			}
		}
		return "";
	}

	/**
	 * 根据OPEN ID获取部门ID和部门名(已自动生成access token)
	 *
	 * @param openDepartmentId
	 * @return
	 */
	public static String getDepartmentIdAndName(String openDepartmentId) {
		if (StringUtil.isEmpty(openDepartmentId) || "0".equals(openDepartmentId)) {
			return "0";
		} else {
			String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
			return getDepartmentIdAndName(accessToken, openDepartmentId);
		}
	}

	/**
	 * 多维表格新增多条记录
	 *
	 * @param json
	 * @return
	 */
	public static List<String> batchInsertRecord(String json, String appToken, String tableId, String name) {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return batchInsertRecord(accessToken, json, appToken, tableId, name);
	}

	/**
	 * 多维表格新增多条记录
	 *
	 * @param accessToken
	 * @param json
	 * @return
	 */
	private static List<String> batchInsertRecord(String accessToken, String json, String appToken, String tableId, String name) {
		List<String> recordIds = new ArrayList<>();
		String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/bitable/v1/apps/" + appToken + "/tables/" + tableId + "/records/batch_create")
				.header("Authorization", "Bearer " + accessToken)
				.body(json)
				.execute()
				.body();
		if (StringUtils.isNotEmpty(resultStr)) {
			JSONObject resultObject = (JSONObject) JSON.parse(resultStr);
			if (!"0".equals(resultObject.getString("code"))) {
				log.info("批量插入失败：{}", resultObject.getString("msg"));
				return new ArrayList<>();
			} else {
				JSONObject data = (JSONObject) resultObject.get("data");
				JSONArray records = (JSONArray) data.get("records");
				for (int i = 0; i < records.size(); i++) {
					JSONObject jsonObject = records.getJSONObject(i);
					String recordId = jsonObject.getString("record_id");
					JSONObject fields = (JSONObject) jsonObject.get("fields");
					String nameTemp = fields.getString(name);
					if (nameTemp == null) {
						nameTemp = " ";
					}
					recordIds.add(recordId + "," + nameTemp);
				}
			}
		}
		return recordIds;
	}

	/**
	 * 批量根据内存记录的插入记录ids清空表格
	 *
	 * @param recordIds
	 * @param appToken
	 * @param tableId
	 */
	public static void batchClearTable(List<String> recordIds, String appToken, String tableId) {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		batchClearTable(accessToken, recordIds, appToken, tableId);
	}

	/**
	 * 批量根据内存记录的插入记录ids清空表格
	 *
	 * @param accessToken
	 * @param recordIds
	 * @param appToken
	 * @param tableId
	 */
	public static void batchClearTable(String accessToken, List<String> recordIds, String appToken, String tableId) {
		log.info("===开始批量删除 {} ===", tableId);
		if (recordIds != null && recordIds.size() > 0) {
			List<List<String>> partitions = ListUtils.partition(recordIds, 500);
			for (List<String> partition : partitions) {
				JSONObject object = new JSONObject();
				object.put("records", JSONArray.parseArray(JSON.toJSONString(partition)));
				String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/bitable/v1/apps/" + appToken + "/tables/" + tableId + "/records/batch_delete")
						.header("Authorization", "Bearer " + accessToken)
						.body(object.toJSONString())
						.execute()
						.body();
				if (StringUtils.isNotEmpty(resultStr)) {
					JSONObject resultObject = (JSONObject) JSON.parse(resultStr);
					if (!"0".equals(resultObject.getString("code"))) {
						log.info("批量删除失败：{}", resultObject.getString("msg"));
					}
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		log.info("===批量删除完成===");
	}

	/**
	 * 获取部门直属用户列表
	 *
	 * @return
	 */
	public static List<FeishuUser> findByDepartment() {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return findByDepartment(accessToken);
	}

	/**
	 * 获取部门直属用户列表
	 *
	 * @param accessToken
	 * @return
	 */
	public static List<FeishuUser> findByDepartment(String accessToken) {
		List<FeishuUser> users = new ArrayList<>();
		Map<String, Object> param = new HashMap<>();
		while (true) {
			param.put("department_id", 0);
			param.put("page_size", 50);
			String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/contact/v3/users/find_by_department")
					.header("Authorization", "Bearer " + accessToken)
					.form(param)
					.execute()
					.body();
//			System.out.println(resultStr);
			JSONObject jsonObject = JSON.parseObject(resultStr);
			JSONObject data = (JSONObject) jsonObject.get("data");
			JSONArray items = (JSONArray) data.get("items");
			for (int i = 0; i < items.size(); i++) {
				// 构造飞书用户对象
				FeishuUser feishuUser = items.getJSONObject(i).toJavaObject(FeishuUser.class);
				JSONArray departmentIds = items.getJSONObject(i).getJSONArray("department_ids");
				if (departmentIds.size() >= 2) {
					feishuUser.setDepartmentId(departmentIds.getString(1));
				} else {
					feishuUser.setDepartmentId("0");
				}
				users.add(feishuUser);
			}

			if ((boolean) data.get("has_more")) {
				param.put("page_token", data.getString("page_token"));
			} else {
				break;
			}
		}
		return users;
	}

	/**
	 * 获取子部门列表
	 *
	 * @return
	 */
	public static List<FeishuDept> departments() {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return departments(accessToken);
	}

	/**
	 * 获取子部门列表
	 *
	 * @param accessToken
	 * @return
	 */
	public static List<FeishuDept> departments(String accessToken) {
		List<FeishuDept> depts = new ArrayList<>();
		Map<String, Object> param = new HashMap<>();
		while (true) {
			param.put("user_id_type", "open_id");
			param.put("department_id_type", "department_id");
			param.put("fetch_child", true);
			param.put("page_size", 10);
			String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/contact/v3/departments/0/children")
					.header("Authorization", "Bearer " + accessToken)
					.form(param)
					.execute()
					.body();
//			System.out.println(resultStr);
			JSONObject jsonObject = JSON.parseObject(resultStr);
			JSONObject data = (JSONObject) jsonObject.get("data");
			JSONArray items = (JSONArray) data.get("items");
			for (int i = 0; i < items.size(); i++) {
				// 构造飞书用户对象
				FeishuDept feishuDept = items.getJSONObject(i).toJavaObject(FeishuDept.class);
				depts.add(feishuDept);
			}

			if ((boolean) data.get("has_more")) {
				param.put("page_token", data.getString("page_token"));
			} else {
				break;
			}
		}
		return depts;
	}

	/**
	 * 登录金蝶测试
	 *
	 * @return
	 */
	public static List<HttpCookie> loginCookies() {
		String loginUrl = "http://192.168.121.129/K3Cloud/Kingdee.BOS.WebApi.ServicesStub.AuthService.ValidateUser.common.kdsvc";
		String loginJson = "{\n" +
				"    \"acctID\": \"642427270e9f87\",\n" +
				"    \"username\": \"Administrator\",\n" +
				"    \"password\": \"Admin123456.\",\n" +
				"    \"lcid\": \"2052\"\n" +
				"}";
		HttpResponse loginResponse = HttpRequest.post(loginUrl.toString())
				.body(loginJson)
				.execute();
		return loginResponse.getCookies();
	}

	/**
	 * 获取凭证列表
	 *
	 * @param accessToken
	 * @return
	 */
	public static List<FeishuSpendVoucher> spendVouchers(String accessToken, String formCode) {
		List<FeishuSpendVoucher> vouchers = new ArrayList<>();
		Map<String, Object> param = new HashMap<>();
		while (true) {
			param.put("page_size", 20);
			param.put("form_code", formCode);
			String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/spend/v1/vouchers/scroll")
					.header("Authorization", "Bearer " + accessToken)
					.form(param)
					.execute()
					.body();
//			System.out.println(resultStr);
			JSONObject jsonObject = JSON.parseObject(resultStr);
			if (!"0".equals(jsonObject.getString("code"))) {
				log.info("获取凭证列表失败：{}", resultStr);
				break;
			}
			JSONObject data = (JSONObject) jsonObject.get("data");
			if (data != null) {
				JSONArray items = (JSONArray) data.get("items");
				for (int i = 0; i < items.size(); i++) {
					// 构造飞书费控凭证
					FeishuSpendVoucher feishuDept = items.getJSONObject(i).toJavaObject(FeishuSpendVoucher.class);
					vouchers.add(feishuDept);
				}

				if ((boolean) data.get("has_more")) {
					param.put("page_token", data.getString("page_token"));
				} else {
					break;
				}
			} else {
				break;
			}

		}
		return vouchers;
	}

	/**
	 * 获取凭证列表
	 *
	 * @return
	 */
	public static List<FeishuSpendVoucher> spendVouchers(String formCode) {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return spendVouchers(accessToken, formCode);
	}

	/**
	 * 获取表单列表
	 *
	 * @param accessToken
	 * @param searchDate
	 * @return
	 */
	public static List<FeishuSpendForm> spendForms(String accessToken, Date searchDate) {
		List<FeishuSpendForm> forms = new ArrayList<>();
		Map<String, Object> param = new HashMap<>();
		param.put("page_size", 20);
		if (searchDate != null) {
//			Date date = new Date();
//			Calendar calendar = new GregorianCalendar();
//			calendar.setTime(date);
//			calendar.add(Calendar.DATE, -10);
//			date = calendar.getTime();
//
//			Date endDate = new Date();
//			Calendar endCalendar = new GregorianCalendar();
//			endCalendar.setTime(endDate);
//			endCalendar.add(Calendar.DATE, -1);
//			endDate = endCalendar.getTime();

			param.put("submit_start_time", new SimpleDateFormat("yyyy-MM-dd").format(searchDate));
			param.put("submit_end_time", new SimpleDateFormat("yyyy-MM-dd").format(searchDate));
		}
		while (true) {
			String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/spend/v1/forms")
					.header("Authorization", "Bearer " + accessToken)
					.form(param)
					.execute()
					.body();
//			System.out.println(resultStr);
			JSONObject jsonObject = JSON.parseObject(resultStr);
			JSONObject data = (JSONObject) jsonObject.get("data");
			JSONArray items = (JSONArray) data.get("items");
			for (int i = 0; i < items.size(); i++) {
				// 构造飞书费控凭证
				FeishuSpendForm form = items.getJSONObject(i).toJavaObject(FeishuSpendForm.class);
				forms.add(form);
			}

			if ((boolean) data.get("has_more")) {
				param.put("page_token", data.getString("page_token"));
			} else {
				break;
			}
		}
		return forms;
	}

	/**
	 * 获取表单列表
	 *
	 * @param searchDate
	 * @return
	 */
	public static List<FeishuSpendForm> spendForms(Date searchDate) {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return spendForms(accessToken, searchDate);
	}

	/**
	 * 获取凭证列表
	 *
	 * @param accessToken
	 * @param searchDate
	 * @return
	 */
	public static List<FeishuSpendVoucher> spendFormsWithTimestamp(String accessToken, Date searchDate) {
		List<FeishuSpendVoucher> vouchers = new ArrayList<>();
		Map<String, Object> param = new HashMap<>();
		param.put("page_size", 20);
		if (searchDate != null) {
//			Date date = new Date();
//			Calendar calendar = new GregorianCalendar();
//			calendar.setTime(date);
//			calendar.add(Calendar.DATE, -10);
//			date = calendar.getTime();
//
//			Date endDate = new Date();
//			Calendar endCalendar = new GregorianCalendar();
//			endCalendar.setTime(endDate);
//			endCalendar.add(Calendar.DATE, -1);
//			endDate = endCalendar.getTime();

			param.put("index_time_after", TimeUtil.getDailyStartTime(searchDate));
			param.put("index_time_before", TimeUtil.getDailyEndTime(searchDate));
		}
		while (true) {
			String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/spend/v1/vouchers/scroll")
					.header("Authorization", "Bearer " + accessToken)
					.form(param)
					.execute()
					.body();
//			System.out.println(resultStr);
			JSONObject jsonObject = JSON.parseObject(resultStr);
			if (!"0".equals(jsonObject.getString("code"))) {
				log.info("获取凭证列表失败：{}", resultStr);
				break;
			}
			JSONObject data = (JSONObject) jsonObject.get("data");
			if (data != null) {
				JSONArray items = (JSONArray) data.get("items");
				for (int i = 0; i < items.size(); i++) {
					// 构造飞书费控凭证
					FeishuSpendVoucher feishuDept = items.getJSONObject(i).toJavaObject(FeishuSpendVoucher.class);
					vouchers.add(feishuDept);
				}

				if ((boolean) data.get("has_more")) {
					param.put("page_token", data.getString("page_token"));
				} else {
					break;
				}
			} else {
				break;
			}
		}
		return vouchers;
	}

	/**
	 * 获取凭证列表
	 *
	 * @param searchDate
	 * @return
	 */
	public static List<FeishuSpendVoucher> spendFormsWithTimestamp(Date searchDate) {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return spendFormsWithTimestamp(accessToken, searchDate);
	}

	/**
	 * 更新飞书用户只能定义字段：公司编码、成本中心编码
	 *
	 * @return
	 */
	public static boolean updateFeishuUser(FeishuUser user, String companyCodeAttrId, String costCenterCodeAttrId) {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return updateFeishuUser(accessToken, user, companyCodeAttrId, costCenterCodeAttrId);
	}

	/**
	 * 更新飞书用户只能定义字段：公司编码、成本中心编码
	 *
	 * @param accessToken
	 * @return
	 */
	public static boolean updateFeishuUser(String accessToken, FeishuUser user, String companyCodeAttrId, String costCenterCodeAttrId) {
		Map<String, Object> param = new HashMap<>();
//		param.put("user_id_type", "user_id");
//		param.put("department_id_type", "department_id");
		JSONObject object = new JSONObject();
		String json = "[{\"type\":\"TEXT\",\"id\":\"companyCodeId\",\"value\": {\"text\": \"companyCodeText\"}},{\"type\":\"TEXT\",\"id\":\"costCenterCodeId\",\"value\": {\"text\": \"costCenterCodeText\"}}]";
		json = json.replaceAll("companyCodeId", companyCodeAttrId);
		json = json.replaceAll("costCenterCodeId", costCenterCodeAttrId);
		json = json.replaceAll("companyCodeText", user.getCompanyCode());
		json = json.replaceAll("costCenterCodeText", user.getCostCenterCode());
		object.put("custom_attrs", JSONArray.parseArray(json));
		String resultStr = HttpRequest.patch("https://open.feishu.cn/open-apis/contact/v3/users/" + user.getUserId() + "?user_id_type=user_id&department_id_type=department_id")
				.header("Authorization", "Bearer " + accessToken)
				.form(param)
				.body(object.toJSONString())
				.execute()
				.body();
//		System.out.println(resultStr);
		JSONObject jsonObject = JSONObject.parseObject(resultStr);
		if ("0".equals(jsonObject.getString("code"))) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 获取自定义字段列表
	 *
	 * @return
	 */
	public static List<CustomAttr> getCustomAttrs() {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return getCustomAttrs(accessToken);
	}

	/**
	 * 获取自定义字段列表
	 *
	 * @param accessToken
	 * @return
	 */
	public static List<CustomAttr> getCustomAttrs(String accessToken) {
		List<CustomAttr> customAttrList = new ArrayList<>();
		Map<String, Object> param = new HashMap<>();
		param.put("page_size", "100");
		String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/contact/v3/custom_attrs")
				.header("Authorization", "Bearer " + accessToken)
				.form(param)
				.execute()
				.body();
		JSONObject jsonObject = JSONObject.parseObject(resultStr);
		if ("0".equals(jsonObject.getString("code"))) {
			JSONObject dataJsonObject = jsonObject.getJSONObject("data");
			if (dataJsonObject != null) {
				JSONArray items = dataJsonObject.getJSONArray("items");
				if (items != null) {

					for (int i = 0; i < items.size(); i++) {
						String id = items.getJSONObject(i).getString("id");
						String type = items.getJSONObject(i).getString("type");
						CustomAttr customAttr = CustomAttr.builder()
								.id(id)
								.type(type)
								.build();
						JSONArray i18n = items.getJSONObject(i).getJSONArray("i18n_name");
						for (int j = 0; j < i18n.size(); j++) {
							if ("default".equals(i18n.getJSONObject(j).getString("locale"))) {
								customAttr.setValue(i18n.getJSONObject(j).getString("value"));
								break;
							}
						}
						customAttrList.add(customAttr);

					}
				}

			}
		}
		return customAttrList;
	}

	/**
	 * 获取飞书用户
	 *
	 * @param accessToken
	 * @return
	 */
	public static FeishuUser getFeishuUser(String accessToken, String userId) {
		List<CustomAttr> customAttrList = getCustomAttrs();

		Map<String, Object> param = new HashMap<>();
		param.put("user_id_type", "user_id");
		param.put("department_id_type", "department_id");
		FeishuUser feishuUser = new FeishuUser();
		String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/contact/v3/users/" + userId)
				.header("Authorization", "Bearer " + accessToken)
				.form(param)
				.execute()
				.body();
//		System.out.println(resultStr);
		JSONObject jsonObject = JSONObject.parseObject(resultStr);
		if ("0".equals(jsonObject.getString("code"))) {
			JSONObject user = jsonObject.getJSONObject("data").getJSONObject("user");
			if (user != null) {
				// userId jobTitle
				feishuUser.setUserId(user.getString("user_id"));
				feishuUser.setEmployeeNo(user.getString("employee_no"));
				// customAttrs
				JSONArray customAttrs = user.getJSONArray("custom_attrs");
				if (customAttrs != null) {
					for (int i = 0; i < customAttrs.size(); i++) {
						String id = customAttrs.getJSONObject(i).getString("id");
//					System.out.println("id:" + id);
						for (int j = 0; j < customAttrList.size(); j++) {
							CustomAttr customAttr = customAttrList.get(j);
//						System.out.println("customAttr:" + customAttr);
							if (customAttr.getId().equals(id) && "Company Code".equals(customAttr.getValue())) {
								feishuUser.setCompanyCode(customAttrs.getJSONObject(i).getJSONObject("value").getString("text"));
								break;
							}
							if (customAttr.getId().equals(id) && "Cost Center Code".equals(customAttr.getValue())) {
								feishuUser.setCostCenterCode(customAttrs.getJSONObject(i).getJSONObject("value").getString("text"));
								break;
							}
						}
					}
				}

			}
			return feishuUser;
		} else {
			return null;
		}
	}

	/**
	 * 获取飞书用户
	 *
	 * @return
	 */
	public static FeishuUser getFeishuUser(String userId) {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return getFeishuUser(accessToken, userId);
	}

	/**
	 * 飞书（标准版）获取花名册信息
	 *
	 * @param accessToken
	 * @return
	 */
	public static List<FeishuUser> getFeishuEmployees(String accessToken) {
		List<FeishuUser> feishuEmployees = new ArrayList<>();
		Map<String, Object> param = new HashMap<>();

//		优化：将view改为base，匹配需要同步的员工id具体为哪些，再传值user_ids精确查询员工full全字段列表
//		if (employeeNoList != null) {
//			// 解析员工编号为userId编号并传为user_ids，形如user_ids=xxxx&user_ids=xxxxx
//			param.put("user_ids", );
//		}
		param.put("view", "full");
		param.put("user_id_type", "user_id");
		param.put("page_size", "100");
		while (true) {
			String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/ehr/v1/employees")
					.header("Authorization", "Bearer " + accessToken)
					.form(param)
					.execute()
					.body();
//			System.out.println(resultStr);
			JSONObject jsonObject = JSON.parseObject(resultStr);
			if (jsonObject != null && "0".equals(jsonObject.getString("code"))) {
				// 获取到100个员工
				JSONObject data = (JSONObject) jsonObject.get("data");
				JSONArray items = (JSONArray) data.get("items");
				for (int i = 0; i < items.size(); i++) {
					// 构造飞书用户
					FeishuUser feishuUser = new FeishuUser();
					feishuUser.setUserId(items.getJSONObject(i).getString("user_id"));
					JSONObject systemFields = items.getJSONObject(i).getJSONObject("system_fields");
					feishuUser.setEmployeeNo(systemFields.getString("employee_no"));
					JSONArray customFields = items.getJSONObject(i).getJSONArray("custom_fields");
					for (int j = 0; j < customFields.size(); j++) {
						if ("Company Code".equals(customFields.getJSONObject(j).getString("label"))) {
							feishuUser.setCompanyCode(customFields.getJSONObject(j).getString("value"));
							break;
						}
					}
					for (int j = 0; j < customFields.size(); j++) {
						if ("Cost Center Code".equals(customFields.getJSONObject(j).getString("label"))) {
							feishuUser.setCostCenterCode(customFields.getJSONObject(j).getString("value"));
							break;
						}
					}
					feishuEmployees.add(feishuUser);
				}

				if ((boolean) data.get("has_more")) {
					param.put("page_token", data.getString("page_token"));
				} else {
					break;
				}
			}
		}
		return feishuEmployees;
	}

	/**
	 * 飞书（标准版）获取花名册信息
	 *
	 * @return
	 */
	public static List<FeishuUser> getFeishuEmployees() {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return getFeishuEmployees(accessToken);
	}

	/**
	 * 飞书（标准版）获取花名册基础信息
	 *
	 * @param accessToken
	 * @return
	 */
	public static List<FeishuUser> getFeishuBaseEmployees(String accessToken) {
		List<FeishuUser> feishuEmployees = new ArrayList<>();
		Map<String, Object> param = new HashMap<>();
		param.put("view", "basic");
		param.put("user_id_type", "user_id");
		param.put("page_size", "100");
		while (true) {
			String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/ehr/v1/employees")
					.header("Authorization", "Bearer " + accessToken)
					.form(param)
					.execute()
					.body();
			JSONObject jsonObject = JSON.parseObject(resultStr);
			if (jsonObject != null && "0".equals(jsonObject.getString("code"))) {
				// 获取到100个员工
				JSONObject data = (JSONObject) jsonObject.get("data");
				JSONArray items = (JSONArray) data.get("items");
				for (int i = 0; i < items.size(); i++) {
					// 构造飞书用户
					FeishuUser feishuUser = new FeishuUser();
					feishuUser.setUserId(items.getJSONObject(i).getString("user_id"));
					JSONObject systemFields = items.getJSONObject(i).getJSONObject("system_fields");
					feishuUser.setEmployeeNo(systemFields.getString("employee_no"));
					feishuEmployees.add(feishuUser);
				}

				if ((boolean) data.get("has_more")) {
					param.put("page_token", data.getString("page_token"));
				} else {
					break;
				}
			}
		}
		feishuEmployees = feishuEmployees.stream().filter(e -> !e.getEmployeeNo().isEmpty()).collect(Collectors.toList());
		return feishuEmployees;
	}

	/**
	 * 飞书（标准版）获取花名册基础信息
	 *
	 * @return
	 */
	public static List<FeishuUser> getFeishuBaseEmployees() {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return getFeishuBaseEmployees(accessToken);
	}

	/**
	 * 飞书（费控）查询自定义纬度列表
	 *
	 * @param accessToken
	 * @return
	 */
	public static List<SpendCustomField> getSpendCustomFields(String accessToken, String fieldCode) {
		List<SpendCustomField> spendCustomFields = new ArrayList<>();
		String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/spend/v1/custom_fields/" + fieldCode)
				.header("Authorization", "Bearer " + accessToken)
				.execute()
				.body();
		JSONObject jsonObject = JSON.parseObject(resultStr);
		if (jsonObject != null && "0".equals(jsonObject.getString("code"))) {
			JSONObject data = (JSONObject) jsonObject.get("data");
			JSONArray items = (JSONArray) data.get("value_list");
			for (int i = 0; i < items.size(); i++) {
				// 构造飞书用户
				SpendCustomField customField = new SpendCustomField();
				customField.setCode(items.getJSONObject(i).getString("code"));
				customField.setNameI18n(items.getJSONObject(i).getString("name_i18n"));
				customField.setIsValid(items.getJSONObject(i).getBoolean("is_valid"));
				spendCustomFields.add(customField);
			}
		}
		// 解析此格式：{\"zh\":\"61007030\"}
		for (SpendCustomField spendCustomField : spendCustomFields) {
			spendCustomField.setNameI18n(StringUtil.getZhCustomFie(spendCustomField.getNameI18n()));
		}
		// 过滤有效的
		spendCustomFields = spendCustomFields.stream().filter(SpendCustomField::getIsValid).collect(Collectors.toList());
		return spendCustomFields;
	}

	/**
	 * 飞书（费控）查询自定义纬度列表
	 *
	 * @return
	 */
	public static List<SpendCustomField> getSpendCustomFields(String fieldCode) {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return getSpendCustomFields(accessToken, fieldCode);
	}

	/**
	 * 遍历支付池
	 *
	 * @param accessToken
	 * @return
	 */
	public static List<FeishuPaypool> getPaypools(String accessToken, String formCode) {
		List<FeishuPaypool> feishuPaypoolList = new ArrayList<>();
		Map<String, Object> param = new HashMap<>();
		JSONObject bodyObject = new JSONObject();
		bodyObject.put("page_size", "100");
		if (formCode != null) {
			bodyObject.put("vendor_form_header_code_from", formCode);
			bodyObject.put("vendor_form_header_code_to", formCode);
		}
		while (true) {
			String resultStr = HttpRequest.post("https://open.feishu.cn/open-apis/spend/v1/paypools2/scroll?page_token="
							+ (param.get("page_token") == null ? "" : param.get("page_token")))
					.header("Authorization", "Bearer " + accessToken)
//					.form(param)
					.body(bodyObject.toJSONString())
					.execute()
					.body();
			JSONObject jsonObject = JSON.parseObject(resultStr);
			if (jsonObject != null && "0".equals(jsonObject.getString("code"))) {
				JSONObject data = (JSONObject) jsonObject.get("data");
				JSONArray items = (JSONArray) data.get("items");
				for (int i = 0; i < items.size(); i++) {
					FeishuPaypool paypool = items.getJSONObject(i).toJavaObject(FeishuPaypool.class);
					feishuPaypoolList.add(paypool);
				}

				if (data.getBoolean("has_more")) {
					param.put("page_token", data.getString("page_token"));
				} else {
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return feishuPaypoolList;
	}

	/**
	 * 飞书（标准版）获取花名册信息
	 *
	 * @return
	 */
	public static List<FeishuPaypool> getPaypools(String formCode) {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return getPaypools(accessToken, formCode);
	}

	/**
	 * 更新支付池支付状态
	 *
	 * @param accessToken
	 * @param id
	 * @param accountant
	 * @return
	 */
	public static boolean updatePaypool(String accessToken, String id, String accountant) {
		JSONObject bodyObject = new JSONObject();
		bodyObject.put("payment_status_code", "A5");
		bodyObject.put("modifier", accountant);
		String resultStr = HttpRequest.patch("https://open.feishu.cn/open-apis/spend/v1/paypools2/" + id)
				.header("Authorization", "Bearer " + accessToken)
				.body(bodyObject.toJSONString())
				.execute()
				.body();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println(resultStr);
		JSONObject jsonObject = JSON.parseObject(resultStr);
		if (jsonObject != null && jsonObject.getInteger("code") == 0) {
			return true;
		} else {
			log.info("更新支付池支付状态失败: {}", resultStr);
			return false;
		}
	}

	/**
	 * 更新支付池支付状态
	 *
	 * @param id
	 * @param accountant
	 * @return
	 */
	public static boolean updatePaypool(String id, String accountant) {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return updatePaypool(accessToken, id, accountant);
	}
}
