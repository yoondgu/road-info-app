package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import dto.MatchedPoint;

public class CSVUtil {
	final static String PATH = "C:/eclipse/workspace-skmap/road-info-app/src/main/resources/Location.csv";

    public static List<List<String>> read() {
        List<List<String>> csvList = new ArrayList<List<String>>();
        File csv = new File(PATH);
        BufferedReader br = null;
        String line = "";

        try {
            br = new BufferedReader(new FileReader(csv));
            while ((line = br.readLine()) != null) { // readLine()은 파일에서 개행된 한 줄의 데이터를 읽어온다.
                List<String> aLine = new ArrayList<String>();
                String[] lineArr = line.split(","); // 파일의 한 줄을 ,로 나누어 배열에 저장 후 리스트로 변환한다.
                aLine = new ArrayList<>(Arrays.asList(lineArr));
                csvList.add(aLine);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) { 
                    br.close(); // 사용 후 BufferedReader를 닫아준다.
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
        return csvList;
    }
    
    // TODO 작업 중 오류 발생 시 파일 변경사항 되돌리기
    public static void write(List<MatchedPoint> points) {
    	// 원본 csv파일을 한 줄씩 문자열로 저장한 리스트 획득
    	List<List<String>> csvList = read();
    	// 요청좌표의 인덱스를 key, 해당 좌표의 points 객체를 value로 갖는 map객체 생성
    	Map<Integer, MatchedPoint> pointWithIdx = new HashMap<>();
    	for (MatchedPoint point : points) {
    		pointWithIdx.put(point.getSourceLocation().getCsvIdx(), point);
    	}
    	
        File csv = new File(PATH);
        BufferedWriter bw = null; // 출력 스트림 생성
        try {
        	// csv파일의 기존 값에 이어쓰려면 위처럼 true를 지정하고, 기존 값을 덮어쓰려면 true를 삭제한다
            bw = new BufferedWriter(new FileWriter(csv));
            // 0번째 행은 그대로이다.
            bw.write(generateString(csvList.get(0)));
            bw.newLine();
            
            String speedLimit = "";
            for (int i=1; i<csvList.size(); i++) {
                List<String> data = csvList.get(i);
                
                // 해당 행에 추가할 데이터가 있는지 확인(없으면 이전 행의 데이터를 그대로 저장)
                MatchedPoint point = pointWithIdx.get(i-1);
                String isMatched = point != null ? "Y" : "N";
                if (point != null) {
                	// if문으로 map에 i-1을 키로 갖는 points 찾아서 speedLimit 저장하기
                	speedLimit = point.getSpeed();
                }
                
                // TODO 더 정확한 방법으로 csv 열 상태 판단하기
                if (data.size() > 10) {
                	// 속도값을 다시 저장하는 경우
                	data.set(9, speedLimit);
                	data.set(10, isMatched);
                } else {
                	// 속도값을 새로 저장하는 경우
                	data.add(speedLimit);
                	data.add(isMatched);
                }
                bw.write(generateString(data));
                bw.newLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null) {
                    bw.flush(); // 남아있는 데이터까지 보내 준다
                    bw.close(); // 사용한 BufferedWriter를 닫아 준다
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private static String generateString(List<String> datas) {
    	StringJoiner joiner = new StringJoiner(",");
    	for (String data : datas) {
    		joiner.add(data);
    	}
    	return joiner.toString();
    }
    
}
