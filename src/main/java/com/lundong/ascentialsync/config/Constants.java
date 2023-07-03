package com.lundong.ascentialsync.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author RawChen
 * @date 2023-03-02 14:52
 */
@Component
@Data
@ConfigurationProperties(prefix = "custom")
public class Constants {

	// 飞书自建应用 App ID
	public String APP_ID_FEISHU;

	// 飞书自建应用 App Secret
	public String APP_SECRET_FEISHU;

	public String SFTP_HOST;

	public String SFTP_USER_ID;

	public String SFTP_PASSWORD;

	public String CHAT_ID_ARG;

	public String USER_ID_ARG;

}
