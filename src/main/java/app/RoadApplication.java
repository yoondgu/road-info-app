package app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import response.ResponseData;
import service.RoadService;

public class RoadApplication {

	public static void main(String[] args) {
		// TODO AWS Lambda 통해 값 반환
		ResponseData responseData = RoadService.getRoadInfo(0.05, 100); // km단위로 단위거리 입력(ex: 최소 50m 간격의 좌표들을 기준으로 조회), 최대 지원하는 요청좌표 개수 100개
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String response = gson.toJson(responseData);
		System.out.println(response);
		
	}
}
