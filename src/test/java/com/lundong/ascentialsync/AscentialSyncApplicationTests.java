package com.lundong.ascentialsync;

import com.lundong.ascentialsync.entity.CustomAttr;
import com.lundong.ascentialsync.entity.FeishuUser;
import com.lundong.ascentialsync.util.SignUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

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

}
