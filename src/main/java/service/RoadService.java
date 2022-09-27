package service;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dto.MatchedPoint;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import response.ListResponseData;
import response.ResponseData;
import util.CSVUtil;
import util.HaversineDistance;
import vo.Coord;

public class RoadService {

	/**
	 * skopenapi에서 제공하는 원도로등급 인덱스 별 명칭
	 * 원도로등급 (0:고속국도, 1:도시고속화도로, 2:국도, 3;국가지원지방도, 4:지방도, 5:주요도로 1, 6:주요도로 2, 7:주요도로 3, 8:기타도로 1, 9:이면도로, 10:페리항로, 11:단지내도로, 12 :이면도로 2(세도로))
	 */
	final static String[] roadCategories = {"고속국도", "도시고속화도로", "국도", "국가지원지방도", "지방도", "주요도로 1", "주요도로 2", "주요도로 3", "기타도로 1", "이면도로", "페리항로", "단지내도로", "이면도로 2(세도로)"};
	
	public static ResponseData getRoadInfo(List<Coord> requestCoords, int maxCount) {
		// matchToRoads api는 도로정보를 조회하기 위한 좌표를 최대 100/500/1000개까지 지원하고, 이에 따라 요청 url이 달라진다.
		if (maxCount == 0) {
			maxCount = 100;
		}
		if (maxCount != 100 && maxCount != 500 && maxCount != 1000) {
			return ResponseData.create(false, "요청좌표의 최대 개수가 올바르지 않습니다. (100,500,1000 중 선택할 것)");
		}
		if (requestCoords.size() > maxCount) {
			return ResponseData.create(false, "요청좌표의 개수가 지원하는 최대값을 초과합니다.");
		}
		
		// 파라미터로 받은 좌표 리스트를 쿼리스트링으로 변환
		StringJoiner sj = new StringJoiner("|");
		for (int i = 0; i < requestCoords.size(); i++) {
			Coord coord = requestCoords.get(i);
			sj.add(coord.getLongitude() + "," + coord.getLatitude());
		}
		String coordsString = sj.toString();
		
		// SK open API를 통해 해당 좌표들에 매칭되는 도로 정보 획득
		// TODO 통신오류 / 시간 지연 문제 발생 시 예외처리
		try {
			OkHttpClient client = new OkHttpClient();
			MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
			RequestBody body = RequestBody.create("responseType=1&coords=" + coordsString, mediaType);
			Request request = new Request.Builder()
					.url("https://apis.openapi.sk.com/tmap/road/matchToRoads" + (maxCount == 100 ? "" : maxCount) + "?version=1")
					.post(body)
					.addHeader("accept", "application/json")
					.addHeader("Content-Type", "application/x-www-form-urlencoded")
					.addHeader("appKey", "l7xx1317e6cad24d4f0d8048aa7336e5623b")
					.build();
			
			Response response = client.newCall(request).execute();
			// TODO 응답 에러 발생시 ResponseData객체에 에러코드, 메시지 담을 것
			System.out.println(response.code() + " " + response.message());
			
			// 응답객체의 내용을 JsonObject, JsonArray 객체로 변환하여 필요한 정보 획득하기
			JsonElement element = JsonParser.parseString(response.body().string());
			JsonObject rootob = element.getAsJsonObject().get("resultData").getAsJsonObject();
			Object matchedPointsObj = rootob.get("matchedPoints");
			if (matchedPointsObj == null) {
				return ResponseData.create(false, "도로정보가 존재하지 않습니다.");
			}
			JsonArray matchedPoints = rootob.get("matchedPoints").getAsJsonArray();
			
			// JsonArray 객체의 정보를 조작하여 반환할 리스트 생성해서 응답객체로 반환하고 CSV파일에 저장하기
			List<MatchedPoint> result = matchedPointsToResultList(requestCoords, matchedPoints);
			// TODO 읽기/쓰기 중 무엇이든 오류가 났을때 트랜잭션 처리
			insertLocationInfos(result);
			
			return ListResponseData.create(result);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseData.create(false, "오류가 발생했습니다.");
		}
	}
	
	public static List<MatchedPoint> matchedPointsToResultList(List<Coord> requestCoords, JsonArray matchedPoints) {
		List<MatchedPoint> resultData = new ArrayList<>();

		// 요청좌표 중 매칭되는 지점으로 객체를 만들어 리스트에 저장할 때, 해당 요청좌표의 인덱스가 i일 때 matched[i]에 true를 저장한다.
		boolean[] matched = new boolean[requestCoords.size()];
		for (int i = 0; i < matchedPoints.size(); i++) {
			JsonObject obj = matchedPoints.get(i).getAsJsonObject();
			MatchedPoint matchedPoint = new MatchedPoint();

			// 보간점인지 아닌지 sourceIndex null 여부로 확인 (보간점일 경우 저장x)
			JsonElement sourceIndexEl = obj.get("sourceIndex");
			if (sourceIndexEl != null) {
				int sourceIndex = sourceIndexEl.getAsInt();
				// 해당 matchedPoint가 보간점이 아니고 matched[i]가 false인 경우에만 리스트에 저장
				// 이미 true로 저장된 지점에 대해서는, 요청좌표에 대해 1개 이상의 값이 반환되는 것이므로 객체를 만들지 않고 넘어간다.
				if (!matched[sourceIndex]) {
					matchedPoint.setSourceLocation(requestCoords.get(sourceIndex));
					matchedPoint.setIdxName(obj.get("idxName").getAsString());
					matchedPoint.setLinkId(obj.get("linkId").getAsString());
					matchedPoint.setSpeed(obj.get("speed").getAsString());
					matchedPoint.setRoadCategoryName(roadCategories[obj.get("roadCategory").getAsInt()]);
					resultData.add(matchedPoint);
					matched[sourceIndex] = true;
				}
			}
		}
		return resultData;
	}
	
	public static List<Coord> selectLocations(double unitDistance) {
		List<List<String>> csvList = CSVUtil.read();
		List<Coord> coords = new ArrayList<>();
		
		double lat1 = 0;
		double lon1 = 0;
		for (int i = 1; i < csvList.size(); i++) {
			List<String> aLine = csvList.get(i);
			
			// 좌표간의 거리를 계산해서 Nm 간격의 좌표만 리스트에 담는다.
			// 단, 가장 첫번째 좌표와 마지막 좌표는 무조건 담는다.
			if (i == 1 || i == csvList.size()-1) {
				lat1 = Double.parseDouble(aLine.get(2));
				lon1 = Double.parseDouble(aLine.get(3));
				coords.add(new Coord(lat1, lon1, i-1));
			} else {
				double lat2 = Double.parseDouble(aLine.get(2));
				double lon2 = Double.parseDouble(aLine.get(3));
				double distance = new HaversineDistance(lat1, lon1, lat2, lon2).getDistance();
				if (unitDistance < distance) {
					coords.add(new Coord(lat2, lon2, i-1));
					lat1 = lat2;
					lon1 = lon2;
				}
			}
		}
		// TODO 획득한 좌표개수가 최대 지원수를 넘을 경우 처리하기
		return coords;
	}

	
	public static void insertLocationInfos(List<MatchedPoint> points) {
		// csv파일의 특정 좌표에 해당하는 행에 속도제한 정보를 저장한다.
		CSVUtil.write(points);
	}
}
