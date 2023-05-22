package com.lundong.ascentialsync.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.lundong.ascentialsync.config.Constants;
import com.lundong.ascentialsync.entity.FeishuDept;
import com.lundong.ascentialsync.entity.FeishuSpendVoucher;
import com.lundong.ascentialsync.entity.FeishuUser;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.HttpCookie;
import java.util.*;

/**
 * @author RawChen
 * @date 2023-03-08 18:37
 */
public class SignUtil {

	/**
	 * SAP系统自定义签名规则
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
				System.out.println("批量插入失败：" + resultObject.getString("msg"));
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
		System.out.println("===开始批量删除 " + tableId + "===");
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
						System.out.println("批量删除失败：" + resultObject.getString("msg"));
					}
				}
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		System.out.println("===批量删除完成===");
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
	public static List<FeishuSpendVoucher>spendVouchers(String accessToken) {
		List<FeishuSpendVoucher> vouchers = new ArrayList<>();
		Map<String, Object> param = new HashMap<>();
		while (true) {
			param.put("page_size", 20);
			String resultStr = HttpRequest.get("https://open.feishu.cn/open-apis/spend/v1/vouchers/scroll")
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
				FeishuSpendVoucher feishuDept = items.getJSONObject(i).toJavaObject(FeishuSpendVoucher.class);
				vouchers.add(feishuDept);
			}

			if ((boolean) data.get("has_more")) {
				param.put("page_token", data.getString("page_token"));
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
	public static List<FeishuSpendVoucher>spendVouchers() {
		String accessToken = getAccessToken(Constants.APP_ID_FEISHU, Constants.APP_SECRET_FEISHU);
		return spendVouchers(accessToken);
	}
}
