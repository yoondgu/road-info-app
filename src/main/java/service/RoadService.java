package service;

import java.io.FileNotFoundException;
import java.io.IOException;
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
	
	// 요청할 때마다 OkHttpClient를 새로 만들지 않도록 static 객체로 생성
	private static OkHttpClient client = new OkHttpClient();

	/**
	 * skopenapi에서 제공하는 원도로등급 인덱스 별 명칭
	 * 원도로등급 (0:고속국도, 1:도시고속화도로, 2:국도, 3;국가지원지방도, 4:지방도, 5:주요도로 1, 6:주요도로 2, 7:주요도로 3, 8:기타도로 1, 9:이면도로, 10:페리항로, 11:단지내도로, 12 :이면도로 2(세도로))
	 */
	private final static String[] roadCategories = {"고속국도", "도시고속화도로", "국도", "국가지원지방도", "지방도", "주요도로 1", "주요도로 2", "주요도로 3", "기타도로 1", "이면도로", "페리항로", "단지내도로", "이면도로 2(세도로)"};
	
	// matchToRoads api는 도로정보를 조회하기 위한 좌표를 최대 100/500/1000개까지 지원하고, 이에 따라 요청 url이 달라진다.
	// 지원 범위에 따라 비용이 다르므로 csv파일로 계산한 요청좌표 개수에 따라 최소비용의 maxCount를 자동으로 설정한다.
	public static ResponseData getRoadInfo(double unitDistance) {
		// TODO 통신,읽기/쓰기 작업 시 시간지연 오류 처리
		try {
			List<Coord> requestCoords = selectLocations(unitDistance);
			int maxCount = 100;
			int coordsSize = requestCoords.size();
			if (coordsSize > maxCount) {
				if (coordsSize > 1000) {
					return ResponseData.create(false, "요청좌표의 개수가 1000개를 초과합니다.");
				} else if (coordsSize > 500) {
					maxCount = 1000;
				} else {
					maxCount = 500;
				}
			}
			System.out.println("설정한 요청좌표 최대 지원개수: " + maxCount);
			
			// SK open API에 http 요청 보내기 : 특정 좌표 리스트에 매칭되는 도로 정보 리스트 요청
			// OkHttpClient 객체 생성
			
			MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
			RequestBody body = RequestBody.create("responseType=1&coords=" + coordsToString(requestCoords), mediaType);
			Request request = new Request.Builder()
					.url("https://apis.openapi.sk.com/tmap/road/matchToRoads" + (maxCount == 100 ? "" : maxCount) + "?version=1")
					.post(body)
					.addHeader("accept", "application/json")
					.addHeader("Content-Type", "application/x-www-form-urlencoded")
					.addHeader("appKey", "l7xx6e3d85ee83db468daaa1fcc76f50932d")
					.build();
			Response response = client.newCall(request).execute();
			
			String responseBodyString = response.body().string();
			String message = response.message();
			int errorCode = response.code();
			response.close(); // http 통신 스트림 닫기 (메모리 누수 방지)
			
			if (response.code() == 200) {
				// 정상적으로 응답받았을 경우, 응답객체의 내용을 JsonObject, JsonArray 객체로 변환하여 필요한 정보 획득하기
				JsonElement element = JsonParser.parseString(responseBodyString);
				JsonObject rootob = element.getAsJsonObject().get("resultData").getAsJsonObject();
				Object matchedPointsObj = rootob.get("matchedPoints");
				if (matchedPointsObj == null) {
					return ResponseData.create(false, "도로정보가 존재하지 않습니다.");
				}
				JsonArray matchedPoints = rootob.get("matchedPoints").getAsJsonArray();
				// JsonArray 객체의 정보를 조작하여 반환할 리스트 생성해서 응답객체로 반환하고 CSV파일에 저장하기
				List<MatchedPoint> result = matchedPointsToResultList(requestCoords, matchedPoints);
				insertLocationInfos(result);
				
				return ListResponseData.create(result);
			} else {
				return ResponseData.create(false, "SK open API 응답 오류: " + message, errorCode);
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseData.create(false, "서버 오류: " + e.getMessage());
		}
	}

	private static String coordsToString(List<Coord> coords) {
		// 파라미터로 받은 좌표 리스트를 SK open API 요청형식에 맞는 쿼리스트링으로 변환
		StringJoiner sj = new StringJoiner("|");
		for (int i = 0; i < coords.size(); i++) {
			Coord coord = coords.get(i);
			sj.add(coord.getLongitude() + "," + coord.getLatitude());
		}
		return sj.toString();
	}
	
	private static List<MatchedPoint> matchedPointsToResultList(List<Coord> requestCoords, JsonArray matchedPoints) {
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
	
	// 단위거리 정밀도 관련 : 자동차 속력은 일반적으로 60~80km/h 이므로 초당 16~22m 이동한다고 볼 수 있다. 따라서 앞 뒤 좌표간의 거리가 단위거리보다 크더라도 그 차이가 아주 크지 않으므로 별도 고려할 필요 x
	// 요청좌표 개수 관련 처리 : maxCount보다 반환값의 size가 클 경우 요청보내는 maxCount 설정을 바꾼다. (ex: 100이면 500이나 1000, 1000이면 예외처리) => getRoadInfo 메소드에서 처리
	private static List<Coord> selectLocations(double unitDistance) throws FileNotFoundException, IOException {
		List<List<String>> csvList = CSVUtil.read();
		List<Coord> coords = new ArrayList<>();
		double lat1 = 0;
		double lon1 = 0;
		for (int i = 1; i < csvList.size(); i++) {
			List<String> aLine = csvList.get(i);
			
			// 이전 순서 좌표와의 거리가 unitDistance(단위거리) 이상인 좌표만 리스트에 담는다.
			// 단, 가장 첫번째 좌표와 마지막 좌표는 무조건 담는다.
			if (i == 1 || i == csvList.size()-1) {
				lat1 = Double.parseDouble(aLine.get(2));
				lon1 = Double.parseDouble(aLine.get(3));
				coords.add(new Coord(lat1, lon1, i-1));
			} else {
				double lat2 = Double.parseDouble(aLine.get(2));
				double lon2 = Double.parseDouble(aLine.get(3));
				double distance = new HaversineDistance(lat1, lon1, lat2, lon2).getDistance();
				// TODO 단위거리에 대해 특정 범위의 오차 허용하기
				if (unitDistance < distance) {
					coords.add(new Coord(lat2, lon2, i-1));
					lat1 = lat2;
					lon1 = lon2;
				}
			}
		}
		 System.out.println("요청좌표 개수: " + coords.size());
		return coords;
	}
	
	private static void insertLocationInfos(List<MatchedPoint> points) throws FileNotFoundException, IOException {
		// csv파일의 특정 좌표에 해당하는 행에 속도제한 정보를 저장한다.
		CSVUtil.write(points);
	}
}
