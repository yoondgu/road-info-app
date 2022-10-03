package app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import response.ResponseData;
import service.RoadService;

public class RoadApplication {

	public static void main(String[] args) {
		// TODO AWS Lambda 통해 값 반환  (나중에)
		// TODO 요청한 API에서 좌표 선택 기준, idxName 등 정보 문의해서 도로정보 자세히 얻기
		// TODO 경로검색 등 다른 방식으로 도로정보 획득해보기
		ResponseData responseData = RoadService.getRoadInfo(0.02); // km단위로 단위거리 입력(ex: 0.05 = 최소 50m 간격의 좌표들을 기준으로 조회)
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String response = gson.toJson(responseData);
		System.out.println(response);
	}
}
