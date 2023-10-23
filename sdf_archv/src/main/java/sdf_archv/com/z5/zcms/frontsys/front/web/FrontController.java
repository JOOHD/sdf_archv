package com.z5.zcms.frontsys.front.web;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import com.google.gson.Gson;
import com.z5.zcms.admsys.board.domain.FrontBoardVo;
import com.z5.zcms.admsys.board.service.FrontBoardService;
import com.z5.zcms.admsys.user.service.ZUserService;
import com.z5.zcms.frontsys.archv.domain.ArchvVO;
import com.z5.zcms.frontsys.front.dao.FrontDAO;
import com.z5.zcms.frontsys.front.service.FrontMainService;
import com.z5.zcms.frontsys.front.service.FrontService;
import com.z5.zcms.util.DataTable;
import com.z5.zcms.util.IpUtil;
import com.z5.zcms.util.ZPrint;
import com.z5.zcms.view.AjaxJsonView;

@Controller
public class FrontController extends HttpServlet {

	private static final Logger log = LoggerFactory.getLogger(FrontController.class);

	@RequestMapping(value = "/archive/search.html", method = RequestMethod.POST)
	public ModelAndView archiveSearch(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ModelAndView mav = new ModelAndView(new AjaxJsonView());

		try {
			DataTable input = new DataTable(req);
			// String keywordTest = req.getParameter("keyword");

			// 필수로 사용해야 되는 데이터
			// Keyword
			String keyword = input.get("keyword");

			// 현재 페이지
			String currentPage = req.getParameter("current_page");

			// 대분류 카테고리
			String categoryCd1 = req.getParameter("category_cd1");

			// 중분류 카테고리
			String categoryCd2 = req.getParameter("category_cd2");

			// 실제 카테고리
			String categoryType = req.getParameter("category_type");

			// 메타 유형
			String metaId = req.getParameter("meta_id");

			// 메타 벨류
			String metaVal = req.getParameter("meta_val");
			if (StringUtils.isEmpty(currentPage)) {
				currentPage = "1";
			}
			// 1. parameter 조합

			StringBuffer sb = new StringBuffer();

			// sb.append(uri);
			// 파라미터 값 넣기

			sb.append("currentPage=" + URLEncoder.encode(currentPage, "utf-8")); // currentPage : 출력할 현재 페이지
			sb.append("&");
			sb.append("searchType=" + URLEncoder.encode("title", "utf-8")); // searchType : 검색 조건
			sb.append("&");
			sb.append("searchText=" + URLEncoder.encode(keyword, "utf-8")); // searchText : 검색 키워드
			sb.append("&");
			sb.append("blockCount=" + URLEncoder.encode("8", "utf-8")); // blockCount : 콘텐츠 출력 개수
			if (StringUtils.isNotEmpty(categoryCd1)) {
				sb.append("&");
				sb.append("categoryCd1=" + URLEncoder.encode(categoryCd1, "utf-8"));
			}
			if (StringUtils.isNotEmpty(categoryCd2) && StringUtils.isNotEmpty(categoryType)) {
				sb.append("&");
				sb.append("categoryCd2=" + URLEncoder.encode(categoryCd2, "utf-8"));
				sb.append("&");
				sb.append("categoryType=" + URLEncoder.encode(categoryType, "utf-8"));
			}
			if (StringUtils.isNotEmpty(metaVal)) {
				sb.append("&");
				sb.append("metaId=" + URLEncoder.encode(metaId, "utf-8"));
				sb.append("&");
				sb.append("metaVal=" + URLEncoder.encode(metaVal, "utf-8"));
			}

			// 2. url 보내기

			// api List url
			String uri = "https://seouldesign.or.kr/tigenweb/api/apiContentsList.do";
			// String currentPage = URLEncoder.encode("1", "utf-8");

			URL url = null;
			HttpURLConnection conn = null;
			url = new URL(uri);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			DataOutputStream out = null;
			boolean isOk = false;
			try {
				out = new DataOutputStream(conn.getOutputStream());

				System.out.println("parameter ::::: " + sb.toString());

				out.writeBytes(sb.toString());
				out.flush();
				isOk = true;
			} finally {
				if (out != null)
					out.close();
				isOk = false;
			}

			StringBuffer outBuffer = new StringBuffer();

			InputStream is = conn.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"));
			String line = null;
			while ((line = br.readLine()) != null) {
				outBuffer.append(line);
			}

			is.close();
			br.close();

			// Gson gson = new Gson();
			// Type mapType = new TypeToken<HashMap<String, Object>>() {}.getType();
			// Map<String, Object> resultMap = gson.fromJson(outBuffer.toString(), mapType);
			// map = resultMap;

			res.setCharacterEncoding("utf-8");

			System.out.println("result ::::: " + outBuffer.toString());
			String ajaxJson = outBuffer.toString();

			mav.addObject("ajaxJson", ajaxJson);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return mav;
	}

	@RequestMapping(value = "/archive/searchDetail.html", method = RequestMethod.POST)
	public ModelAndView archiveSearchDetail(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ModelAndView mav = new ModelAndView(new AjaxJsonView());

		Map<String, String> resultMap = new HashMap<String, String>();

		// 성공/실패 status
		String status = "success";
		String failMessage = "오류가 발생했습니다.";

		// 1. 파라미터 받기
		// 1) contentsId(필수)(String 콘텐츠 아이디)
		String contentsId = req.getParameter("contents_id");

		// 2) encType(필수)(String 컨텐츠 타입(video : 비디오, image : 이미지, audio : 오디오, etc : 문서,
		// all : 전체)
		String encType = req.getParameter("enc_type");

		// 3) currentPage(필수)(int 출력할 현재 페이지) --> encode를 위해 String으로 지정
		String currentPage = req.getParameter("current_page");

		// 4) blockCount(선택)(int 콘텐츠 출력 개수(없을시, 5개 출력) --> encode를 위해 String으로 지정
		String blockCount = req.getParameter("block_count");

		// 2. null처리
		// null처리중 return 처리 하기
		if (StringUtils.isEmpty(contentsId)) {
			// 오류 메세지 처리
			return null;
		}
		if (StringUtils.isEmpty(encType)) {
			// 오류 메세지 처리
			return null;
		}
		if (StringUtils.isEmpty(currentPage)) {
			currentPage = "1";
		}
		if (StringUtils.isEmpty(blockCount)) {
			// 오류 메세지 처리
			return null;
		}

		String contentsIdParam = "contentsId=" + URLEncoder.encode(contentsId, "UTF-8");
		String encTypeIdParam = "encType=" + URLEncoder.encode(encType, "UTF-8");
		String currentPageParam = "currentPage=" + URLEncoder.encode(currentPage, "UTF-8");
		String blockCountIdParam = "blockCount=" + URLEncoder.encode(blockCount, "UTF-8");

		String uri = "https://seouldesign.or.kr/tigenweb/api/apiContentsDetail.do";

		uri += "?" + contentsIdParam;
		uri += "&" + encTypeIdParam;
		uri += "&" + currentPageParam;
		uri += "&" + blockCountIdParam;

		// 3. Api 호출
		// 1) parameter 담기
		// http 통신을 하기위한 객체 선언 실시
		URL url = null;
		HttpURLConnection conn = null;

		// http 통신 요청 후 응답 받은 데이터를 담기 위한 변수
		String responseData = "";
		BufferedReader br = null;
		StringBuffer sb = null;

		// 메소드 호출 결과값을 반환하기 위한 변수
		String returnData = "";

		try {
			// 파라미터로 들어온 url을 사용해 connection 실시
			url = new URL(uri);
			conn = (HttpURLConnection) url.openConnection();

			// http 요청에 필요한 타입 정의 실시
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestMethod("GET");

			// http 요청 실시
			conn.connect();

			// http 요청 후 응답 받은 데이터를 버퍼에 쌓는다
			br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			sb = new StringBuffer();
			while ((responseData = br.readLine()) != null) {
				sb.append(responseData); // StringBuffer에 응답받은 데이터 순차적으로 저장 실시
			}

			// 메소드 호출 완료 시 반환하는 변수에 버퍼 데이터 삽입 실시
			returnData = sb.toString();

			// http 요청 응답 코드 확인 실시
			String responseCode = String.valueOf(conn.getResponseCode());
			System.out.println("http 응답 코드 : " + responseCode);
			System.out.println("http 응답 데이터 : " + returnData);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// http 요청 및 응답 완료 후 BufferedReader를 닫아줍니다
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		mav.addObject("ajaxJson", returnData);
		// String ajaxJson = "{\"contentsDetail\": {\"contentsTitle\":
		// \"TEST\",\"contentsId\": \"4\",\"regUserId\": \"tgadmin\",\"regUserName\":
		// \"2022-09-29 14:36:04\",\"descr\": \"테스트 게시글입니다.\",\"downYn\": \"테스트
		// 게시글입니다.\",\"categoryCd\": \"2\",\"tagName\": \"태그입니다\",\"reprImgFileUrl\":
		// \"/upload/encoding/video/2022/09/130dd6b1ef3c4c54a0e00e3aa9f3c97f_thumb1.png\",\"majorContentsYn\":
		// \"N\",\"viewCnt\": \"12\"}}";
		// resultMap.put("ajaxJson", ajaxJson);
		// mav.addObject("status", status);
		// mav.addObject("failMessage ", failMessage);
		// mav.addObject("ajaxJson", ajaxJson);

		return mav;
	}

	@RequestMapping(value = "/archive/categoryList.html", method = RequestMethod.POST)
	public ModelAndView archiveCategoryList(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ModelAndView mav = new ModelAndView(new AjaxJsonView());

		// 1. 파라미터 받기
		// 1) upSeq(선택)
		String upSeq = req.getParameter("up_seq");

		// 3)필요 uri조합
		String uri = "https://seouldesign.or.kr/tigenweb/api/categoryList.do";

		// 2. null처리
		// null처리중 return 처리 하기
		if (StringUtils.isNotEmpty(upSeq)) {
			String upSeqParam = "upSeq=" + URLEncoder.encode(upSeq, "UTF-8");
			uri += "?" + upSeqParam;
		}

		// 3. Api 호출
		// 1) parameter 담기
		// http 통신을 하기위한 객체 선언 실시
		URL url = null;
		HttpURLConnection conn = null;

		// http 통신 요청 후 응답 받은 데이터를 담기 위한 변수
		String responseData = "";
		BufferedReader br = null;
		StringBuffer sb = null;

		// 메소드 호출 결과값을 반환하기 위한 변수
		String returnData = "";

		try {
			// 파라미터로 들어온 url을 사용해 connection 실시
			url = new URL(uri);
			conn = (HttpURLConnection) url.openConnection();

			// http 요청에 필요한 타입 정의 실시
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestMethod("GET");

			// http 요청 실시
			conn.connect();

			// http 요청 후 응답 받은 데이터를 버퍼에 쌓는다
			br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			sb = new StringBuffer();
			while ((responseData = br.readLine()) != null) {
				sb.append(responseData); // StringBuffer에 응답받은 데이터 순차적으로 저장 실시
			}

			// 메소드 호출 완료 시 반환하는 변수에 버퍼 데이터 삽입 실시
			returnData = sb.toString();

			// http 요청 응답 코드 확인 실시
			String responseCode = String.valueOf(conn.getResponseCode());
			System.out.println("http 응답 코드 : " + responseCode);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// http 요청 및 응답 완료 후 BufferedReader를 닫아줍니다
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// 테스트
		// String returnData =
		// "{\"categoryList\":[{\"categorySeq\":2,\"categoryName\":\"홈\",\"categoryUpSeq\":1},{\"categorySeq\":3,\"categoryName\":\"공지사항\",\"categoryUpSeq\":1},{\"categorySeq\":6,\"categoryName\":\"인기
		// 콘텐츠\",\"categoryUpSeq\":1},{\"categorySeq\":7,\"categoryName\":\"주요
		// 콘텐츠\",\"categoryUpSeq\":1},{\"categorySeq\":8,\"categoryName\":\"최신
		// 콘텐츠\",\"categoryUpSeq\":1},{\"categorySeq\":9,\"categoryName\":\"연도별분류\",\"categoryUpSeq\":1},{\"categorySeq\":10,\"categoryName\":\"2008\",\"categoryUpSeq\":9},{\"categorySeq\":11,\"categoryName\":\"2009\",\"categoryUpSeq\":9},{\"categorySeq\":12,\"categoryName\":\"2010\",\"categoryUpSeq\":9},{\"categorySeq\":13,\"categoryName\":\"2011\",\"categoryUpSeq\":9},{\"categorySeq\":14,\"categoryName\":\"2012\",\"categoryUpSeq\":9},{\"categorySeq\":15,\"categoryName\":\"2013\",\"categoryUpSeq\":9},{\"categorySeq\":16,\"categoryName\":\"2014\",\"categoryUpSeq\":9},{\"categorySeq\":17,\"categoryName\":\"2015\",\"categoryUpSeq\":9},{\"categorySeq\":18,\"categoryName\":\"2016\",\"categoryUpSeq\":9},{\"categorySeq\":19,\"categoryName\":\"2017\",\"categoryUpSeq\":9},{\"categorySeq\":20,\"categoryName\":\"2018\",\"categoryUpSeq\":9},{\"categorySeq\":21,\"categoryName\":\"2019\",\"categoryUpSeq\":9},{\"categorySeq\":22,\"categoryName\":\"2020\",\"categoryUpSeq\":9},{\"categorySeq\":23,\"categoryName\":\"2021\",\"categoryUpSeq\":9},{\"categorySeq\":24,\"categoryName\":\"2022\",\"categoryUpSeq\":9},{\"categorySeq\":26,\"categoryName\":\"디자인사업분류\",\"categoryUpSeq\":1},{\"categorySeq\":27,\"categoryName\":\"디자인문화확산\",\"categoryUpSeq\":26},{\"categorySeq\":28,\"categoryName\":\"디자인산업진흥\",\"categoryUpSeq\":26},{\"categorySeq\":29,\"categoryName\":\"디자인공공성강화\",\"categoryUpSeq\":26},{\"categorySeq\":30,\"categoryName\":\"재단일반\",\"categoryUpSeq\":26},{\"categorySeq\":31,\"categoryName\":\"전시\",\"categoryUpSeq\":27},{\"categorySeq\":32,\"categoryName\":\"행사\",\"categoryUpSeq\":27},{\"categorySeq\":33,\"categoryName\":\"포럼\",\"categoryUpSeq\":27},{\"categorySeq\":34,\"categoryName\":\"네트워크(도시/기관/기업)\",\"categoryUpSeq\":27},{\"categorySeq\":35,\"categoryName\":\"서울디자인\",\"categoryUpSeq\":28},{\"categorySeq\":36,\"categoryName\":\"DDP디자인페어\",\"categoryUpSeq\":28},{\"categorySeq\":37,\"categoryName\":\"컨설팅\",\"categoryUpSeq\":28},{\"categorySeq\":38,\"categoryName\":\"개발\",\"categoryUpSeq\":28},{\"categorySeq\":39,\"categoryName\":\"창업\",\"categoryUpSeq\":28},{\"categorySeq\":40,\"categoryName\":\"마케팅\",\"categoryUpSeq\":28},{\"categorySeq\":41,\"categoryName\":\"디자인어워드\",\"categoryUpSeq\":29},{\"categorySeq\":42,\"categoryName\":\"연구\",\"categoryUpSeq\":29},{\"categorySeq\":43,\"categoryName\":\"교육\",\"categoryUpSeq\":29},{\"categorySeq\":44,\"categoryName\":\"출판\",\"categoryUpSeq\":29},{\"categorySeq\":45,\"categoryName\":\"인물/컬렉션\",\"categoryUpSeq\":29},{\"categorySeq\":46,\"categoryName\":\"포럼/세미나/디자인운동\",\"categoryUpSeq\":29},{\"categorySeq\":47,\"categoryName\":\"주요연혁\",\"categoryUpSeq\":30},{\"categorySeq\":48,\"categoryName\":\"DDP\",\"categoryUpSeq\":30}],\"totalCount\":44}";
		// String returnData = "{\"categoryList\": [{\"categorySeq\":
		// \"1\",\"categoryName\": \"디자인문화확산\",\"parentsNo\": \"0\"},{\"categorySeq\":
		// \"2\",\"categoryName\": \"디자인산업진흥\",\"parentsNo\": \"0\"},{\"categorySeq\":
		// \"3\",\"categoryName\": \"디자인공공성강화\",\"parentsNo\": \"0\"},{\"categorySeq\":
		// \"4\",\"categoryName\": \"재단일반\",\"parentsNo\": \"0\"},{\"categorySeq\":
		// \"5\",\"categoryName\": \"재단\",\"parentsNo\": \"2\"},{\"categorySeq\":
		// \"6\",\"categoryName\": \"특별\",\"parentsNo\": \"2\"},{\"categorySeq\":
		// \"7\",\"categoryName\": \"협력\",\"parentsNo\": \"2\"},{\"categorySeq\":
		// \"8\",\"categoryName\": \"디지털\",\"parentsNo\": \"3\"},{\"categorySeq\":
		// \"9\",\"categoryName\": \"돼지털\",\"parentsNo\": \"3\"},{\"categorySeq\":
		// \"10\",\"categoryName\": \"별똥별\",\"parentsNo\": \"3\"},{\"categorySeq\":
		// \"11\",\"categoryName\": \"서울라이트\",\"parentsNo\": \"3\"},{\"categorySeq\":
		// \"12\",\"categoryName\": \"오우예아\",\"parentsNo\": \"4\"}]}";

		mav.addObject("ajaxJson", returnData);

		return mav;
	}

	@RequestMapping(value = "/archive/metaList.html", method = RequestMethod.POST)
	public ModelAndView archiveMetaDataList(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ModelAndView mav = new ModelAndView(new AjaxJsonView());

		// 1. 파라미터 받기
		// 1) 메타데이터 ID(선택)(String 콘텐츠 아이디)
		String metaId = req.getParameter("meta_id");

		// 2) 메타데이터 이름 ( 사용자에게 노출되는 항목 )
		String metaNm = req.getParameter("metaNm");

		// 3)필요 uri조합
		String uri = "https://seouldesign.or.kr/tigenweb/api/metaList.do";

		// 2. null처리
		// null처리중 return 처리 하기
		boolean metaIdEmpty = StringUtils.isEmpty(metaId) ? Boolean.TRUE : Boolean.FALSE;
		boolean metaIdNotEmpty = !metaIdEmpty;

		boolean metaNmEmpty = StringUtils.isEmpty(metaNm) ? Boolean.TRUE : Boolean.FALSE;
		boolean metaNmNotEmpty = !metaNmEmpty;

		if (metaIdNotEmpty) {
			String metaIdParam = "metaId=" + URLEncoder.encode(metaId, "UTF-8");
			uri += "?" + metaIdParam;
		}
		if (metaNmNotEmpty) {
			if (metaIdEmpty) {
				uri += "?";
			} else {
				uri += "&";
			}
			String metaNmParam = "metaNm=" + URLEncoder.encode(metaNm, "UTF-8");
			uri += metaNmParam;
		}

		// 3. Api 호출
		// 1) parameter 담기
		// http 통신을 하기위한 객체 선언 실시
		URL url = null;
		HttpURLConnection conn = null;

		// http 통신 요청 후 응답 받은 데이터를 담기 위한 변수
		String responseData = "";
		BufferedReader br = null;
		StringBuffer sb = null;

		// 메소드 호출 결과값을 반환하기 위한 변수
		String returnData = "";

		try {
			// 파라미터로 들어온 url을 사용해 connection 실시
			url = new URL(uri);
			conn = (HttpURLConnection) url.openConnection();

			// http 요청에 필요한 타입 정의 실시
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestMethod("GET");

			// http 요청 실시
			conn.connect();

			// http 요청 후 응답 받은 데이터를 버퍼에 쌓는다
			br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			sb = new StringBuffer();
			while ((responseData = br.readLine()) != null) {
				sb.append(responseData); // StringBuffer에 응답받은 데이터 순차적으로 저장 실시
			}

			// 메소드 호출 완료 시 반환하는 변수에 버퍼 데이터 삽입 실시
			returnData = sb.toString();

			// http 요청 응답 코드 확인 실시
			String responseCode = String.valueOf(conn.getResponseCode());
			System.out.println("http 응답 코드 : " + responseCode);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// http 요청 및 응답 완료 후 BufferedReader를 닫아줍니다
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// 테스트
		// String returnData = "{\"metaList\": [{\"metaId\": \"4\",\"metaNm\":
		// \"보도자료(URL)\",\"metaYn\": \"Y\", \"itemList\" : \"\"},{\"metaId\":
		// \"5\",\"metaNm\": \"자료유형\",\"metaYn\": \"Y\", \"itemList\" : \"사진 || 영상 ||
		// 보도자료 || 포스터 || 기타홍보물 || 문서 || 소장품\"},{\"metaId\": \"6\",\"metaNm\":
		// \"출처\",\"metaYn\": \"Y\", \"itemList\" : \"\"},{\"metaId\": \"7\",\"metaNm\":
		// \"생산일(행사일)\",\"metaYn\": \"Y\", \"itemList\" : \"\"},{\"metaId\":
		// \"8\",\"metaNm\": \"장소\",\"metaYn\": \"Y\", \"itemList\" : \"\"},{\"metaId\":
		// \"9\",\"metaNm\": \"저작권(생산부서/창작자)\",\"metaYn\": \"Y\", \"itemList\" :
		// \"\"},{\"metaId\": \"10\",\"metaNm\": \"관리정보1\",\"metaYn\": \"Y\",
		// \"itemList\" : \"\"}]}";

		mav.addObject("ajaxJson", returnData);

		return mav;
	}

	@RequestMapping(value = "/archive/metaDetail.html", method = RequestMethod.POST)
	public ModelAndView archiveMetaData(HttpServletRequest req, HttpServletResponse res) throws Exception {
		ModelAndView mav = new ModelAndView(new AjaxJsonView());

		// 1. 파라미터 받기
		// 1) 메타데이터 ID(선택)(String 콘텐츠 아이디)
		String contentsId = req.getParameter("contents_id");

		// 3)필요 uri조합
		String uri = "https://seouldesign.or.kr/tigenweb/api/metaDetail.do";
		String contentsIdParam = "?contentsId=" + URLEncoder.encode(contentsId, "UTF-8");

		// 2. null처리
		// null처리중 return 처리 하기
		boolean contentsIdEmpty = StringUtils.isEmpty(contentsId) ? Boolean.TRUE : Boolean.FALSE;
		boolean contentsIdNotEmpty = !contentsIdEmpty;
		// 3. Api 호출
		// 1) parameter 담기
		uri += contentsIdParam;

		// http 통신을 하기위한 객체 선언 실시
		URL url = null;
		HttpURLConnection conn = null;

		// http 통신 요청 후 응답 받은 데이터를 담기 위한 변수
		String responseData = "";
		BufferedReader br = null;
		StringBuffer sb = null;

		// 메소드 호출 결과값을 반환하기 위한 변수
		String returnData = "";

		try {
			// 파라미터로 들어온 url을 사용해 connection 실시
			url = new URL(uri);
			conn = (HttpURLConnection) url.openConnection();

			// http 요청에 필요한 타입 정의 실시
			conn.setRequestProperty("Accept", "application/json");
			conn.setRequestMethod("GET");

			// http 요청 실시
			conn.connect();

			// http 요청 후 응답 받은 데이터를 버퍼에 쌓는다
			br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			sb = new StringBuffer();
			while ((responseData = br.readLine()) != null) {
				sb.append(responseData); // StringBuffer에 응답받은 데이터 순차적으로 저장 실시
			}

			// 메소드 호출 완료 시 반환하는 변수에 버퍼 데이터 삽입 실시
			returnData = sb.toString();

			// http 요청 응답 코드 확인 실시
			String responseCode = String.valueOf(conn.getResponseCode());
			System.out.println("http 응답 코드 : " + responseCode);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// http 요청 및 응답 완료 후 BufferedReader를 닫아줍니다
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// 테스트
		// String returnData = "";

		mav.addObject("ajaxJson", returnData);

		return mav;
	}
}
