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

	/**
	 * skopenapi에서 제공하는 원도로등급 인덱스 별 명칭
	 * 원도로등급 (0:고속국도, 1:도시고속화도로, 2:국도, 3;국가지원지방도, 4:지방도, 5:주요도로 1, 6:주요도로 2, 7:주요도로 3, 8:기타도로 1, 9:이면도로, 10:페리항로, 11:단지내도로, 12 :이면도로 2(세도로))
	 */
	final static String[] roadCategories = {"고속국도", "도시고속화도로", "국도", "국가지원지방도", "지방도", "주요도로 1", "주요도로 2", "주요도로 3", "기타도로 1", "이면도로", "페리항로", "단지내도로", "이면도로 2(세도로)"};
	
	private static String coordsToString(List<Coord> coords) {
		// 파라미터로 받은 좌표 리스트를 SK open API 요청형식에 맞는 쿼리스트링으로 변환
		StringJoiner sj = new StringJoiner("|");
		for (int i = 0; i < coords.size(); i++) {
			Coord coord = coords.get(i);
			sj.add(coord.getLongitude() + "," + coord.getLatitude());
		}
		return sj.toString();
	}
	
	public static ResponseData getRoadInfo(double unitDistance, int maxCount) {
		// TODO return으로 예외처리하는 코드들 모두 throw로 바꾸고 try/catch에서 처리?
		
		// matchToRoads api는 도로정보를 조회하기 위한 좌표를 최대 100/500/1000개까지 지원하고, 이에 따라 요청 url이 달라진다.
		if (maxCount == 0) {
			maxCount = 100;
		}
		if (maxCount != 100 && maxCount != 500 && maxCount != 1000) {
			return ResponseData.create(false, "요청좌표의 최대 개수가 올바르지 않습니다. (100,500,1000 중 선택할 것)");
		}

		// TODO 통신,읽기/쓰기 작업 시 시간지연 오류 처리
		try {
			List<Coord> requestCoords = selectLocations(unitDistance, maxCount);
			if (requestCoords.size() > maxCount) {
				return ResponseData.create(false, "요청좌표의 개수가 지원하는 최대값(" + maxCount + ")보다 많습니다.");
			}
			
			String coordsString = coordsToString(requestCoords);
			
			// SK open API에 http 요청 보내기 : 특정 좌표 리스트에 매칭되는 도로 정보 리스트 요청
			// OkHttpClient 객체 생성
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
			
			if (response.code() == 200) {
				// 정상적으로 응답받았을 경우, 응답객체의 내용을 JsonObject, JsonArray 객체로 변환하여 필요한 정보 획득하기
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
			} else {
				return ResponseData.create(false, "SK open API 응답 오류: " + response.message(), response.code());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseData.create(false, "서버 오류: " + e.getMessage());
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
	
	public static List<Coord> selectLocations(double unitDistance, int maxCount) throws FileNotFoundException, IOException {
		List<List<String>> csvList = CSVUtil.read();
		// TODO 단위거리에 비해 훨씬 먼 거리의 좌표들로 이루어져있을 때 그냥 똑같이 고를 건지?(ex: 5m기준인데 가장 가까운 두 좌표 간 거리가 50m여도 선택된다)
		// 아니면 오차범위 내에서만 선택할 건지?
		// TODO 좌표 객체를 만들기 전에 미리 CSV에 저장된 주행정보의 총 거리를 구하고, unitDistance로 나눠서 maxCount보다 크면 예외를 발생시킨다?
		// TODO 아니면 예외발생시키고 여러개로 쪼개서 조회하기?
		
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
				if (unitDistance < distance) {
					coords.add(new Coord(lat2, lon2, i-1));
					lat1 = lat2;
					lon1 = lon2;
				}
			}
		}

		return coords;
	}

	
	public static void insertLocationInfos(List<MatchedPoint> points) throws FileNotFoundException, IOException {
		// csv파일의 특정 좌표에 해당하는 행에 속도제한 정보를 저장한다.
		CSVUtil.write(points);
	}
}
