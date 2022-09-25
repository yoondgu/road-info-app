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
import java.util.List;

import dto.MatchedPoint;

public class CSVUtil {
	final static String PATH = "C:/eclipse/workspace-skmap/java-roadapi/src/main/resources/Location.csv";

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
                aLine = Arrays.asList(lineArr);
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
    
    public static void write(List<MatchedPoint> points) {
    	// 원본 파일 획득
    	List<List<String>> dataList = read();
    	
        File csv = new File(PATH);
        BufferedWriter bw = null; // 출력 스트림 생성
        try {
            bw = new BufferedWriter(new FileWriter(csv));
            // csv파일의 기존 값에 이어쓰려면 위처럼 true를 지정하고, 기존 값을 덮어쓰려면 true를 삭제한다

            // TODO csv형식을 다시 만들지 말고 read()메소드에서 얻은 lineArr에 "," + speedLimit 만 덧붙이면 안되나?
            for (int i = 0; i < dataList.size(); i++) {
                List<String> data = dataList.get(i);
                String aData = "";
                aData = data.get(0) + "," + data.get(1) + "," + data.get(2) + "," + data.get(3);
                // 한 줄에 넣을 각 데이터 사이에 ,를 넣는다
                bw.write(aData);
                // 작성한 데이터를 파일에 넣는다
                bw.newLine(); // 개행
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
}
