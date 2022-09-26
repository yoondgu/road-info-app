package app;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import response.ResponseData;
import service.RoadService;
import vo.Coord;

public class RoadApplication {

	public static void main(String[] args) {
		// CSV파일에서 특정 거리에 따라 요청할 좌표 선별하여 리스트에 저장
		List<Coord> rqCoords = RoadService.selectLocations(0.05); // 50m 단위로 조회
		
		// TODO AWS Lambda 통해 값 반환
		ResponseData responseData = RoadService.getRoadInfo(rqCoords, 100);
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String response = gson.toJson(responseData);
		System.out.println(response);
		
	}
}
