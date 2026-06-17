package com.shopping.wx.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.shopping.base.utils.Utils;
import com.shopping.wx.token.authorization.manager.JwtTokenUtils;
import com.shopping.wx.token.config.ResultStatus;
import com.shopping.wx.token.model.CheckResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * 小程序鉴权过滤器。
 * 白名单前缀(无需 Token)之外的请求, 必须在请求头 Token 中携带合法 JWT。
 */
@Component
public class SecurityFilter implements Filter {

	protected Logger logger = Logger.getLogger(this.getClass());

	/** 无需登录态的前缀: 业务接口/后台/微信回调/Druid监控 */
	private static final Set<String> GreenUrlSet = new HashSet<String>();

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		GreenUrlSet.add("/bc");
		GreenUrlSet.add("/admin");
		GreenUrlSet.add("/wx");
		GreenUrlSet.add("/druid");
	}

	@Override
	public void doFilter(ServletRequest srequest, ServletResponse sresponse, FilterChain filterChain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) srequest;
		String uri = request.getRequestURI();
		if (containsSuffix(uri) || greenUrlCheck(uri) || containsKey(uri) || containsSwagger(uri)) {
			filterChain.doFilter(srequest, sresponse);
			return;
		}
		String authHeader = request.getHeader("Token");
		if (StringUtils.isEmpty(authHeader)) {
			logger.info("Signature verification does not exist.");
			print(sresponse, ResultStatus.JWT_ERRCODE_NULL.getCode(), ResultStatus.JWT_ERRCODE_NULL.getEgMessage());
			return;
		}
		// 验证JWT签名
		CheckResult checkResult = JwtTokenUtils.validateJWT(authHeader);
		if (checkResult.isSuccess()) {
			filterChain.doFilter(srequest, sresponse);
		} else {
			ResultStatus resultStatus = ResultStatus.getEnumByCode(checkResult.getErrCode());
			if (Utils.isNotEmpty(resultStatus)) {
				print(sresponse, checkResult.getErrCode(), resultStatus.getEgMessage());
			}
		}
	}

	public void print(ServletResponse servletResponse, Object errorCode, String errorMesg) throws IOException {
		JSONObject json = new JSONObject();
		json.put("error", errorCode);
		json.put("error_msg", errorMesg);
		servletResponse.getWriter().write(JSON.toJSONString(json));
	}

	/** 检查无需登录前缀 */
	private boolean greenUrlCheck(String url) {
		for (String greenUrl : GreenUrlSet) {
			if (url.startsWith(greenUrl)) {
				return true;
			}
		}
		return false;
	}

	/** 静态资源后缀放行 */
	private boolean containsSuffix(String url) {
		return url.endsWith(".js") || url.endsWith(".css") || url.endsWith(".jpg")
				|| url.endsWith(".gif") || url.endsWith(".png") || url.endsWith(".html")
				|| url.endsWith(".eot") || url.endsWith(".svg") || url.endsWith(".ttf")
				|| url.endsWith(".woff") || url.endsWith(".ico") || url.endsWith(".woff2");
	}

	/** 媒体资源放行 */
	private boolean containsKey(String url) {
		return url.contains("/media/");
	}

	/** swagger 文档放行 */
	private boolean containsSwagger(String url) {
		return url.contains("swagger") || url.contains("/v2/api-docs");
	}

	@Override
	public void destroy() {
	}
}
